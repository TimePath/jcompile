#include "defs.qh"

#if 0
#  define entityclass(name) typedef entity name
#  define new(class) spawn()
#else
#  define entityclass(name) entityclass name {}
#  define new(class) ((class) spawn())
#endif

entityclass(A);

[[class(A)]]
.int justA;

entityclass(B);

[[class(B)]]
.int justB;

void main() {
    entity e = spawn();
    A a = new(A);
    B b = new(B);
    e = a;
    e = b;
}
