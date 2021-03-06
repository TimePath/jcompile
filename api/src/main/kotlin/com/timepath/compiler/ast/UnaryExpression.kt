package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import org.antlr.v4.runtime.ParserRuleContext as PRC

public fun Expression.inv() = UnaryExpression.BitNot(this, null)
public operator fun Expression.not() = UnaryExpression.Not(this, null)
public operator fun Expression.unaryMinus() = UnaryExpression.Minus(this, null)
public operator fun Expression.unaryPlus() = UnaryExpression.Plus(this, null)
public infix fun Expression.to(type: Type) = UnaryExpression.Cast(type, this, null)
public fun Expression.address() = UnaryExpression.Address(this, null)
public fun Expression.deref() = UnaryExpression.Dereference(this, null)

public abstract class UnaryExpression protected constructor(val op: String, val operand: Expression, override val ctx: PRC?) : Expression() {

    init {
        add(operand)
    }

    override fun toString(): String = "$op($operand)"

    class Cast(val type: Type, operand: Expression, ctx: PRC?) : UnaryExpression("($type)", operand, ctx) {
        override val simpleName = "Cast"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = Cast(type, children.single(), ctx)

        override fun toString(): String = "(($type) $operand)"
    }

    public abstract class Post protected constructor(op: String, operand: Expression, ctx: PRC?) : UnaryExpression(op, operand, ctx) {

        override fun toString(): String = "($operand $op)"
    }

    class PostIncrement(operand: Expression, ctx: PRC?) : UnaryExpression.Post("++", operand, ctx) {
        override val simpleName = "PostIncrement"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = PostIncrement(children.single(), ctx)
    }

    class PostDecrement(operand: Expression, ctx: PRC?) : UnaryExpression.Post("--", operand, ctx) {
        override val simpleName = "PostDecrement"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = PostDecrement(children.single(), ctx)
    }

    class PreIncrement(operand: Expression, ctx: PRC?) : UnaryExpression("++", operand, ctx) {
        override val simpleName = "PreIncrement"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = PreIncrement(children.single(), ctx)
    }

    class PreDecrement(operand: Expression, ctx: PRC?) : UnaryExpression("--", operand, ctx) {
        override val simpleName = "PreDecrement"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = PreDecrement(children.single(), ctx)
    }

    class Address(operand: Expression, ctx: PRC?) : UnaryExpression("&", operand, ctx) {
        override val simpleName = "Address"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = Address(children.single(), ctx)
    }

    class Dereference(operand: Expression, ctx: PRC?) : UnaryExpression("*", operand, ctx) {
        override val simpleName = "Dereference"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = Dereference(children.single(), ctx)
    }

    class Plus(operand: Expression, ctx: PRC?) : UnaryExpression("+", operand, ctx) {
        override val simpleName = "Plus"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = Plus(children.single(), ctx)
    }

    class Minus(operand: Expression, ctx: PRC?) : UnaryExpression("-", operand, ctx) {
        override val simpleName = "Minus"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = Minus(children.single(), ctx)
    }

    class BitNot(operand: Expression, ctx: PRC?) : UnaryExpression("~", operand, ctx) {
        override val simpleName = "BitNot"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = BitNot(children.single(), ctx)
    }

    class Not(operand: Expression, ctx: PRC?) : UnaryExpression("!", operand, ctx) {
        override val simpleName = "Not"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = Not(children.single(), ctx)
    }
}
