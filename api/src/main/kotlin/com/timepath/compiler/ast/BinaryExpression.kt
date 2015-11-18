package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

public infix fun Expression.or(other: Expression) = BinaryExpression.BitOr(this, other, null)
public infix fun Expression.xor(other: Expression) = BinaryExpression.BitXor(this, other, null)
public infix fun Expression.and(other: Expression) = BinaryExpression.BitAnd(this, other, null)
public infix fun Expression.shl(other: Expression) = BinaryExpression.Lsh(this, other, null)
public infix fun Expression.shr(other: Expression) = BinaryExpression.Rsh(this, other, null)
public operator fun Expression.plus(other: Expression) = BinaryExpression.Add(this, other, null)
public operator fun Expression.minus(other: Expression) = BinaryExpression.Subtract(this, other, null)
public operator fun Expression.times(other: Expression) = BinaryExpression.Multiply(this, other, null)
public operator fun Expression.div(other: Expression) = BinaryExpression.Divide(this, other, null)
public operator fun Expression.mod(other: Expression) = BinaryExpression.Modulo(this, other, null)
public infix fun Expression.set(other: Expression) = BinaryExpression.Assign(this, other, null)
public infix fun Expression.eq(other: Expression) = BinaryExpression.Eq(this, other, null)

public abstract class BinaryExpression(val op: String, val left: Expression, val right: Expression, override val ctx: PRC?) : Expression() {

    init {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    class Comma(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression(",", left, right, ctx) {
        override val simpleName = "Comma"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Comma(l, r, ctx)
        }
    }

    class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("=", left, right, ctx) {
        override val simpleName = "Assign"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Assign(l, r, ctx)
        }
    }

    class Or(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("||", left, right, ctx) {
        override val simpleName = "Or"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Or(l, r, ctx)
        }
    }

    class And(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("&&", left, right, ctx) {
        override val simpleName = "And"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            And(l, r, ctx)
        }
    }

    class BitOr(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("|", left, right, ctx) {
        override val simpleName = "BitOr"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            BitOr(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("|=", left, right, ctx) {
            override val simpleName = "BitOr.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class BitXor(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("^", left, right, ctx) {
        override val simpleName = "BitXor"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            BitXor(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("^=", left, right, ctx) {
            override val simpleName = "BitXor.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class BitAnd(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("&", left, right, ctx) {
        override val simpleName = "BitAnd"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            BitAnd(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("&=", left, right, ctx) {
            override val simpleName = "BitAnd.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class Eq(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("==", left, right, ctx) {
        override val simpleName = "Eq"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Eq(l, r, ctx)
        }
    }

    class Ne(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("!=", left, right, ctx) {
        override val simpleName = "Ne"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Ne(l, r, ctx)
        }
    }

    class Lsh(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("<<", left, right, ctx) {
        override val simpleName = "Lsh"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Lsh(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("<<=", left, right, ctx) {
            override val simpleName = "Lsh.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class Rsh(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression(">>", left, right, ctx) {
        override val simpleName = "Rsh"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Rsh(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression(">>=", left, right, ctx) {
            override val simpleName = "Rsh.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class Lt(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("<", left, right, ctx) {
        override val simpleName = "Lt"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Lt(l, r, ctx)
        }
    }

    class Le(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("<=", left, right, ctx) {
        override val simpleName = "Le"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Le(l, r, ctx)
        }
    }

    class Gt(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression(">", left, right, ctx) {
        override val simpleName = "Gt"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Gt(l, r, ctx)
        }
    }

    class Ge(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression(">=", left, right, ctx) {
        override val simpleName = "Ge"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Ge(l, r, ctx)
        }
    }

    class Add(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("+", left, right, ctx) {
        override val simpleName = "Add"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Add(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("+=", left, right, ctx) {
            override val simpleName = "Add.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class Subtract(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("-", left, right, ctx) {
        override val simpleName = "Subtract"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Subtract(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("-=", left, right, ctx) {
            override val simpleName = "Subtract.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class Multiply(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("*", left, right, ctx) {
        override val simpleName = "Multiply"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Multiply(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("*=", left, right, ctx) {
            override val simpleName = "Multiply.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class Divide(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("/", left, right, ctx) {
        override val simpleName = "Divide"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Divide(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("/=", left, right, ctx) {
            override val simpleName = "Divide.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

    class Modulo(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("%", left, right, ctx) {
        override val simpleName = "Modulo"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
            val (l, r) = children
            Modulo(l, r, ctx)
        }

        class Assign(left: Expression, right: Expression, ctx: PRC?) : BinaryExpression("%=", left, right, ctx) {
            override val simpleName = "Modulo.Assign"
            override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

            override fun withChildren(children: List<Expression>) = require(children.size == 2).let {
                val (l, r) = children
                Assign(l, r, ctx)
            }
        }
    }

}
