#include "defs.qh"

float glob;

float test(float x) {
    glob = x;
    return x;
}

void main() {
    float f = test(1) + test(2);
    assert(glob == 2, "Bad order");
}
