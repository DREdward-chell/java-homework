package com.edwards.gpucalc.cli;

import com.edwards.gpucalc.backend.BackendRegistry;
import com.edwards.gpucalc.backend.RenderBackend;
import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.CliOverrides;
import com.edwards.gpucalc.config.ConfigParseException;
import com.edwards.gpucalc.config.ConfigResolver;
import com.edwards.gpucalc.config.ConfigValidationException;
import com.edwards.gpucalc.config.JsonConfigParser;
import com.edwards.gpucalc.config.RawConfig;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.metrics.MetricsService;
import com.edwards.gpucalc.output.EpochSink;
import com.edwards.gpucalc.output.OutputRegistry;
import com.edwards.gpucalc.output.OutputSink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
@Command(
        name = "gpucalc",
        mixinStandardHelpOptions = false,
        versionProvider = RootCommand.ManifestVersionProvider.class,
        description = "Fractal Flame renderer based on the Chaos Game algorithm.",
        sortOptions = false)
public class RootCommand implements Callable<Integer> {

    static final int EXIT_OK = 0;
    static final int EXIT_GENERIC = 1;
    static final int EXIT_CONFIG = 2;
    static final int EXIT_BACKEND_INIT = 3;
    static final int EXIT_IO = 4;

    static final String DEFAULT_BACKEND = "java";
    static final String DEFAULT_OUTPUT_MODE = "png";

    private final ConfigResolver configResolver;
    private final JsonConfigParser jsonConfigParser;
    private final BackendRegistry backendRegistry;
    private final OutputRegistry outputRegistry;
    private final MetricsService metrics;

    @Spec CommandSpec spec;

