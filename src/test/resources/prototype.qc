#include "defs.qh"

void func(int);
void func(int i);

int glob;

void main() {
	func(1);
	assert(glob == 1, "Prototype fail");
}

void func(int g) {
	glob = g;
}
