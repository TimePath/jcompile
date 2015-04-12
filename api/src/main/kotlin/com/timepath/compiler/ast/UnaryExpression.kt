package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import org.antlr.v4.runtime.ParserRuleContext

abstract class UnaryExpression(val op: String, val operand: Expression, override val ctx: ParserRuleContext? = null) : Expression() {

    init {
        add(operand)
    }

    override fun toString(): String = "$op($operand)"

    class Cast(val type: Type, operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("($type)", operand, ctx) {
        override val simpleName = "Cast"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun toString(): String = "(($type) $operand)"
    }

    abstract class Post(op: String, operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(op, operand, ctx) {

        override fun toString(): String = "($operand $op)"
    }

    class PostIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression.Post("++", operand, ctx) {
        override val simpleName = "PostIncrement"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class PostDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression.Post("--", operand, ctx) {
        override val simpleName = "PostDecrement"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class PreIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("++", operand, ctx) {
        override val simpleName = "PreIncrement"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class PreDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("--", operand, ctx) {
        override val simpleName = "PreDecrement"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Address(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("&", operand, ctx) {
        override val simpleName = "Address"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Dereference(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("*", operand, ctx) {
        override val simpleName = "Dereference"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Plus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("+", operand, ctx) {
        override val simpleName = "Plus"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Minus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("-", operand, ctx) {
        override val simpleName = "Minus"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class BitNot(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("~", operand, ctx) {
        override val simpleName = "BitNot"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Not(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("!", operand, ctx) {
        override val simpleName = "Not"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }
}
