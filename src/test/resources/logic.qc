#include "defs.qh"

float glob;

void mutate() {
    glob = 1;
}

void main() {
    float result;
    glob = 0;
    result = 1 || mutate();
    assert(glob == 0, "Short circuit fail");
    glob = 0;
    result = 0 && mutate();
    assert(glob == 0, "Short circuit fail");
}
