package com.edwards.logsparser.cliapp.task.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Statistics {
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class ResponseSizeInBytes {
        Double average;
        Long max;
        Long p95;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Resource {
        String resource;
        Long totalRequestsCount;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class ResponseCode {
        Long code;
        Long totalResponsesCount;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RequestsPerDate {
        String date;
        String weekday;
        Long totalRequestsCount;
        Double totalRequestsPercentage;
    }

    List<String> files;
    Long totalRequestsCount;
    ResponseSizeInBytes responseSizeInBytes;
    List<Resource> resources;
    List<ResponseCode> responseCodes;
    List<RequestsPerDate> requestsPerDate;
}
