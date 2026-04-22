vec2 variation_horseshoe(vec2 p) {
    float r = sqrt(p.x * p.x + p.y * p.y);
    if (r == 0.0) return vec2(0.0);
    float inv = 1.0 / r;
    return vec2((p.x - p.y) * (p.x + p.y) * inv, 2.0 * p.x * p.y * inv);
}
