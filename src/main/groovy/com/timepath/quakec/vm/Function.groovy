package com.timepath.quakec.vm

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor

@CompileStatic
@TupleConstructor(excludes = 'loader')
class Function {
    int firstStatement, firstLocal, numLocals, profiling, nameOffset, fileNameOffset, numParams
    byte[] sizeof

    Loader loader

    public String getName() {
        loader.strings[this.nameOffset]
    }

    public String getFileName() {
        loader.strings[this.fileNameOffset]
    }

    @Override
    public String toString() {
        return """\
Function{
    firstStatement=$firstStatement,
    firstLocal=$firstLocal,
    numLocals=$numLocals,
    profiling=$profiling,
    name=$name,
    fileName=$fileName,
    numParams=$numParams,
    sizeof=${Arrays.toString(sizeof)}
}"""
    }
}