package com.edwards.logsparser.cliapp.output;

import com.edwards.logsparser.cliapp.task.out.Statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface OutputBuilder {
    static OutputBuilder forFormat(String format) {
        return switch (format) {
            case "json" -> new JsonBuilder();
            case "markdown" -> new MarkdownBuilder();
            default -> throw new IllegalArgumentException("Unknown format: " + format);
        };
    }

    String getFileExtension();

    String build(Statistics statistics);

    default void save(File file, String data) throws IOException {
        Path path = Paths.get(file.getAbsolutePath());
        Files.createDirectories(path.getParent());
        Files.createFile(path);
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            fileWriter.write(data);
            fileWriter.flush();
        }
    }
}