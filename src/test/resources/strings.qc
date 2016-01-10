#include "defs.qh"

string glob = "glob.";
string string_null = nil;

void main() {
    // truthy empty strings are useful for file parsing where lines may be empty, but there are still more lines
    assert(!string_null, "True null string");
    assert("", "False empty string");
    assert(".", "False non-empty string");
    assert(glob == glob, "Strings ref == ref == false");
    assert("glob." == "glob.", "Strings val == val == false");
    assert(glob == "glob.", "Strings ref == val == false");
}
