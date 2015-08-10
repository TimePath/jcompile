#include "defs.qh"

void main() {
    vector v = '1 2 3';
    assert(v.x == 1, "v.x != 1");
    assert(v.y == 2, "v.y != 2");
    assert(v.z == 3, "v.z != 3");
}
