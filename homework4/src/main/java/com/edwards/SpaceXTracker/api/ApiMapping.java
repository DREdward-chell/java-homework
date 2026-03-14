package com.edwards.SpaceXTracker.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiMapping {
    public static boolean isError(int statusCode) {
        return statusCode >= 400;
    }
}
