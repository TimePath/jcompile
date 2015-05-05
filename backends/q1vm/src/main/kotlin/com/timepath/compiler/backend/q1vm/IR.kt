package com.timepath.compiler.backend.q1vm

import com.timepath.q1vm.Instruction
import com.timepath.q1vm.ProgramData

open class IR(val instr: Instruction? = null,
              val args: Array<Int> = array(),
              /** Continuation passing */
              open val ret: Int = 0,
              val name: String) {

    override fun toString(): String {
        val s = "$instr(${args.map { "$" + it }.join(", ")})"
        return "$s /* $name */"
    }

    val real = this !is Fake

    private abstract class Fake(val repr: String) : IR(name = repr) {
        override fun toString(): String = "/* $repr */"
    }

    class Return(override val ret: Int) : Fake("return = $$ret")
    class Function(s: String, val function: ProgramData.Function) : Fake("function $s")
    class Label(val id: String) : Fake("label $id")
}
