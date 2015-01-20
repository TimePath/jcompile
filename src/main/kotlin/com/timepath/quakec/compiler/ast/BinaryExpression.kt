package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.Type
import com.timepath.quakec.compiler.ast.Expression as rvalue
import com.timepath.quakec.compiler.ast.ReferenceExpression as lvalue
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import org.antlr.v4.runtime.ParserRuleContext

abstract class BinaryExpression<L : Expression, R : Expression>(val op: String, val left: L, val right: R, ctx: ParserRuleContext? = null) : rvalue(ctx) {

    {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    override fun generate(ctx: Generator): List<IR> = Type.handle(Type.Operation(op, Type.Float, Type.Float))(ctx, left, right)

    class Comma(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(",", left, right, ctx)

    class Assign(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("=", left, right, ctx)

    class Or(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("||", left, right, ctx)

    class And(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("&&", left, right, ctx)

    class BitOr(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("|", left, right, ctx)

    class BitXor(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("^", left, right, ctx)

    class BitAnd(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("&", left, right, ctx)

    class Eq(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("==", left, right, ctx)

    class Ne(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("!=", left, right, ctx)

    class Lsh(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("<<", left, right, ctx)

    class Rsh(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(">>", left, right, ctx)

    class Lt(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("<", left, right, ctx)

    class Le(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("<=", left, right, ctx)

    class Gt(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(">", left, right, ctx)

    class Ge(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>(">=", left, right, ctx)

    class Add(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("+", left, right, ctx)

    class Sub(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("-", left, right, ctx)

    class Mul(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("*", left, right, ctx)

    class Div(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("/", left, right, ctx)

    class Mod(left: rvalue, right: rvalue, ctx: ParserRuleContext? = null) : BinaryExpression<rvalue, rvalue>("%", left, right, ctx)

}
