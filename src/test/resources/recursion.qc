#include "defs.qh"

int rec(int n) {
    if (n == 0) return 0;
    return n + rec(n - 1);
}

void main() {
    int n = rec(2);
    print(ftos(n));
    assert(n == 3, "Stack clobbered");
}
