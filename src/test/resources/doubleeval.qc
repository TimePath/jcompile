#include "defs.qh"

float glob = 0;
.float f;
entity e;

entity test() {
    ++glob;
    return e;
}

void main() {
    e = spawn();
    e.f = 1;
    assert(e.f == 1, "Simple entity field assignment");
    e.f += 2;
    assert(e.f == 3, "Simple entity field increment");
    test().f = 4;
    assert(e.f == 4, "Complex entity field assignment");
    assert(glob == 1, "Double evaluation");
    test().f += 5;
    assert(e.f == 9, "Complex entity field increment");
    assert(glob == 2, "Double evaluation");
}
