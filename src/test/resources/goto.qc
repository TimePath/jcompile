void main() {
    goto end;
    assert(0, "Goto didn't skip");
    :end
    ;
}