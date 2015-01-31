float glob;

void test(float n) {
    glob = 0;
    switch (n) {
        case 1:
            ++glob;
            // fallthrough
        case 2:
            ++glob;
            break;
        case 3:
            glob = -3;
            switch (glob) {
                case -3: break;
                default: glob++;
            }
            return;
        default:
            glob = -1;
            return;
    }
}

void main() {
    test(0);
    assert(glob == -1, "Switch fail 0");
    test(1);
    assert(glob == 2, "Switch fail 1");
    test(2);
    assert(glob == 1, "Switch fail 2");
    test(3);
    assert(glob == -3, "Switch fail 3");
}
