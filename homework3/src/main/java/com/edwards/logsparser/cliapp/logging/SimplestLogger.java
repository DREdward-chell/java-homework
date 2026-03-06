package com.edwards.logsparser.cliapp.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimplestLogger implements AutoCloseable {
    private final Writer writer;

    private SimplestLogger() throws IOException {
        Path path = Paths.get("./logs/log" + DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm:ss").format(LocalDateTime.now()) + ".log");
        Files.createDirectories(path.getParent());
        Files.createFile(path);
        writer = new FileWriter(path.toAbsolutePath().toString(), true);
    }

    public static SimplestLogger INSTANCE;

    static {
        try {
            INSTANCE = new SimplestLogger();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void logFaulty(String initial) {
        try {
            writer.write("Log: ```" + initial + "``` is ill-formed!\n");
            writer.flush();
        }  catch (IOException e) {
            System.err.println("Error while writing log: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
