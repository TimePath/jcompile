#include "defs.qh"

.int a, b;

void main() {
    entity e = spawn();
    e.a = 1;
    e.b = 2;
    assert(a != b, "Same field");
    assert(e.a != e.b, "Same values");
}
