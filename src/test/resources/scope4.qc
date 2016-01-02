#include "defs.qh"

int glob = 1;

[[accumulate]] void main() {
    int glob = 2;
}

[[accumulate]] void main() {
    assert(glob == 1, "Global shadowed");
}
