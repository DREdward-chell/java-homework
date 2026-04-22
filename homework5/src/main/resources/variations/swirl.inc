vec2 variation_swirl(vec2 p) {
    float r2 = p.x * p.x + p.y * p.y;
    float s = sin(r2);
    float c = cos(r2);
    return vec2(p.x * s - p.y * c, p.x * c + p.y * s);
}
