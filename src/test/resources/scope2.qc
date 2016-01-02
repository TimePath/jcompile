#include "defs.qh"

void func() {}
void func2(int func);
void func2(int funcalias) { funcalias++; }

void main() {
    void() oldfunc = func;
    func2(0);
    assert(func == oldfunc, "Function clobbered");
}
