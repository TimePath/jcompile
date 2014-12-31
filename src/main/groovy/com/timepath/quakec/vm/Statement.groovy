package com.timepath.quakec.vm

import groovy.transform.CompileStatic
import groovy.transform.ToString

import java.nio.ByteBuffer

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

    Loader loader

    int call(Loader data) {
        op.call(this, data)
    }

    @Override
    String toString() {
        op.toString(this, loader)
    }
}