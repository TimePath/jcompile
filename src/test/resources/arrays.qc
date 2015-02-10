float array[2];

void main() {
    for(float f = 0; f < array_size; ++f) {
        float(bool, float) v = __array_access(f);
        assert(v(0) == f, "Initial value");
    }
    float(bool, float) index = __array_access(0);
    assert(index(1, 2) == 2, "Result of assignment");
    assert(index(0) == 2, "Read");
}
