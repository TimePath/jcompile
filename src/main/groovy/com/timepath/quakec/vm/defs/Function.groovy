package com.timepath.quakec.vm.defs

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

@CompileStatic
@TupleConstructor(excludes = 'data')
class Function {
    int firstStatement, firstLocal, numLocals, profiling, nameOffset, fileNameOffset, numParams
    byte[] sizeof

    ProgramData data

    public String getName() {
        data.strings[this.nameOffset]
    }

    public String getFileName() {
        data.strings[this.fileNameOffset]
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