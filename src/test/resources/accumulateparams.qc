#include "defs.qh"

void dummy(float f) {}

float test(float f) {
    dummy(f + 1);
}

[[accumulate]] float test(float f) {
    return f;
}

void main() {
    assert(test(0) == 0, "Duplicate param store");
}
