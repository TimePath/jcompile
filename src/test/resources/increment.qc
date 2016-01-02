#include "defs.qh"

void main() {
    float a = 1;
    assert(++a == 2, "++a fail");
    assert((a += 1) == 3, "a+=1 fail");

    float b = 1;
    assert(--b == 0, "--b fail");
    assert((b -= 1) == -1, "b-=1 fail");

    float c = 1;
    assert(c++ == 1, "c++ fail");
    assert(c == 2, "c++ fail");

    float d = 1;
    assert(d-- == 1, "d-- fail");
    assert(d == 0, "d-- fail");
}
