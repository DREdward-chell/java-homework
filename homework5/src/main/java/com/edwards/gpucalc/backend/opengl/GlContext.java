package com.edwards.gpucalc.backend.opengl;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.system.MemoryUtil.NULL;

@Slf4j
public final class GlContext implements AutoCloseable {
    private final long window;
    private final GLCapabilities caps;
    private final GLFWErrorCallback errorCallback;

    private GlContext(long window, GLCapabilities caps, GLFWErrorCallback errorCallback) {
        this.window = window;
        this.caps = caps;
        this.errorCallback = errorCallback;
    }

    public static GlContext createHeadless() {
        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();
        if (!glfwInit()) {
            errorCallback.free();
            throw new IllegalStateException("glfwInit() failed — GLFW native cannot start");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        long window = glfwCreateWindow(1, 1, "gpucalc-compute", NULL, NULL);
        if (window == NULL) {
            glfwTerminate();
            errorCallback.free();
            throw new IllegalStateException(
                    "glfwCreateWindow failed — cannot obtain OpenGL 4.3 Core context headlessly");
        }
        glfwMakeContextCurrent(window);
        GLCapabilities caps = GL.createCapabilities();
        if (!caps.OpenGL43) {
            glfwDestroyWindow(window);
            glfwTerminate();
            errorCallback.free();
            throw new IllegalStateException(
                    "driver did not advertise OpenGL 4.3 - compute shaders unavailable");
        }
        log.info("OpenGL context ready (4.3 Core, headless GLFW window)");

        @SuppressWarnings("unused") int noApi = GLFW_NO_API;
        return new GlContext(window, caps, errorCallback);
    }

    @Override
    public void close() {
        glfwDestroyWindow(window);
        glfwTerminate();
        errorCallback.free();
    }
}
