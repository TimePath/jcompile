package com.timepath.quakec.vm.defs

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

@CompileStatic
@TupleConstructor(excludes = 'loader')
class Definition {
    short type, offset
    int nameOffset

    ProgramData data

    public String getName() {
        data.strings[this.nameOffset]
    }

    @Override
    public String toString() {
        return """\
Definition{
    type=$type,
    offset=$offset,
    name=$name
}"""
    }
}