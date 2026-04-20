package com.edwards.SpaceXTracker.api.DTO;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Core {
    private String core;
    private Integer flight;
    private Boolean gridfins;
    private Boolean legs;
    private Boolean reused;
    @SerializedName("landing_attempt")
    private Boolean landingAttempt;
    @SerializedName("landing_success")
    private Boolean landingSuccess;
    @SerializedName("landing_type")
    private String landingType;
    private String landpad;
}
