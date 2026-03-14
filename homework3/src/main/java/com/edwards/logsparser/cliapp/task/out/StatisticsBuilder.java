package com.edwards.logsparser.cliapp.task.out;

import com.edwards.logsparser.cliapp.log.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class StatisticsBuilder {
    List<String> files;
    Long totalRequestsCount;
    Statistics.ResponseSizeInBytes responseSizeInBytes;
    Map<String, Long> resources;
    Map<Long, Long> responseCodes;
    Map<Date, Long> requestsPerDate;

    PriorityQueue<Long> p95_tracker;

    public StatisticsBuilder() {
        files = new ArrayList<>();
        totalRequestsCount = 0L;
        responseSizeInBytes = new Statistics.ResponseSizeInBytes(0.0, 0L, 0L);
        resources = new HashMap<>();
        responseCodes = new HashMap<>();
        requestsPerDate = new HashMap<>();

        p95_tracker = new PriorityQueue<>();
    }

    public void loadFiles(List<File> files) {
        for (File file : files) {
            this.files.add(file.getAbsolutePath());
        }
    }

    private void incrementTotalRequestsCount() {
        this.totalRequestsCount++;
    }

    private void addResponseSize(Long bytes) {
        Double current = responseSizeInBytes.getAverage();
        Double newAverage = current + (bytes.doubleValue() - current) / totalRequestsCount.doubleValue();

        responseSizeInBytes.setAverage(newAverage);
        responseSizeInBytes.setMax(Math.max(responseSizeInBytes.getMax(), bytes));

        p95_tracker.add(bytes);
    }

    private void addResource(String resource) {
        Long prev = resources.getOrDefault(resource, 0L);
        resources.put(resource, prev + 1L);
    }

    private void addResponseCode(Long code) {
        Long prev = responseCodes.getOrDefault(code, 0L);
        responseCodes.put(code, prev + 1L);
    }

    private void addDate(Date date) {
        Long prev = requestsPerDate.getOrDefault(date, 0L);
        requestsPerDate.put(date, prev + 1L);
    }

    public void addLog(Log log) {
        incrementTotalRequestsCount();
        addResponseSize(log.getBodyBytes());
        addResource(log.getResource());
        addResponseCode(log.getStatus());
        addDate(log.getDate());
    }

    private void extractPercentile() {
        Long top5 = 0L;
        for (int i = 0; i < 5 && !p95_tracker.isEmpty(); i++) {
            top5 = p95_tracker.poll();
        }

        responseSizeInBytes.setP95(top5);
    }

    private List<Statistics.Resource> top10Resources() {
        PriorityQueue<Map.Entry<String, Long>> resourceTracker = new PriorityQueue<>(
                Map.Entry.comparingByValue()
        );

        resourceTracker.addAll(resources.entrySet());

        List<Statistics.Resource> resources = new ArrayList<>();

        for (int i = 0; i < 10 && !resourceTracker.isEmpty(); i++) {
            Map.Entry<String, Long> entry = resourceTracker.poll();
            Statistics.Resource resource = new Statistics.Resource(entry.getKey(), entry.getValue());
            resources.add(resource);
        }

        return resources;
    }

    private List<Statistics.ResponseCode> getResponseCodes() {
        List<Statistics.ResponseCode> responseCodesList = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : responseCodes.entrySet()) {
            Statistics.ResponseCode responseCode = new Statistics.ResponseCode(entry.getKey(), entry.getValue());
            responseCodesList.add(responseCode);
        }
        return responseCodesList;
    }

    private List<Statistics.RequestsPerDate> buildDatesStatistics() {
        List<Statistics.RequestsPerDate> requestsPerDateList = new ArrayList<>();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        DateFormat weekdayFormat = new SimpleDateFormat("EEEE");
        for (Map.Entry<Date, Long> entry : requestsPerDate.entrySet()) {
            Date date = entry.getKey();
            Long count = entry.getValue();
            Statistics.RequestsPerDate requestsPerDate = Statistics.RequestsPerDate.builder()
                    .date(dateFormat.format(date))
                    .totalRequestsCount(count)
                    .totalRequestsPercentage(count.doubleValue() / totalRequestsCount.doubleValue())
                    .weekday(weekdayFormat.format(date))
                    .build();
            requestsPerDateList.add(requestsPerDate);
        }
        return requestsPerDateList;
    }

    public Statistics build() {
        extractPercentile();
        return Statistics.builder()
                .files(files)
                .totalRequestsCount(totalRequestsCount)
                .responseSizeInBytes(responseSizeInBytes)
                .resources(top10Resources())
                .responseCodes(getResponseCodes())
                .requestsPerDate(buildDatesStatistics())
                .build();
    }
}