package com.edwards.SpaceXTracker.api.DTO;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Launch DTO
 * @see <a href="https://github.com/r-spacex/SpaceX-API/blob/master/docs/launches/v5">SpaceX API Launches v5</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Launch {
    private String id;
    private String name;
    @SerializedName("flight_number")
    private int flightNumber;
    @SerializedName("date_utc")
    private String dateUtc;
    private Boolean success;
    private boolean upcoming;
    private String details;
    private List<Failure> failures;
    private List<Core> cores;
}
