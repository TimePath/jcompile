package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.Value
import com.timepath.compiler.ast.Expression as rvalue
import com.timepath.compiler.ast.ReferenceExpression as lvalue
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import org.antlr.v4.runtime.ParserRuleContext

abstract class BinaryExpression<L : Expression, R : Expression>(val op: String, val left: L, val right: R, ctx: ParserRuleContext? = null) : rvalue(ctx) {

    {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    open fun handler(gen: Generator) = Type.handle(Type.Operation(op, left.type(gen), right.type(gen)))

    override fun type(gen: Generator) = handler(gen).type

    override fun generate(gen: Generator): List<IR> = handler(gen)(gen, left, right)

    class Comma(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(",", left, right, ctx)

    class Assign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("=", left, right, ctx)

    class Or(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("||", left, right, ctx)

    class And(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("&&", left, right, ctx)

    class BitOr(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("|", left, right, ctx)

    class OrAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("|=", left, right, ctx)

    class ExclusiveOr(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("^", left, right, ctx)

    class ExclusiveOrAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("^=", left, right, ctx)

    class BitAnd(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("&", left, right, ctx)

    class AndAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("&=", left, right, ctx)

    class Eq(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("==", left, right, ctx)

    class Ne(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("!=", left, right, ctx)

    class Lsh(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("<<", left, right, ctx)

    class LshAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("<<=", left, right, ctx)

    class Rsh(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(">>", left, right, ctx)

    class RshAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(">>=", left, right, ctx)

    class Lt(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("<", left, right, ctx)

    class Le(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("<=", left, right, ctx)

    class Gt(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(">", left, right, ctx)

    class Ge(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(">=", left, right, ctx)

    class Add(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("+", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.plus(right.evaluate())
    }

    class AddAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("+=", left, right, ctx)

    class Subtract(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("-", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.minus(right.evaluate())
    }

    class SubtractAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("-=", left, right, ctx)

    class Multiply(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("*", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.times(right.evaluate())
    }

    class MultiplyAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("*=", left, right, ctx)

    class Divide(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("/", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.div(right.evaluate())
    }

    class DivideAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("/=", left, right, ctx)

    class Modulo(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("%", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.mod(right.evaluate())
    }

    class ModuloAssign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("%=", left, right, ctx)

}