#include "defs.qh"

void main() {
    assert(!"", "True empty string");
    assert(".", "False non-empty string");
}
