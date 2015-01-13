float sum(float x, float y, float z) {
    return x + y + z;
}

void main() {
    float a = 1;
    float b = 2;
    float c = 3;

    float f; f = sum(sum(a, sum(a, b, c), c),
            sum(sum(sum(a, b, c), b, sum(a, b, c)), b, sum(a, b, sum(a, b, c))),
            sum(sum(a, b, c), b, c));

    assert(f == 46, "Return value clobbered");
};