package com.timepath.quakec.ast

import com.timepath.quakec.vm.Instruction
import groovy.transform.TupleConstructor
import org.antlr.v4.runtime.misc.Utils

@TupleConstructor
class IR {
    Instruction instr
    int[] args = []
    int ret
    String name = ''
    boolean dummy = false

    @Override
    public String toString() {
        "IR { $instr(${args.collect { '$' + it }.join(', ')}) -> \$$ret ${"/* ${Utils.escapeWhitespace(name, false)} */"} }"
    }
}
