package com.edwards.gpucalc.config;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class VariationCatalog {

    private static final Set<String> KNOWN = Set.of(
            "linear", "sinusoidal", "spherical", "swirl",
            "horseshoe", "polar", "handkerchief", "heart", "disc", "spiral",
            "hyperbolic", "diamond", "ex", "julia", "bent", "waves",
            "fisheye", "popcorn", "exponential", "power", "cosine"
    );

    public boolean contains(String name) {
        return KNOWN.contains(name);
    }

    public Set<String> names() {
        return KNOWN;
    }
}
