float glob1, glob2;
void main() {
    glob1 = 1;
    glob2 = 2;
    assert(glob1 != glob2, "References clobbered");
    {
        float a, b, c;
        a = 1;
        {
            float d, e, f;
            d = 2;
        }
    }
    {
        float g, h, i;
        g = 2;
    }
}
