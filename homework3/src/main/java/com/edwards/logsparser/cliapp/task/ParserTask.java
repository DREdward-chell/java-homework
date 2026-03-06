package com.edwards.logsparser.cliapp.task;

import com.edwards.logsparser.cliapp.output.OutputBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Data
public class ParserTask {
    private Date from;
    private Date to;
    private List<File> files;
    private OutputBuilder outputBuilder;
    private File outputFile;

    public static TaskBuilder builder() {
        return new TaskBuilder();
    }
}
