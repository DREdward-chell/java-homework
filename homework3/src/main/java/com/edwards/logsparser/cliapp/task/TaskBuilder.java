package com.edwards.logsparser.cliapp.task;

import com.edwards.logsparser.cliapp.output.OutputBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskBuilder {
    private Date from;
    private Date to;
    private List<File> files;
    private OutputBuilder outputBuilder;
    private File outputFile;

    public TaskBuilder fromDate(String from) throws ParseException {
        if (from == null) {
            return this;
        }

        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        this.from = dateFormat.parse(from);
        return this;
    }

    public TaskBuilder toDate(String to) throws ParseException {
        if (to == null) {
            return this;
        }

        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        this.to = dateFormat.parse(to);
        return this;
    }

    public TaskBuilder fromFiles(String[] paths) throws FileNotFoundException {
        files = new ArrayList<>();
        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("File: " + path + " does not exist");
                System.exit(-1);
            }
            files.add(file);
        }
        return this;
    }

    public TaskBuilder outputFormat(String format) {
        outputBuilder = OutputBuilder.forFormat(format);
        return this;
    }

    public TaskBuilder outputFile(String file) {
        File f = Paths.get(file).toFile();
        if (f.exists()) {
            throw new RuntimeException("Output file already exists: " + file);
        }
        outputFile = f;
        return this;
    }

    public ParserTask build() {
        if (files == null || outputBuilder == null || outputFile == null) {
            throw new IllegalArgumentException("ParserTask was not built properly");
        }
        if (!outputFile.getName().endsWith("." + outputBuilder.getFileExtension())) {
            throw new IllegalArgumentException("Output file has wrong extension! Expected: " + outputBuilder.getFileExtension() + ", got: " + outputFile.getName());
        }
        return new ParserTask(from, to, files, outputBuilder, outputFile);
    }
}
