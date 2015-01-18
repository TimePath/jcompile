package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.ast.Expression as rvalue
import com.timepath.quakec.compiler.ast.ReferenceExpression as lvalue
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

abstract class BinaryExpression<L : Expression, R : Expression>(val left: L, val right: R, ctx: ParserRuleContext? = null) : rvalue(ctx) {

    abstract val instr: Instruction
    abstract val op: String

    {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    override fun generate(ctx: Generator): List<IR> {
        // ast:
        // temp(c) = left(a) op right(b)
        // vm:
        // c (=) a (op) b
        val genL = left.doGenerate(ctx)
        val genR = right.doGenerate(ctx)
        val global = ctx.allocator.allocateReference()
        return (genL + genR
                + IR(instr, array(genL.last().ret, genR.last().ret, global.ref), global.ref, this.toString()))
    }

    class Comma(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.AND // TODO
        override val op = ","
    }

    class Assign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.STORE_FLOAT
        override val op = "="
        override fun generate(ctx: Generator): List<IR> {
            // ast:
            // left(a) = right(b)
            // vm:
            // b (=) a
            val left = when (left) {
                is BinaryExpression.Dot -> {
                    // make a copy to avoid changing the right half of the assignment
                    val special = BinaryExpression.Dot(left.left, left.right, ctx = this.ctx)
                    special.instr = Instruction.ADDRESS
                    special
                }
                else -> left
            }
            val instr = when {
                left is BinaryExpression.Dot -> {
                    Instruction.STOREP_FLOAT
                }
                else -> instr
            }
            val genL = left.doGenerate(ctx)
            val genR = right.doGenerate(ctx)
            return (genL + genR
                    + IR(instr, array(genR.last().ret, genL.last().ret), genL.last().ret, this.toString()))
        }
    }

    class Or(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.OR
        override val op = "||"
        override fun generate(ctx: Generator): List<IR> {
            return ConditionalExpression(left,
                    pass = ConstantExpression(1),
                    fail = ConditionalExpression(right,
                            pass = ConstantExpression(1),
                            fail = ConstantExpression(0))
            ).doGenerate(ctx)
        }
    }

    class And(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.AND
        override val op = "&&"
        override fun generate(ctx: Generator): List<IR> {
            return ConditionalExpression(left,
                    fail = ConstantExpression(0),
                    pass = ConditionalExpression(right,
                            fail = ConstantExpression(0),
                            pass = ConstantExpression(1))
            ).doGenerate(ctx)
        }
    }

    class BitOr(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.BITOR
        override val op = "|"
    }

    class BitXor(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.NE_FLOAT // TODO
        override val op = "^"
    }

    class BitAnd(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.BITAND
        override val op = "&"
    }

    class Eq(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.EQ_FLOAT
        override val op = "=="
    }

    class Ne(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.NE_FLOAT
        override val op = "!="
    }

    class Lsh(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.ADD_FLOAT // TODO
        override val op = "<<"
    }

    class Rsh(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.ADD_FLOAT // TODO
        override val op = ">>"
    }

    class Lt(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.LT
        override val op = "<"
    }

    class Le(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.LE
        override val op = "<="
    }

    class Gt(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.GT
        override val op = ">"
    }

    class Ge(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.GE
        override val op = ">="
    }

    class Add(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.ADD_FLOAT
        override val op = "+"
    }

    class Sub(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.SUB_FLOAT
        override val op = "-"
    }

    class Mul(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.MUL_FLOAT
        override val op = "*"
    }

    class Div(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.DIV_FLOAT
        override val op = "/"
    }

    class Mod(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override val instr = Instruction.DIV_FLOAT // TODO
        override val op = "%"
    }

    class Dot(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(left, right, ctx) {
        override var instr = Instruction.LOAD_FLOAT
        override val op = "."
    }
}