package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.QCParser

abstract class UnaryExpression<T : Expression>(val operand: T, ctx: ParserRuleContext? = null) : Expression(ctx) {

    abstract val op: String

    {
        add(operand)
    }

    override fun toString(): String = "($op $operand)"

    class PreIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression<Expression>(operand, ctx) {
        override val op = "++"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Assign(operand, BinaryExpression.Add(operand, ConstantExpression(1f))).doGenerate(ctx)
        }
    }

    class PreDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression<Expression>(operand, ctx) {
        override val op = "--"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Assign(operand, BinaryExpression.Sub(operand, ConstantExpression(1f))).doGenerate(ctx)
        }
    }

    class Plus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression<Expression>(operand, ctx) {
        override val op = "+"
        override fun generate(ctx: Generator): List<IR> {
            return operand.doGenerate(ctx)
        }
    }

    class Minus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression<Expression>(operand, ctx) {
        override val op = "-"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Sub(ConstantExpression(0f), operand).doGenerate(ctx)
        }
    }

    class BitNot(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression<Expression>(operand, ctx) {
        override val op = "~"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Sub(ConstantExpression(-1f), operand).doGenerate(ctx)
        }
    }

    class Not(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression<Expression>(operand, ctx) {
        override val op = "!"
        override fun generate(ctx: Generator): List<IR> {
            return BinaryExpression.Eq(ConstantExpression(0f), operand).doGenerate(ctx)
        }
    }
}