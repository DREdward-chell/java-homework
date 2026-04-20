package com.edwards.SpaceXTracker.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Failure {
    private int time;
    private Integer altitude;
    private String reason;
}
