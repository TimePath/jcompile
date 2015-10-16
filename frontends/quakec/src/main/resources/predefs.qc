#define _(x) x
#define not(x) (!(x))
#define enumflags enum : flags
[[inline]] int __builtin_floor(float a) { return (int) a | 0; }
[[inline]] bool __builtin_logxor(int a, int b) { return !a != !b; }
[[inline]] int __builtin_sign(float a) { return a < 0 ? -1 : 1; }
[[inline]] float __builtin_mod(float a, float b) {
    float div = a / b;
    int sign = __builtin_sign(div);
    return a - b * __builtin_floor(div * sign) * sign;
}
[[inline]] int __builtin_xor(int a, int b) { return (a | b) & ~(a & b); }
