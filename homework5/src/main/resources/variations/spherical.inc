vec2 variation_spherical(vec2 p) {
    float r2 = p.x * p.x + p.y * p.y;
    if (r2 == 0.0) return vec2(0.0);
    return p / r2;
}
