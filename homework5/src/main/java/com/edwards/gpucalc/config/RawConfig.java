package com.edwards.gpucalc.config;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class RawConfig {

    @Nullable public Size size;
    @Nullable @SerializedName("iteration_count") public Integer iterationCount;
    @Nullable @SerializedName("output_path") public String outputPath;
    @Nullable public Integer threads;
    @Nullable public Double seed;
    @Nullable public List<RawFunction> functions;
    @Nullable @SerializedName("affine_params") public List<RawAffine> affineParams;
    @Nullable @SerializedName("symmetry_level") public Integer symmetryLevel;
    @Nullable @SerializedName("epoch_count") public Integer epochCount;
    @Nullable @SerializedName("epoch_stride") public Integer epochStride;
    @Nullable @SerializedName("gif_delay") public Integer gifDelayCentiseconds;
    @Nullable @SerializedName("cl_platform") public String clPlatform;
    @Nullable @SerializedName("cl_device") public String clDevice;

    public static final class Size {
        @Nullable public Integer width;
        @Nullable public Integer height;
    }

    public static final class RawFunction {
        @Nullable public String name;
        @Nullable public Double weight;
    }

    public static final class RawAffine {
        @Nullable public Double a;
        @Nullable public Double b;
        @Nullable public Double c;
        @Nullable public Double d;
        @Nullable public Double e;
        @Nullable public Double f;
    }
}
