float glob = 0;
entity e;

entity test() {
    ++glob;
    return e;
}

void main() {
    e = spawn();
    print(ftos(e.f));
    test().f += 7;
    print(ftos(e.f));
    print(ftos(glob));
    assert(glob == 1, "Double evaluation");
}
