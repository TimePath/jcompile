#include "defs.qh"

float array[2];

void main() {
    for (int i = 0; i < array_size; i++) {
        assert(array[i] == 0, "Initial value");
    }
    int test = 0;
    int expect = 2;
    float(bool, float) index = __array_access(test);
    assert(index(1, expect) == expect, "Result of assignment");
    assert(array[test] == expect, "Read");
}
