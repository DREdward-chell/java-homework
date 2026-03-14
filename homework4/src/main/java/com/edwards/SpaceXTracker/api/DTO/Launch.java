package com.edwards.SpaceXTracker.api.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Launch DTO
 * @see <a href="https://github.com/r-spacex/SpaceX-API/blob/master/docs/launches/v5">SpaceX API Launches v5</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Launch {

    @JsonProperty("flight_number")
    private Integer flightNumber;

    private String name;

    @JsonProperty("date_utc")
    private Instant dateUtc;

    @JsonProperty("date_unix")
    private Long dateUnix;

    @JsonProperty("date_local")
    private String dateLocal;

    @JsonProperty("date_precision")
    private DatePrecision datePrecision;

    @JsonProperty("static_fire_date_utc")
    private Instant staticFireDateUtc;

    @JsonProperty("static_fire_date_unix")
    private Long staticFireDateUnix;

    private Boolean tdb;

    private Boolean net;

    private Integer window;

    private UUID rocket;

    private Boolean success;

    @Singular("failure")
    private List<Failure> failures;

    private Boolean upcoming;

    private String details;

    private Fairings fairings;

    @Singular("crewMember")
    @JsonProperty("crew")
    private List<CrewMember> crew;

    @Singular("ship")
    private List<UUID> ships;

    @Singular("capsule")
    private List<UUID> capsules;

    @Singular("payload")
    private List<UUID> payloads;

    private UUID launchpad;

    @Singular("core")
    private List<Core> cores;

    private Links links;

    @JsonProperty("auto_update")
    private Boolean autoUpdate;

    public enum DatePrecision {
        half, quarter, year, month, day, hour
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Failure {
        private Integer time;
        private Integer altitude;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Fairings {
        private Boolean reused;

        @JsonProperty("recovery_attempt")
        private Boolean recoveryAttempt;

        private Boolean recovered;

        @Singular("ship")
        private List<UUID> ships;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrewMember {
        private UUID crew;
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Core {
        private UUID core;
        private Integer flight;
        private Boolean gridfins;
        private Boolean legs;
        private Boolean reused;

        @JsonProperty("landing_attempt")
        private Boolean landingAttempt;

        @JsonProperty("landing_success")
        private Boolean landingSuccess;

        @JsonProperty("landing_type")
        private String landingType;

        private UUID landpad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Links {
        private Patch patch;
        private Reddit reddit;
        private Flickr flickr;
        private String presskit;
        private String webcast;

        @JsonProperty("youtube_id")
        private String youtubeId;

        private String article;
        private String wikipedia;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Patch {
            private String small;
            private String large;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Reddit {
            private String campaign;
            private String launch;
            private String media;
            private String recovery;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Flickr {
            @Singular("smallImage")
            private List<String> small;

            @Singular("originalImage")
            private List<String> original;
        }
    }
}