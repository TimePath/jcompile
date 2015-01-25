string __builtin_debug_typestring();
float __builtin_exp();
[[inline]] float __builtin_floor(float a) { return a | 0; }
[[inline]] float __builtin_logxor(float a, float b) { return !a != !b; }
[[inline]] float __builtin_mod(float a, float b) {
    float div = a / b;
    float sign = __builtin_sign(div);
    return a - b * __builtin_floor(div * sign) * sign;
}
[[inline]] float __builtin_sign(float a) { return a < 0 ? -1 : 1; }
[[inline]] float __builtin_xor(float a, float b) { return (a | b) & ~(a & b); }
