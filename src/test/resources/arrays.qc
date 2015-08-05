#include "defs.qh"

float array[2];

void main() {
    for (int i = 0; i < 2; i++) {
        assert(array[i] == 0, "Initial value");
    }
    int test = 0;
    int expect = 1;
    assert((array[test] = expect) == expect, "Result of assignment");
    assert(array[test] == expect, "Read");
}
