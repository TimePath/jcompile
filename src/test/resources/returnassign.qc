#include "defs.qh"

[[accumulate]] entity make(entity e) { return = e; }
.bool flag;
[[accumulate]] entity make(entity e)
{
    e.flag = true;
    spawn(); // attempt to clobber
}

[[accumulate]] entity make_2(entity e, float i) { return = e; }
.bool flag2;
[[accumulate]] entity make_2(entity e, float i)
{
    make(e);
    e.flag2 = true;
    spawn(); // attempt to clobber
}

entity world;
void main() {
    entity e = make_2(spawn(), 1);
    assert(e.flag);
    assert(e.flag2);
}
