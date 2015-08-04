#include "defs.qh"

.vector test;

void main() {
    vector v = [1, 2, 3];
    entity e = spawn();
    e.test = v;
    assert(e.test.x == 1, "Failed to store x");
    assert(e.test.y == 2, "Failed to store y");
    assert(e.test.z == 3, "Failed to store z");
}
