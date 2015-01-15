void main() {
    {
        float test = 0;
        for(float f = 0; f < 1; ++f) {
            ++test;
        }
        assert(test == 1, "Expected 1 loop");
    }
    {
        float test = 0;
        float f = 3;
        while(--f) {
            ++test;
        }
        assert(test == 2, "Expected 2 loops");
    }
    {
        float test = 0;
        float f = 4;
        do {
            ++test;
        } while(--f);
        assert(test == 3, "Expected 3 loops");
    }
    {
        float test = 0;
        for(float f = 0; f < 10; ++f) {
            ++test;
            if (test == 5)
                break;
        }
        assert(test == 5, "Expected 5 loops");
    }
    {
        float test = 0;
        for(float f = 0; f < 10; ++f) {
            ++test;
            continue;
        }
        assert(test == 10, "Expected 10 loops");
    }
}