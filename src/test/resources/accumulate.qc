#include "defs.qh"

.int fld;

void test(entity e) {
    e = spawn();
}

[[accumulate]] void test(entity e) {
    e.fld = 2;
}

[[last]] void test(entity e) {
    assert(e.fld == 2, "Accumulate is broken");
}

void main() {
    test((entity) 0);
}
