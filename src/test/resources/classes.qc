#include "defs.qh"

#define entityclass(name) entityclass name {}
#define class(name) [[class(name)]]
#define new(class) ((class) spawn())

entityclass(A);
class(A) .int justA;

entityclass(B);
class(B) .int justB;

void main() {
    entity e = spawn();
    A a = new(A);
    B b = new(B);
    e = a;
    e = b;
}
