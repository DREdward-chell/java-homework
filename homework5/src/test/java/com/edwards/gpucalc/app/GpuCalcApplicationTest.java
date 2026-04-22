package com.edwards.gpucalc.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanity test: the Spring context loads in non-web mode and passing {@code --help}
 * through the CommandLineRunner is a successful, no-op invocation.
 */
@SpringBootTest(args = "--help")
class GpuCalcApplicationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void contextLoadsInNonWebMode() {
        assertThat(context).isNotNull();
        assertThat(context.getBean(GpuCalcApplication.class)).isNotNull();
        assertThat(context.getEnvironment()
                .getProperty("spring.main.web-application-type"))
                .isEqualToIgnoringCase(WebApplicationType.NONE.name());
    }

    @Test
    void rootCommandExitCodeIsZeroForHelp() {
        GpuCalcApplication app = context.getBean(GpuCalcApplication.class);
        assertThat(app.getExitCode()).isZero();
    }
}
