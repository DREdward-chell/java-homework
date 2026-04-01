package com.edwards.SpaceXTracker.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    private List<Launch> docs;
    private int totalDocs;
    private int limit;
    private int totalPages;
    private int page;
    private boolean hasNextPage;
    private boolean hasPrevPage;
}
