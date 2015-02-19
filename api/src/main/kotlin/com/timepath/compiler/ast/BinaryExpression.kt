package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.Value
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import org.antlr.v4.runtime.ParserRuleContext as PRC

abstract class BinaryExpression(val op: String, val left: Expression, val right: Expression, ctx: PRC? = null) : Expression(ctx) {

    {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    open fun handler(gen: Generator) = Type.handle(Type.Operation(op, left.type(gen), right.type(gen)))

    override fun type(gen: Generator) = handler(gen).type

    override fun generate(gen: Generator): List<IR> = handler(gen)(gen, left, right)

    class Comma(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(",", left, right, ctx)

    class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("=", left, right, ctx)

    class Or(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("||", left, right, ctx)

    class And(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&&", left, right, ctx)

    class BitOr(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("|", left, right, ctx)

    class OrAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("|=", left, right, ctx)

    class ExclusiveOr(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("^", left, right, ctx)

    class ExclusiveOrAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("^=", left, right, ctx)

    class BitAnd(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&", left, right, ctx)

    class AndAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&=", left, right, ctx)

    class Eq(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("==", left, right, ctx)

    class Ne(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("!=", left, right, ctx)

    class Lsh(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<<", left, right, ctx)

    class LshAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<<=", left, right, ctx)

    class Rsh(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">>", left, right, ctx)

    class RshAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">>=", left, right, ctx)

    class Lt(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<", left, right, ctx)

    class Le(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<=", left, right, ctx)

    class Gt(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">", left, right, ctx)

    class Ge(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">=", left, right, ctx)

    class Add(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("+", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.plus(right.evaluate())
    }

    class AddAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("+=", left, right, ctx)

    class Subtract(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("-", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.minus(right.evaluate())
    }

    class SubtractAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("-=", left, right, ctx)

    class Multiply(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("*", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.times(right.evaluate())
    }

    class MultiplyAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("*=", left, right, ctx)

    class Divide(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("/", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.div(right.evaluate())
    }

    class DivideAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("/=", left, right, ctx)

    class Modulo(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("%", left, right, ctx) {
        override fun evaluate(): Value? = left.evaluate()?.mod(right.evaluate())
    }

    class ModuloAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("%=", left, right, ctx)

}
