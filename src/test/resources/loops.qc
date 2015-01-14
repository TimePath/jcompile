void main() {
    float f;
    float test;

    test = 0;
    for(f = 0; f < 1; ++f) {
        ++test;
    }
    assert(test == 1, "Expected 1 loop")

    test = 0;
    f = 3;
    while(--f) {
        ++test;
    }
    assert(test == 2, "Expected 2 loops")

    test = 0;
    f = 4;
    do {
        ++test;
    } while(--f)
    assert(test == 3, "Expected 3 loops")
}