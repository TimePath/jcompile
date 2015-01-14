package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.ast.Expression as rvalue
import com.timepath.quakec.compiler.ast.ReferenceExpression as lvalue
import com.timepath.quakec.vm.Instruction

abstract class BinaryExpression<L : Expression, R : Expression>(val left: L, val right: R) : rvalue() {

    abstract val instr: Instruction
    abstract val op: String

    {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    class Assign(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.STORE_FLOAT
        override val op = "="
    }

    class Or(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.OR
        override val op = "||"
    }

    class And(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.AND
        override val op = "&&"
    }

    class BitOr(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.BITOR
        override val op = "|"
    }

    class BitXor(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.NE_FLOAT // TODO
        override val op = "^"
    }

    class BitAnd(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.BITAND
        override val op = "&"
    }

    class Eq(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.EQ_FLOAT
        override val op = "=="
    }

    class Ne(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.NE_FLOAT
        override val op = "!="
    }

    class Lsh(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.ADD_FLOAT // TODO
        override val op = "<<"
    }

    class Rsh(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.ADD_FLOAT // TODO
        override val op = ">>"
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

    class Mod(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override val instr = Instruction.DIV_FLOAT // TODO
        override val op = "%"
    }

    class Dot(left: rvalue, right: rvalue) : BinaryExpression<rvalue, rvalue>(left, right) {
        override var instr = Instruction.LOAD_FLOAT
        override val op = "."
    }
}