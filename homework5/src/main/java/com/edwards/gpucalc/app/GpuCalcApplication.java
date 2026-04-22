package com.edwards.gpucalc.app;

import com.edwards.gpucalc.cli.RootCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.ParseResult;

@Slf4j
@SpringBootApplication(scanBasePackages = "com.edwards.gpucalc")
@RequiredArgsConstructor
public class GpuCalcApplication implements CommandLineRunner, ExitCodeGenerator {

    private static final int EXIT_GENERIC = 1;

    private final IFactory picocliFactory;
    private final RootCommand rootCommand;
    private int exitCode;

    public static void main(String[] args) {
        int code = SpringApplication.exit(
                new SpringApplicationBuilder(GpuCalcApplication.class)
                        .web(WebApplicationType.NONE)
                        .bannerMode(Banner.Mode.OFF)
                        .run(args));
        System.exit(code);
    }

    @Override
    public void run(String... args) {
        CommandLine cli = new CommandLine(rootCommand, picocliFactory);
        cli.setExecutionExceptionHandler(this::handleExecutionException);
        exitCode = cli.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
        log.error("Unhandled exception during command execution", ex);
        cmd.getErr().println("Error: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return EXIT_GENERIC;
    }
}
