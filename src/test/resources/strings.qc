#include "defs.qh"

string glob = "glob.";

void main() {
    assert(!"", "True empty string");
    assert(".", "False non-empty string");
    assert(glob == glob, "Strings ref == ref == false");
    assert("glob." == "glob.", "Strings val == val == false");
    assert(glob == "glob.", "Strings ref == val == false");
}
