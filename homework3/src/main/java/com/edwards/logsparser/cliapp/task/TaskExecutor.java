package com.edwards.logsparser.cliapp.task;

import com.edwards.logsparser.cliapp.log.Log;
import com.edwards.logsparser.cliapp.task.out.Statistics;
import com.edwards.logsparser.cliapp.task.out.StatisticsBuilder;
import lombok.AllArgsConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@AllArgsConstructor
public class TaskExecutor {
    private ParserTask parserTask;

    private Stream<Stream<Optional<Log>>> streamExecution() {
        return filterLogs(parserTask.getFiles().stream()
                .map(file -> {
                    try {
                        Scanner scanner = new Scanner(file);
                        scanner.useDelimiter(Pattern.compile("\\R"));
                        return Optional.of(new Scanner(file));
                    } catch (FileNotFoundException e) {
                        System.err.println("File: " + file.getAbsolutePath() + "not found");
                        return Optional.empty();
                    }
                })
                .map(scanner ->
                        scanner.map(sc -> {
                            Scanner scan = (Scanner) sc;
                            scan.useDelimiter(Pattern.compile("\\R"));
                            return scan.tokens();
                        })
                )
                .map(optLogs -> {
                            if (optLogs.isEmpty()) {
                                return Stream.empty();
                            }
                            return optLogs.get().map(Log::fromNullable);
                        }
                ));
    }

    private Stream<Stream<Optional<Log>>> filterLogs(Stream<Stream<Optional<Log>>> parserStream) {
        return parserStream.map(file ->
                file.filter(log -> {
                    if (log.isEmpty()) {
                        return true;
                    }
                    Log logObj = log.get();
                    boolean afterBegin = parserTask.getFrom() == null || parserTask.getFrom().before(logObj.getDate());
                    boolean beforeEnd = parserTask.getTo() == null || parserTask.getTo().after(logObj.getDate());
                    return afterBegin && beforeEnd;
                })
        );
    }

    public Statistics collectStatistics() {
        Stream<Stream<Optional<Log>>> streamExecution = streamExecution();
        StatisticsBuilder statisticsBuilder = new StatisticsBuilder();
        statisticsBuilder.loadFiles(parserTask.getFiles());
        streamExecution.forEach(file ->
            file.forEach(log -> {
                if (log.isEmpty()) {
                    return;
                }
                statisticsBuilder.addLog(log.get());
            })
        );
        return statisticsBuilder.build();
    }

    public void saveStatistics(Statistics statistics) throws IOException {
        String outputString = parserTask.getOutputBuilder().build(statistics);
        parserTask.getOutputBuilder().save(parserTask.getOutputFile(), outputString);
    }
}
