package com.edwards.gpucalc.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
public class JsonConfigParser {

    private static final Set<String> ALLOWED_TOP_LEVEL = Set.of(
            "size",
            "iteration_count",
            "output_path",
            "threads",
            "seed",
            "functions",
            "affine_params",
            "symmetry_level",
            "epoch_count",
            "epoch_stride",
            "gif_delay"
    );

    private final Gson gson = new Gson();

    public RawConfig parse(Path file) throws IOException {
        String content = Files.readString(file);
        return parseString(content, file.toString());
    }

    public RawConfig parseString(String json, String sourceForErrors) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new ConfigParseException(
                    sourceForErrors + ": not a valid JSON object", e);
        }
        rejectUnknownFields(root, sourceForErrors);
        try {
            return gson.fromJson(root, RawConfig.class);
        } catch (JsonParseException e) {
            throw new ConfigParseException(
                    sourceForErrors + ": JSON shape does not match config schema: "
                            + e.getMessage(), e);
        }
    }

    private static void rejectUnknownFields(JsonObject root, String source) {
        for (String key : root.keySet()) {
            if (!ALLOWED_TOP_LEVEL.contains(key)) {
                throw new ConfigParseException(
                        source + ": unknown top-level field '" + key + "' (allowed: "
                                + String.join(", ", ALLOWED_TOP_LEVEL) + ")");
            }
        }
    }
}
