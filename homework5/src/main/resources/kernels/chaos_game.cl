//#include "variations"

static uint rng_next(uint* state) {
    uint s = *state;
    s ^= s << 13u;
    s ^= s >> 17u;
    s ^= s << 5u;
    if (s == 0u) s = 0x9E3779B9u;
    *state = s;
    return s;
}

static uint rng_range(uint* state, uint n) {
    return rng_next(state) % n;
}

static float2 apply_variation_blend(float2 p,
                                    __global const uint* variations,
                                    uint numVariations) {
    float2 acc = (float2)(0.0f, 0.0f);
    for (uint i = 0u; i < numVariations; i++) {
        uint id = variations[i];
        float w = as_float(variations[numVariations + i]);
        float2 v;
        switch (id) {
            case 0u: v = variation_linear(p);     break;
            case 1u: v = variation_sinusoidal(p); break;
            case 2u: v = variation_spherical(p);  break;
            case 3u: v = variation_swirl(p);      break;
            case 4u: v = variation_horseshoe(p);  break;
            default: v = (float2)(0.0f, 0.0f);    break;
        }
        acc += w * v;
    }
    return acc;
}

static float2 apply_transform(float2 p,
                              uint ti,
                              __global const float* transforms,
                              __global const uint*  variations,
                              uint numVariations,
                              float* outColorCoord) {
    uint base = ti * 8u;
    float a = transforms[base + 0u];
    float b = transforms[base + 1u];
    float c = transforms[base + 2u];
    float d = transforms[base + 3u];
    float e = transforms[base + 4u];
    float f = transforms[base + 5u];
    *outColorCoord = transforms[base + 6u];
    float2 affine = (float2)(a * p.x + b * p.y + c, d * p.x + e * p.y + f);
    return apply_variation_blend(affine, variations, numVariations);
}

static void plot(float2 p, float3 rgb,
                 __global uint* hist,
                 uint width, uint height,
                 float viewMinX, float viewInvRangeX,
                 float viewMinY, float viewInvRangeY,
                 float colorScale) {
    if (isinf(p.x) || isinf(p.y) || isnan(p.x) || isnan(p.y)) return;
    float nx = (p.x - viewMinX) * viewInvRangeX;
    float ny = 1.0f - (p.y - viewMinY) * viewInvRangeY;
    int px = (int)(nx * (float)(width - 1u));
    int py = (int)(ny * (float)(height - 1u));
    if (px < 0 || py < 0 || px >= (int)width || py >= (int)height) return;
    uint idx = ((uint)py * width + (uint)px) * 4u;
    atomic_add(&hist[idx + 0u], 1u);
    atomic_add(&hist[idx + 1u], (uint)(rgb.x * colorScale));
    atomic_add(&hist[idx + 2u], (uint)(rgb.y * colorScale));
    atomic_add(&hist[idx + 3u], (uint)(rgb.z * colorScale));
}

static float3 sample_palette(float c, __global const float4* palette) {
    int i = (int)(clamp(c, 0.0f, 1.0f) * 255.0f);
    float4 p = palette[i];
    return (float3)(p.x, p.y, p.z);
}

__kernel void chaos_game(
    __global const float*  transforms,
    __global const uint*   variations,
    __global uint*         hist,
    __global const float4* palette,
    uint   width,
    uint   height,
    uint   numTransforms,
    uint   numVariations,
    uint   iterationsPerInvocation,
    uint   warmupIterations,
    uint   seedLo,
    uint   seedHi,
    float  viewMinX,
    float  viewInvRangeX,
    float  viewMinY,
    float  viewInvRangeY,
    float  colorScale
) {
    uint gid = (uint)get_global_id(0);

    uint state = seedLo ^ (gid * 0x9E3779B9u) ^ (seedHi + gid * 0x85EBCA6Bu);
    if (state == 0u) state = 1u + gid;

    float2 p = (float2)(0.0f, 0.0f);
    float  c = 0.5f;

    for (uint i = 0u; i < warmupIterations; i++) {
        uint ti = rng_range(&state, numTransforms);
        float cc;
        p = apply_transform(p, ti, transforms, variations, numVariations, &cc);
        c = (c + cc) * 0.5f;
    }

    for (uint i = 0u; i < iterationsPerInvocation; i++) {
        uint ti = rng_range(&state, numTransforms);
        float cc;
        p = apply_transform(p, ti, transforms, variations, numVariations, &cc);
        c = (c + cc) * 0.5f;
        float3 rgb = sample_palette(c, palette);
        plot(p, rgb, hist, width, height,
             viewMinX, viewInvRangeX, viewMinY, viewInvRangeY, colorScale);
    }
}
