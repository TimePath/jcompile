#include "defs.qh"

int glob = 1;

void main() {
    {
        int glob = 2;
        ++glob;
    }
    assert(glob == 1, "Global clobbered");
}
