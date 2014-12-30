package com.timepath.quakec.vm

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor

@CompileStatic
@ToString
@TupleConstructor
class Statement {
    short op, a, b, c

    int exec(int fp) {
        Instruction.from(op)(this)
        return fp + 1
    }
}