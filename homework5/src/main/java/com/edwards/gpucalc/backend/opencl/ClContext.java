package com.edwards.gpucalc.backend.opencl;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ALL;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_NAME;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetDeviceInfo;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clGetPlatformInfo;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

@Slf4j
public final class ClContext implements AutoCloseable {

    private final long platform;
    private final long device;
    private final long context;
    private final long queue;
    private final String platformName;
    private final String deviceName;
    private final boolean deviceIsGpu;

    private ClContext(long platform, long device, long context, long queue,
                      String platformName, String deviceName, boolean deviceIsGpu) {
        this.platform = platform;
        this.device = device;
        this.context = context;
        this.queue = queue;
        this.platformName = platformName;
        this.deviceName = deviceName;
        this.deviceIsGpu = deviceIsGpu;
    }

    public long device() {
        return device;
    }

    public long context() {
        return context;
    }

    public long queue() {
        return queue;
    }

    public String platformName() {
        return platformName;
    }

    public String deviceName() {
        return deviceName;
    }

    public boolean deviceIsGpu() {
        return deviceIsGpu;
    }

    public static ClContext open(@Nullable String platformSel, @Nullable String deviceSel) {
        try (MemoryStack stack = stackPush()) {
            long chosenPlatform = selectPlatform(stack, platformSel);
            String platName = queryString(stack, chosenPlatform, CL_PLATFORM_NAME, true);

            DevicePick pick = selectDevice(stack, chosenPlatform, deviceSel);
            String devName = queryString(stack, pick.device, CL_DEVICE_NAME, false);

            PointerBuffer ctxProps = stack.mallocPointer(3);
            ctxProps.put(CL_CONTEXT_PLATFORM).put(chosenPlatform).put(NULL).flip();
            IntBuffer errcode = stack.mallocInt(1);
            PointerBuffer devBuf = stack.mallocPointer(1).put(0, pick.device);
            long ctx = clCreateContext(ctxProps, devBuf, null, NULL, errcode);
            check(errcode.get(0), "clCreateContext");
            long queue = clCreateCommandQueue(ctx, pick.device, 0L, errcode);
            if (errcode.get(0) != CL_SUCCESS) {
                clReleaseContext(ctx);
                throw new IllegalStateException(
                        "clCreateCommandQueue failed with code " + errcode.get(0));
            }
            log.info("OpenCL context ready: platform='{}', device='{}' ({})",
                    platName, devName, pick.isGpu ? "GPU" : "CPU/Other");
            return new ClContext(chosenPlatform, pick.device, ctx, queue,
                    platName, devName, pick.isGpu);
        }
    }

    private static long selectPlatform(MemoryStack stack, @Nullable String selector) {
        IntBuffer num = stack.mallocInt(1);
        check(clGetPlatformIDs(null, num), "clGetPlatformIDs(count)");
        int n = num.get(0);
        if (n == 0) throw new IllegalStateException("no OpenCL platforms available");
        PointerBuffer ids = stack.mallocPointer(n);
        check(clGetPlatformIDs(ids, (IntBuffer) null), "clGetPlatformIDs(list)");
        List<String> names = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            names.add(queryString(stack, ids.get(i), CL_PLATFORM_NAME, true));
        }
        int pick = DeviceSelector.resolvePlatform(selector, names);
        return ids.get(pick);
    }

    private static DevicePick selectDevice(MemoryStack stack, long platform,
                                           @Nullable String selector) {
        IntBuffer num = stack.mallocInt(1);
        check(clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, null, num),
                "clGetDeviceIDs(count)");
        int n = num.get(0);
        if (n == 0) throw new IllegalStateException(
                "no OpenCL devices on the selected platform");
        PointerBuffer ids = stack.mallocPointer(n);
        check(clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, ids, (IntBuffer) null),
                "clGetDeviceIDs(list)");
        List<String> names = new ArrayList<>(n);
        List<Boolean> isGpu = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            long dev = ids.get(i);
            names.add(queryString(stack, dev, CL_DEVICE_NAME, false));
            long type = queryLong(stack, dev);
            isGpu.add((type & CL_DEVICE_TYPE_GPU) != 0L);
        }
        int pick = DeviceSelector.resolveDevice(selector, names, isGpu);
        return new DevicePick(ids.get(pick), isGpu.get(pick));
    }

    private static String queryString(MemoryStack stack, long handle, int param, boolean platform) {
        PointerBuffer size = stack.mallocPointer(1);
        int err = platform
                ? clGetPlatformInfo(handle, param, (ByteBuffer) null, size)
                : clGetDeviceInfo(handle, param, (ByteBuffer) null, size);
        check(err, "clGet*Info(size)");
        int n = (int) size.get(0);
        ByteBuffer buf = stack.malloc(n);
        err = platform
                ? clGetPlatformInfo(handle, param, buf, null)
                : clGetDeviceInfo(handle, param, buf, null);
        check(err, "clGet*Info(data)");
        byte[] bytes = new byte[n - 1];
        buf.get(bytes);
        return new String(bytes);
    }

    private static long queryLong(MemoryStack stack, long device) {
        java.nio.LongBuffer v = stack.mallocLong(1);
        check(clGetDeviceInfo(device, CL_DEVICE_TYPE, v, null), "clGetDeviceInfo(CL_DEVICE_TYPE)");
        return v.get(0);
    }

    static void check(int err, String call) {
        if (err != CL_SUCCESS) {
            throw new IllegalStateException(call + " failed with CL error " + err);
        }
    }

    @Override
    public void close() {
        clReleaseCommandQueue(queue);
        clReleaseContext(context);
    }

    private record DevicePick(long device, boolean isGpu) {}
}
