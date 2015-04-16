package com.timepath.compiler.backends.q1vm.gen

import com.timepath.compiler.ast.Expression
import com.timepath.q1vm.Instruction
import com.timepath.q1vm.ProgramData

open class IR(val instr: Instruction? = null,
              val args: Array<Int> = array(),
              open val ret: Int = 0,
              val name: String? = null) {

    override fun toString(): String {
        val csv = args.map { '$' + it.toString() }.join(", ")
        val comment = if (name != null) " /* $name */" else ""
        return "$instr($csv)$comment"
    }

    open val real = true

}

open class FakeIR(val repr: String) : IR() {
    override val real = false
    override fun toString(): String = repr
}

class ReferenceIR(override val ret: Int) : FakeIR("ref $ret")
class FunctionIR(val function: ProgramData.Function) : FakeIR("function")
class LabelIR(val id: String) : FakeIR("label $id")
class CaseIR(val expr: Expression?) : FakeIR("case $expr")
