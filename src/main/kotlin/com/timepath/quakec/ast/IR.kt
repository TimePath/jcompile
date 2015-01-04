package com.timepath.quakec.ast

import org.antlr.v4.runtime.misc.Utils
import com.timepath.quakec.vm.Instruction

class IR(val instr: Instruction? = null,
         val args: Array<Int> = array(),
         val ret: Int = 0,
         val name: String = "",
         val dummy: Boolean = false) {

    override fun toString() = "IR { $instr(${args.map { '$' + it.toString() }.join(", ")}) -> \$$ret ${"/* ${Utils.escapeWhitespace(name, false)} */"} }"

}
