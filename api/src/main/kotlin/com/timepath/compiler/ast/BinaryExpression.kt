package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext as PRC
import com.timepath.compiler.gen.type

abstract class BinaryExpression(val op: String, val left: Expression, val right: Expression, override val ctx: PRC? = null) : Expression() {

    {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    open fun handler(gen: Generator) = Type.handle(Type.Operation(op, left.type(gen), right.type(gen)))

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

    class Add(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("+", left, right, ctx)

    class AddAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("+=", left, right, ctx)

    class Subtract(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("-", left, right, ctx)

    class SubtractAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("-=", left, right, ctx)

    class Multiply(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("*", left, right, ctx)

    class MultiplyAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("*=", left, right, ctx)

    class Divide(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("/", left, right, ctx)

    class DivideAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("/=", left, right, ctx)

    class Modulo(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("%", left, right, ctx)

    class ModuloAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("%=", left, right, ctx)

}
