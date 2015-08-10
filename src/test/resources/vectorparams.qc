#include "defs.qh"

vector glob1, glob2;

void test(vector v1, vector v2) {
    glob1 = v1;
    glob2 = v2;
}

void main() {
    test('1 2 3', '4 5 6');
    assert(glob1.x == 1,        "glob1.x != 1");
    assert(glob1.y == 2,        "glob1.y != 2");
    assert(glob1.z == 3,        "glob1.z != 3");
    assert(glob1 == '1 2 3',    "glob1 != '1 2 3'");
    assert(glob2.x == 4,        "glob2.x != 4");
    assert(glob2.y == 5,        "glob2.y != 5");
    assert(glob2.z == 6,        "glob2.z != 6");
    assert(glob2 == '4 5 6',    "glob2 != '4 5 6'");
}
