#include "defs.qh"

void func();

int glob;

void main() {
	func();
	assert(glob == 1, "Prototype fail");
}

void func() {
	glob = 1;
}
