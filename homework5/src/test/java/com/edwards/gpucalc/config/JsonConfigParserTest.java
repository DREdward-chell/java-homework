package com.edwards.gpucalc.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonConfigParserTest {

    private final JsonConfigParser parser = new JsonConfigParser();

    @Test
    void roundTripsTaskSpecExample() {
        String json = """
                {
                  "size": {"width": 1920, "height": 1080},
                  "iteration_count": 2500,
                  "output_path": "result.png",
                  "threads": 4,
                  "seed": 2.1324512,
                  "functions": [
                    {"name": "swirl", "weight": 1.0},
                    {"name": "horseshoe", "weight": 0.7}
                  ],
                  "affine_params": [
                    {"a": 1.0, "b": 1.0, "c": 1.0, "d": 1.0, "e": 1.0, "f": 1.0},
                    {"a": 0.3, "b": 1.0, "c": -0.2, "d": 0.4, "e": 1.0, "f": 1.0}
                  ]
                }
                """;
        RawConfig cfg = parser.parseString(json, "test");
        assertThat(cfg.size).isNotNull();
        assertThat(cfg.size.width).isEqualTo(1920);
        assertThat(cfg.size.height).isEqualTo(1080);
        assertThat(cfg.iterationCount).isEqualTo(2500);
        assertThat(cfg.outputPath).isEqualTo("result.png");
        assertThat(cfg.threads).isEqualTo(4);
        assertThat(cfg.seed).isEqualTo(2.1324512);
        assertThat(cfg.functions).hasSize(2);
        assertThat(cfg.functions.get(0).name).isEqualTo("swirl");
        assertThat(cfg.functions.get(0).weight).isEqualTo(1.0);
        assertThat(cfg.affineParams).hasSize(2);
        assertThat(cfg.affineParams.get(1).c).isEqualTo(-0.2);
    }

    @Test
    void leavesMissingFieldsAsNullForLayerFallback() {
        String json = """
                { "threads": 8 }
                """;
        RawConfig cfg = parser.parseString(json, "test");
        assertThat(cfg.threads).isEqualTo(8);
        assertThat(cfg.size).isNull();
        assertThat(cfg.outputPath).isNull();
        assertThat(cfg.seed).isNull();
        assertThat(cfg.functions).isNull();
        assertThat(cfg.affineParams).isNull();
    }

    @Test
    void rejectsUnknownTopLevelField() {
        String json = """
                { "treads": 8 }
                """;
        assertThatThrownBy(() -> parser.parseString(json, "test"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("treads");
    }

    @Test
    void rejectsMalformedJson() {
        String json = "{ not json at all";
        assertThatThrownBy(() -> parser.parseString(json, "test"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("not a valid JSON object");
    }

    @Test
    void rejectsNonObjectRoot() {
        String json = "[1, 2, 3]";
        assertThatThrownBy(() -> parser.parseString(json, "test"))
                .isInstanceOf(ConfigParseException.class);
    }

    @Test
    void readsFromFile(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("c.json");
        Files.writeString(f, "{ \"threads\": 3 }");
        RawConfig cfg = parser.parse(f);
        assertThat(cfg.threads).isEqualTo(3);
    }

    @Test
    void propagatesMissingFileAsIoException(@TempDir Path tmp) {
        Path f = tmp.resolve("does-not-exist.json");
        assertThatThrownBy(() -> parser.parse(f))
                .isInstanceOf(IOException.class);
    }

    @Test
    void acceptsSymmetryLevelForPhase9Forwards() {
        String json = """
                { "symmetry_level": 6 }
                """;
        RawConfig cfg = parser.parseString(json, "test");
        assertThat(cfg.symmetryLevel).isEqualTo(6);
    }
}
