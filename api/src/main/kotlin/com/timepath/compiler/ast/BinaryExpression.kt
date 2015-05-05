package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

public abstract class BinaryExpression(val op: String, val left: Expression, val right: Expression, override val ctx: PRC? = null) : Expression() {

    init {
        add(left)
        add(right)
    }

    override fun toString(): String = "($left $op $right)"

    class Comma(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(",", left, right, ctx) {
        override val simpleName = "Comma"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("=", left, right, ctx) {
        override val simpleName = "Assign"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Or(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("||", left, right, ctx) {
        override val simpleName = "Or"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class And(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&&", left, right, ctx) {
        override val simpleName = "And"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class BitOr(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("|", left, right, ctx) {
        override val simpleName = "BitOr"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("|=", left, right, ctx) {
            override val simpleName = "BitOr.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class BitXor(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("^", left, right, ctx) {
        override val simpleName = "BitXor"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("^=", left, right, ctx) {
            override val simpleName = "BitXor.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class BitAnd(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&", left, right, ctx) {
        override val simpleName = "BitAnd"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("&=", left, right, ctx) {
            override val simpleName = "BitAnd.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class Eq(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("==", left, right, ctx) {
        override val simpleName = "Eq"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Ne(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("!=", left, right, ctx) {
        override val simpleName = "Ne"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Lsh(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<<", left, right, ctx) {
        override val simpleName = "Lsh"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<<=", left, right, ctx) {
            override val simpleName = "Lsh.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class Rsh(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">>", left, right, ctx) {
        override val simpleName = "Rsh"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">>=", left, right, ctx) {
            override val simpleName = "Rsh.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class Lt(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<", left, right, ctx) {
        override val simpleName = "Lt"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Le(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("<=", left, right, ctx) {
        override val simpleName = "Le"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Gt(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">", left, right, ctx) {
        override val simpleName = "Gt"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Ge(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression(">=", left, right, ctx) {
        override val simpleName = "Ge"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Add(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("+", left, right, ctx) {
        override val simpleName = "Add"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("+=", left, right, ctx) {
            override val simpleName = "Add.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class Subtract(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("-", left, right, ctx) {
        override val simpleName = "Subtract"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("-=", left, right, ctx) {
            override val simpleName = "Subtract.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class Multiply(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("*", left, right, ctx) {
        override val simpleName = "Multiply"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("*=", left, right, ctx) {
            override val simpleName = "Multiply.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class Divide(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("/", left, right, ctx) {
        override val simpleName = "Divide"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("/=", left, right, ctx) {
            override val simpleName = "Divide.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

    class Modulo(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("%", left, right, ctx) {
        override val simpleName = "Modulo"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        class Assign(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("%=", left, right, ctx) {
            override val simpleName = "Modulo.Assign"
            override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
        }
    }

}
