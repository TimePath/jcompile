package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.QCParser

abstract class UnaryExpression(val operand: Expression, ctx: ParserRuleContext? = null) : Expression(ctx) {

    abstract val op: String

    {
        add(operand)
    }

    override fun toString(): String = "($op $operand)"

    abstract class Post(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(operand, ctx) {
        override fun toString(): String = "($operand $op)"
    }

    class PostIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression.Post(operand, ctx) {
        override val op = "++"
        override fun generate(ctx: Generator): List<IR> {
            with(linkedListOf<IR>()) {
                val add = BinaryExpression.Add(operand, ConstantExpression(1f))
                val assign = BinaryExpression.Assign(operand, add)
                // FIXME
                val sub = BinaryExpression.Sub(assign, ConstantExpression(1f))
                addAll(sub.doGenerate(ctx))
                return this
            }
        }
    }

    class PostDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression.Post(operand, ctx) {
        override val op = "--"
        override fun generate(ctx: Generator): List<IR> {
            with(linkedListOf<IR>()) {
                val sub = BinaryExpression.Sub(operand, ConstantExpression(1f))
                val assign = BinaryExpression.Assign(operand, sub)
                // FIXME
                val add = BinaryExpression.Add(assign, ConstantExpression(1f))
                addAll(add.doGenerate(ctx))
                return this
            }
        }
    }

    class PreIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(operand, ctx) {
        override val op = "++"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Assign(operand, BinaryExpression.Add(operand, ConstantExpression(1f))).doGenerate(ctx)
        }
    }

    class PreDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(operand, ctx) {
        override val op = "--"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Assign(operand, BinaryExpression.Sub(operand, ConstantExpression(1f))).doGenerate(ctx)
        }
    }

    class Plus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(operand, ctx) {
        override val op = "+"
        override fun generate(ctx: Generator): List<IR> {
            return operand.doGenerate(ctx)
        }
    }

    class Minus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(operand, ctx) {
        override val op = "-"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Sub(ConstantExpression(0f), operand).doGenerate(ctx)
        }
    }

    class BitNot(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(operand, ctx) {
        override val op = "~"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Sub(ConstantExpression(-1f), operand).doGenerate(ctx)
        }
    }

    class Not(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(operand, ctx) {
        override val op = "!"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Eq(ConstantExpression(0f), operand).doGenerate(ctx)
        }
    }
}