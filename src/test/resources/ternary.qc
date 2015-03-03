#include "defs.qh"

void main() {
    float a;
    a = 7 ? 1 : 0;
    assert(a == 1, "Ternary return fail");
}
