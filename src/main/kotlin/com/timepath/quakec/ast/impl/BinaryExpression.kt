package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression as rvalue
import com.timepath.quakec.ast.impl.ReferenceExpression as lvalue
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.Statement

abstract class BinaryExpression<L : Expression, R : Expression>(val left: L,
                                                                val right: R) : rvalue() {

    abstract val instr: Instruction
    abstract val op: String

    override val attributes: Map<String, Any>
        get() = mapOf()

    override val children: MutableList<Statement>
        get() = arrayListOf(left, right)

    override fun generate(ctx: GenerationContext): List<IR> {
        val genL = left.generate(ctx)
        val genR = right.generate(ctx)
        val allocate = ctx.registry.register(null)
        return genL + genR + IR(instr, array(genL.last().ret, genR.last().ret, allocate), allocate, "${left} $op ${right}")
    }

    class Assign(left: lvalue, right: rvalue) : BinaryExpression<lvalue, rvalue>(left, right) {
        override val instr = Instruction.STORE_FLOAT
        override val op = "="

        override fun generate(ctx: GenerationContext): List<IR> {
            // left = right
            val genL = left.generate(ctx)
            val genR = right.generate(ctx)
            return genL + genR + IR(instr, array(genR.last().ret, genL.last().ret), genL.last().ret, "=")
        }
    }

    class Add(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.ADD_FLOAT
        override val op = "+"
    }

    class Sub(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.SUB_FLOAT
        override val op = "-"
    }

    class Mul(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.MUL_FLOAT
        override val op = "*"
    }

    class Div(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.DIV_FLOAT
        override val op = "/"
    }

    class Eq(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.EQ_FLOAT
        override val op = "=="
    }

    class Ne(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.NE_FLOAT
        override val op = "!="
    }

    class Lt(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.LT
        override val op = "<"
    }

    class Le(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.LE
        override val op = "<="
    }

    class Gt(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.GT
        override val op = ">"
    }

    class Ge(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.GE
        override val op = ">="
    }

    class And(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.AND
        override val op = "&&"
    }

    class BitAnd(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.BITAND
        override val op = "&"
    }

    class Or(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.OR
        override val op = "||"
    }

    class BitOr(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.BITOR
        override val op = "|"
    }
}
