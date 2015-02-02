package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.Type

abstract class UnaryExpression(val op: String, val operand: Expression, ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        add(operand)
    }

    override fun toString(): String = "($op $operand)"

    fun handler(gen: Generator) = Type.handle(Type.Operation(op, operand.type(gen)))

    override fun generate(gen: Generator): List<IR> {
        return handler(gen)(gen, operand, null)
    }

    override fun type(gen: Generator) = handler(gen).type

    abstract class Post(op: String, operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(op, operand, ctx) {
        override fun toString(): String = "($operand $op)"
    }

    class PostIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression.Post("++", operand, ctx)

    class PostDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression.Post("--", operand, ctx)

    class PreIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("++", operand, ctx)

    class PreDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("--", operand, ctx)

    class Address(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("&", operand, ctx)

    class Dereference(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("*", operand, ctx)

    class Plus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("+", operand, ctx)

    class Minus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("-", operand, ctx)

    class BitNot(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("~", operand, ctx)

    class Not(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("!", operand, ctx)
}
