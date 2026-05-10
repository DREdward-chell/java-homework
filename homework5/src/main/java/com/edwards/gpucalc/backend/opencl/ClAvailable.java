package com.edwards.gpucalc.backend.opencl;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ALL;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.system.MemoryStack.stackPush;

@Slf4j
public final class ClAvailable implements Condition {

    private static volatile Boolean cached;

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return available();
    }

    static boolean available() {
        Boolean c = cached;
        if (c != null) return c;
        synchronized (ClAvailable.class) {
            if (cached != null) return cached;
            boolean ok = probe();
            cached = ok;
            return ok;
        }
    }

    private static boolean probe() {
        try {
            CL.create();
        } catch (IllegalStateException alreadyCreated) {
            // This might happen due to buggy Springs inversion of control
        } catch (Throwable t) {
            log.info("OpenCL ICD not loadable ({}); 'opencl' backend will not be registered",
                    t.getMessage());
            return false;
        }
        try (var stack = stackPush()) {
            IntBuffer numPlatforms = stack.mallocInt(1);
            int err = clGetPlatformIDs(null, numPlatforms);
            if (err != CL_SUCCESS || numPlatforms.get(0) == 0) {
                log.info("no OpenCL platforms found; 'opencl' backend will not be registered");
                return false;
            }
            PointerBuffer platforms = stack.mallocPointer(numPlatforms.get(0));
            err = clGetPlatformIDs(platforms, (IntBuffer) null);
            if (err != CL_SUCCESS) return false;
            for (int i = 0; i < platforms.capacity(); i++) {
                long platform = platforms.get(i);
                IntBuffer numDevices = stack.mallocInt(1);
                int derr = clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, null, numDevices);
                if (derr == CL_SUCCESS && numDevices.get(0) > 0) {
                    return true;
                }
            }
            log.info("OpenCL ICD present but no devices found; 'opencl' backend will not be registered");
            return false;
        } catch (Throwable t) {
            log.info("OpenCL probe failed: {}; 'opencl' backend will not be registered",
                    t.getMessage());
            return false;
        }
    }
}
