package com.timepath.quakec.ast

import org.antlr.v4.runtime.misc.Utils
import com.timepath.quakec.vm.Instruction

class IR(val instr: Instruction? = null,
         val args: Array<Int> = array(),
         val ret: Int = 0,
         val name: String? = null,
         val dummy: Boolean = false) {

    override fun toString(): String {
        val csv = args.map { '$' + it.toString() }.join(", ")
        val comment = if (name != null) " /* ${Utils.escapeWhitespace(name, false)} */" else ""
        return "$instr($csv)$comment"
    }

}
