package com.edwards.gpucalc.cli;

import com.edwards.gpucalc.backend.BackendRegistry;
import com.edwards.gpucalc.config.AffineParamsParser;
import com.edwards.gpucalc.config.ConfigResolver;
import com.edwards.gpucalc.config.ConfigValidator;
import com.edwards.gpucalc.config.FunctionsParser;
import com.edwards.gpucalc.config.JsonConfigParser;
import com.edwards.gpucalc.config.VariationCatalog;
import com.edwards.gpucalc.metrics.MetricsService;
import com.edwards.gpucalc.output.OutputRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class RootCommandHelpTest {

    @Test
    void helpListsEveryTaskSpecFlag() {
        String help = runCapturing("--help");

        assertThat(help)
                .as("help output should list every task-spec flag")
                .contains("-w", "--width")
                .contains("-h", "--height")
                .contains("--seed")
                .contains("-i", "--iteration-count")
                .contains("-o", "--output-path")
                .contains("-t", "--threads")
                .contains("-ap", "--affine-params")
                .contains("-f", "--functions")
                .contains("--config");
    }

    @Test
    void helpShortShortCircuitsWithExitZero() {
        CommandLine cli = new CommandLine(newRootCommand());
        int exit = cli.execute("--help");
        assertThat(exit).isZero();
    }

    @Test
    void versionFlagPrintsNameAndExitsZero() {
        String out = runCapturing("--version");
        assertThat(out).contains("gpucalc");
    }

    @Test
    void versionFlagExitsZero() {
        CommandLine cli = new CommandLine(newRootCommand());
        int exit = cli.execute("--version");
        assertThat(exit).isZero();
    }

    @Test
    void minimalValidArgsInvokesCallableAndReturnsZero() {
        CommandLine cli = new CommandLine(newRootCommand());
        int exit = cli.execute("-f", "swirl:1.0", "--dry-run");
        assertThat(exit).isZero();
    }

    @Test
    void unknownOptionProducesNonZeroExit() {
        CommandLine cli = new CommandLine(newRootCommand());
        StringWriter err = new StringWriter();
        cli.setErr(new PrintWriter(err));
        int exit = cli.execute("--no-such-flag");
        assertThat(exit).isNotZero();
    }

    @Test
    void heightShortOptionIsBoundToDashH() {
        RootCommand cmd = newRootCommand();
        CommandLine cli = new CommandLine(cmd);
        int exit = cli.execute("-h", "720", "-w", "1280", "-f", "swirl:1.0", "--dry-run");
        assertThat(exit).isZero();
        assertThat(cmd.height).isEqualTo(720);
        assertThat(cmd.width).isEqualTo(1280);
    }

    @Test
    void versionProviderReturnsGpucalcToken() {
        String[] versions = new RootCommand.ManifestVersionProvider().getVersion();
        assertThat(versions).isNotEmpty();
        assertThat(versions[0]).contains("gpucalc");
    }

    private static RootCommand newRootCommand() {
        ConfigResolver resolver = new ConfigResolver(
                new AffineParamsParser(),
                new FunctionsParser(),
                new ConfigValidator(new VariationCatalog()));
        BackendRegistry backends = Mockito.mock(BackendRegistry.class);
        OutputRegistry outputs = Mockito.mock(OutputRegistry.class);
        MetricsService metrics = new MetricsService();
        return new RootCommand(resolver, new JsonConfigParser(), backends, outputs, metrics);
    }

    private static String runCapturing(String... args) {
        CommandLine cli = new CommandLine(newRootCommand());
        StringWriter out = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.execute(args);
        return out.toString();
    }
}