    @Option(names = "--help", usageHelp = true,
            description = "Show this help message and exit.")
    boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true,
            description = "Print version information and exit.")
    boolean versionRequested;

    @Option(names = {"-w", "--width"}, paramLabel = "<px>",
            description = "Output image width in pixels. Default: 1920.")
    @Nullable Integer width;

    @Option(names = {"-h", "--height"}, paramLabel = "<px>",
            description = "Output image height in pixels. Default: 1080.")
    @Nullable Integer height;

    @Option(names = "--seed", paramLabel = "<number>",
            description = "Seed for the random generator. Accepts integer or fractional. Default: 5.")
    @Nullable Double seed;

    @Option(names = {"-i", "--iteration-count"}, paramLabel = "<n>",
            description = "Number of Chaos Game iterations. Default: 2500.")
    @Nullable Integer iterationCount;

    @Option(names = {"-o", "--output-path"}, paramLabel = "<path>",
            description = "Output file path (relative). Default: result.png.")
    @Nullable String outputPath;

    @Option(names = {"-t", "--threads"}, paramLabel = "<n>",
            description = "Number of worker threads. Default: 1.")
    @Nullable Integer threads;

    @Option(names = {"-ap", "--affine-params"}, paramLabel = "<spec>",
            description = "Affine transforms: a,b,c,d,e,f/a,b,c,d,e,f/...")
    @Nullable String affineParams;

    @Option(names = {"-f", "--functions"}, paramLabel = "<spec>",
            description = "Variations and weights: e.g. swirl:1.0,horseshoe:0.8.")
    @Nullable String functions;

    @Option(names = "--config", paramLabel = "<path>",
            description = "Path to JSON config file.")
    @Nullable String configPath;

    @Option(names = {"-b", "--backend"}, paramLabel = "<id>",
            description = "Rendering backend id. Default: java.")
    @Nullable String backendId;

    @Option(names = "--output-mode", paramLabel = "<id>",
            description = "Output sink id (png, epochs, gif). Default: png.")
    @Nullable String outputMode;

    @Option(names = "--epoch-count", paramLabel = "<n>",
            description = "Number of epoch snapshots during the render (mutually exclusive with --epoch-stride).")
    @Nullable Integer epochCount;

    @Option(names = "--epoch-stride", paramLabel = "<n>",
            description = "Iterations between epoch snapshots (mutually exclusive with --epoch-count).")
    @Nullable Integer epochStride;

    @Option(names = "--gif-delay", paramLabel = "<csec>",
            description = "Per-frame delay of animated GIF output in hundredths of a second. Default: 10.")
    @Nullable Integer gifDelayCentiseconds;

    @Option(names = "--cl-platform", paramLabel = "<index-or-substring>",
            description = "OpenCL platform selector: integer index or case-insensitive substring "
                    + "of the platform name. Default: platform 0.")
    @Nullable String clPlatform;

    @Option(names = "--cl-device", paramLabel = "<index-or-substring>",
            description = "OpenCL device selector on the chosen platform: integer index or "
                    + "case-insensitive substring of the device name. Default: first GPU, else "
                    + "first available device with a warning.")
    @Nullable String clDevice;

    @Option(names = "--dry-run", hidden = true,
            description = "Resolve and print the configuration, then exit without rendering.")
    boolean dryRun;

    @Override
    public Integer call() {
        ResolvedConfig resolved;
        try {
            RawConfig fileConfig = loadJsonIfPresent();
            resolved = configResolver.resolve(toOverrides(), fileConfig);
        } catch (ConfigValidationException e) {
            PrintWriter err = spec.commandLine().getErr();
            err.println("Configuration errors:");
            err.println(e.bulleted());
            err.flush();
            return EXIT_CONFIG;
        } catch (ConfigParseException e) {
            spec.commandLine().getErr().println("Configuration error: " + e.getMessage());
            return EXIT_CONFIG;
        } catch (IOException e) {
            spec.commandLine().getErr().println(
                    "I/O error reading config file '" + configPath + "': " + e.getMessage());
            return EXIT_IO;
        }

        if (dryRun) {
            printResolved(resolved);
            return EXIT_OK;
        }

        String backendKey = backendId != null ? backendId : DEFAULT_BACKEND;
        String sinkKey = outputMode != null ? outputMode : DEFAULT_OUTPUT_MODE;

        RenderBackend backend;
        OutputSink sink;
        try {
            backend = backendRegistry.require(backendKey);
            sink = outputRegistry.require(sinkKey);
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return EXIT_BACKEND_INIT;
        }

        try {
            if (sink instanceof EpochSink epochSink && resolved.epochs() != null) {
                runEpochPipeline(backend, epochSink, resolved);
            } else {
                RenderResult result = backend.render(resolved);
                sink.write(result, resolved);
            }
        } catch (UncheckedEpochIoException e) {
            log.error("I/O error writing output to '{}'", resolved.outputPath(), e.getCause());
            spec.commandLine().getErr().println(
                    "I/O error writing '" + resolved.outputPath() + "': " + e.getCause().getMessage());
            return EXIT_IO;
        } catch (RuntimeException e) {
            log.error("Rendering failed", e);
            spec.commandLine().getErr().println("Rendering failed: " + e.getMessage());
            return EXIT_BACKEND_INIT;
        } catch (IOException e) {
            log.error("I/O error writing output to '{}'", resolved.outputPath(), e);
            spec.commandLine().getErr().println(
                    "I/O error writing '" + resolved.outputPath() + "': " + e.getMessage());
            return EXIT_IO;
        }

        metrics.dumpToLog();
        return EXIT_OK;
    }

    private void runEpochPipeline(RenderBackend backend, EpochSink sink, ResolvedConfig resolved)
            throws IOException {
        sink.beginEpochs(resolved);
        backend.renderEpochs(resolved, (snapshot, index, total) -> {
            try {
                sink.writeEpoch(snapshot, index, total, resolved);
            } catch (IOException ioe) {
                throw new UncheckedEpochIoException(ioe);
            }
        });
        sink.endEpochs(resolved);
    }

    static final class UncheckedEpochIoException extends RuntimeException {
        UncheckedEpochIoException(IOException cause) {
            super(cause);
        }
    }

    private @Nullable RawConfig loadJsonIfPresent() throws IOException {
        if (configPath == null) {
            return null;
        }
        return jsonConfigParser.parse(Path.of(configPath));
    }

    private CliOverrides toOverrides() {
        return new CliOverrides(width, height, seed, iterationCount,
                outputPath, threads, affineParams, functions,
                epochCount, epochStride, gifDelayCentiseconds,
                clPlatform, clDevice);
    }

    private void printResolved(ResolvedConfig r) {
        PrintWriter out = spec.commandLine().getOut();
        out.println("resolved-config:");
        out.println("  width:           " + r.width());
        out.println("  height:          " + r.height());
        out.println("  seed:            " + r.seed());
        out.println("  iteration-count: " + r.iterationCount());
        out.println("  output-path:     " + r.outputPath());
        out.println("  threads:         " + r.threads());
        out.println("  affine-params:   " + r.affineParams().size() + " transform(s)");
        for (int i = 0; i < r.affineParams().size(); i++) {
            out.println("    [" + i + "] " + r.affineParams().get(i));
        }
        out.println("  functions:       " + r.functions().size() + " variation(s)");
        for (int i = 0; i < r.functions().size(); i++) {
            out.println("    [" + i + "] " + r.functions().get(i));
        }
        out.flush();
    }

    static final class ManifestVersionProvider implements picocli.CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            Package pkg = RootCommand.class.getPackage();
            String implVersion = pkg == null ? null : pkg.getImplementationVersion();
            String version = implVersion != null ? implVersion : "0.1.0-SNAPSHOT";
            return new String[]{"gpucalc " + version};
        }
    }
}
