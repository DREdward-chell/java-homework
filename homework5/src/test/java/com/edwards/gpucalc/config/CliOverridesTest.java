package com.edwards.gpucalc.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CliOverridesTest {

    @Test
    void emptyHasAllFieldsNull() {
        CliOverrides e = CliOverrides.empty();
        assertThat(e.width()).isNull();
        assertThat(e.height()).isNull();
        assertThat(e.seed()).isNull();
        assertThat(e.iterationCount()).isNull();
        assertThat(e.outputPath()).isNull();
        assertThat(e.threads()).isNull();
        assertThat(e.affineParams()).isNull();
        assertThat(e.functions()).isNull();
    }

    @Test
    void carriesNonNullValues() {
        CliOverrides o = new CliOverrides(1, 2, 3.0, 4, "p.png", 5, "0,0,0,0,0,0", "linear:1.0");
        assertThat(o.width()).isEqualTo(1);
        assertThat(o.height()).isEqualTo(2);
        assertThat(o.seed()).isEqualTo(3.0);
        assertThat(o.iterationCount()).isEqualTo(4);
        assertThat(o.outputPath()).isEqualTo("p.png");
        assertThat(o.threads()).isEqualTo(5);
        assertThat(o.affineParams()).isEqualTo("0,0,0,0,0,0");
        assertThat(o.functions()).isEqualTo("linear:1.0");
    }
}
