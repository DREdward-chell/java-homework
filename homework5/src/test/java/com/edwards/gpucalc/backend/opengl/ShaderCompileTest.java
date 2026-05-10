package com.edwards.gpucalc.backend.opengl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL43C.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43C.glCompileShader;
import static org.lwjgl.opengl.GL43C.glCreateShader;
import static org.lwjgl.opengl.GL43C.glDeleteShader;
import static org.lwjgl.opengl.GL43C.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL43C.glGetShaderi;
import static org.lwjgl.opengl.GL43C.glShaderSource;

@DisabledOnOs(OS.MAC)
class ShaderCompileTest {

    @Test
    void computeShaderCompilesWithAllFourVariations() {
        GlContext ctx;
        try {
            ctx = GlContext.createHeadless();
        } catch (IllegalStateException | UnsatisfiedLinkError e) {
            return;
        }
        try {
            String src = new ShaderSourceBuilder().build(
                    OpenGlComputeRenderBackend.SHADER_TEMPLATE,
                    List.of("linear", "sinusoidal", "spherical", "swirl"));
            int shader = glCreateShader(GL_COMPUTE_SHADER);
            try {
                glShaderSource(shader, src);
                glCompileShader(shader);
                int status = glGetShaderi(shader, GL_COMPILE_STATUS);
                String log = glGetShaderInfoLog(shader);
                assertThat(status).as("compile status; log=%s", log).isEqualTo(GL_TRUE);
            } finally {
                glDeleteShader(shader);
            }
        } finally {
            ctx.close();
        }
    }
}
