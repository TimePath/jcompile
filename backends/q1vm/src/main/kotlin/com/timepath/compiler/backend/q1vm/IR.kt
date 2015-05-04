package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.ast.Expression
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

    val real = this !is FakeIR

}

private abstract class FakeIR(val repr: String) : IR(name = repr) {
    override fun toString(): String = "/* $repr */"
}

class ReturnIR(override val ret: Int) : FakeIR("return = $$ret")
class FunctionIR(s: String, val function: ProgramData.Function) : FakeIR("function $s")
class LabelIR(val id: String) : FakeIR("label $id")
