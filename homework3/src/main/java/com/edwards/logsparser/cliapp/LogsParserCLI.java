package com.edwards.logsparser.cliapp;

import com.edwards.logsparser.cliapp.options.LogsParserLineOptions;
import com.edwards.logsparser.cliapp.task.ParserTask;
import com.edwards.logsparser.cliapp.task.TaskExecutor;
import lombok.NonNull;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class LogsParserCLI implements ApplicationRunner {
    @Override
    public void run(@NonNull ApplicationArguments args) throws ParseException, java.text.ParseException, IOException {
        CommandLine line = LogsParserLineOptions.parse(args.getSourceArgs());

        // На данном этапе мы знаем, что все аргументы указаны
        ParserTask task = ParserTask.builder()
                .fromDate(line.getOptionValue("from"))
                .toDate(line.getOptionValue("to"))
                .fromFiles(line.getOptionValues("path"))
                .outputFormat(line.getOptionValue("format"))
                .outputFile(line.getOptionValue("output"))
                .build();

//        System.out.println(task.getFrom());
//        System.out.println(task.getTo());
//        System.out.println(task.getFiles());
//        System.out.println(task.getOutputBuilder());

        TaskExecutor executor = new TaskExecutor(task);
        executor.saveStatistics(executor.collectStatistics());
    }
}
