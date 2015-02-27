package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

abstract class BinaryExpression(val op: String, val left: Expression, val right: Expression, override val ctx: PRC? = null) : Expression() {

    {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    class Comma(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(",", left, right, ctx) {
        override val simpleName = "Comma"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("=", left, right, ctx) {
        override val simpleName = "Assign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Or(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("||", left, right, ctx) {
        override val simpleName = "Or"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class And(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&&", left, right, ctx) {
        override val simpleName = "And"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class BitOr(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("|", left, right, ctx) {
        override val simpleName = "BitOr"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class OrAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("|=", left, right, ctx) {
        override val simpleName = "OrAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class ExclusiveOr(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("^", left, right, ctx) {
        override val simpleName = "ExclusiveOr"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class ExclusiveOrAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("^=", left, right, ctx) {
        override val simpleName = "ExclusiveOrAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class BitAnd(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&", left, right, ctx) {
        override val simpleName = "BitAnd"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class AndAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&=", left, right, ctx) {
        override val simpleName = "AndAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Eq(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("==", left, right, ctx) {
        override val simpleName = "Eq"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Ne(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("!=", left, right, ctx) {
        override val simpleName = "Ne"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Lsh(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<<", left, right, ctx) {
        override val simpleName = "Lsh"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class LshAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<<=", left, right, ctx) {
        override val simpleName = "LshAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Rsh(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">>", left, right, ctx) {
        override val simpleName = "Rsh"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class RshAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">>=", left, right, ctx) {
        override val simpleName = "RshAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Lt(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<", left, right, ctx) {
        override val simpleName = "Lt"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Le(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<=", left, right, ctx) {
        override val simpleName = "Le"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Gt(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">", left, right, ctx) {
        override val simpleName = "Gt"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Ge(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">=", left, right, ctx) {
        override val simpleName = "Ge"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Add(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("+", left, right, ctx) {
        override val simpleName = "Add"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class AddAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("+=", left, right, ctx) {
        override val simpleName = "AddAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Subtract(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("-", left, right, ctx) {
        override val simpleName = "Subtract"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class SubtractAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("-=", left, right, ctx) {
        override val simpleName = "SubtractAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Multiply(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("*", left, right, ctx) {
        override val simpleName = "Multiply"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class MultiplyAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("*=", left, right, ctx) {
        override val simpleName = "MultiplyAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Divide(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("/", left, right, ctx) {
        override val simpleName = "Divide"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class DivideAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("/=", left, right, ctx) {
        override val simpleName = "DivideAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Modulo(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("%", left, right, ctx) {
        override val simpleName = "Modulo"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class ModuloAssign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("%=", left, right, ctx) {
        override val simpleName = "ModuloAssign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

}
