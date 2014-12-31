package com.timepath.quakec.vm

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor

@CompileStatic
@TupleConstructor(excludes = 'loader')
class Definition {
    short type, offset
    int nameOffset

    Loader loader

    public String getName() {
        loader.strings[this.nameOffset]
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