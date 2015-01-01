package com.timepath.quakec.vm.defs

import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.vm.defs.ProgramData
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString
class Statement {
    Instruction op
    short a, b, c

    Statement(short op, short a, short b, short c) {
        this.op = Instruction.from(op)
        this.a = a
        this.b = b
        this.c = c
    }

    ProgramData loader

    int call(ProgramData data) {
        op.call(this, data)
    }

    @Override
    String toString() {
        op.toString(this, loader)
    }
}