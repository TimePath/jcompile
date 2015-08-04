#include "defs.qh"

vector glob;

void test(vector v) {
    glob = v;
}

void main() {
    vector v = '1 2 3';
    test(v);
    assert(glob.x == v.x, "glob.x != v.x");
    assert(glob.y == v.y, "glob.y != v.y");
    assert(glob.z == v.z, "glob.z != v.z");
    assert(glob == v, "glob != v");
}
