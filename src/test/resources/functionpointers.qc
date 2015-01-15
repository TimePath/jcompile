float glob;

void mutate() {
    glob = 1;
}

void call(void() f) {
    f();
}

void main() {
    call(mutate);
    assert(glob == 1, "Execution failed");
}