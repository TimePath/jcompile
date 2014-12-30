package com.timepath.quakec.vm

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor

@CompileStatic
@ToString
@TupleConstructor(excludes = 'loader')
class Function {
    int firstStatement, firstLocal, numLocals, profiling, nameOffset, fileNameOffset, numParams
    byte[] sizeof

    Loader loader

    public String getName() {
        loader.stringData.get(this.nameOffset)
    }
}