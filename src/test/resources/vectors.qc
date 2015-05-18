#include "defs.qh"

void main() {
    vector v = [1, 2, 3];
    assert(v.x == 1, "v.x != 1");
    assert(v.y == 2, "v.y != 2");
    assert(v.z == 3, "v.z != 3");
    v = [v.z, v.y, v.x];
    assert(v.x == 3, "v.x != 3");
    assert(v.y == 2, "v.y != 2");
    assert(v.z == 1, "v.z != 1");
    v.x += 1;
    v.y += 1;
    v.z += 1;
    assert(v.x == 4, "v.x != 4");
    assert(v.y == 3, "v.y != 3");
    assert(v.z == 2, "v.z != 2");
    v += [1, 1, 1];
    print(ftos(v.x));
    assert(v.x == 5, "v.x != 5");
    assert(v.y == 4, "v.y != 4");
    assert(v.z == 3, "v.z != 3");
}
