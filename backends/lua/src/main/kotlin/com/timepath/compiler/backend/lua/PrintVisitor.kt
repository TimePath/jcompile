package com.timepath.compiler.backend.lua

import com.timepath.Printer
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Pointer
import com.timepath.compiler.backend.q1vm.Vector

class PrintVisitor(val indent: String = "    ") : ASTVisitor<Printer> {

    fun escape(id: String) = when (id) {
        "end" -> "___end"
        "in" -> "___in"
        else -> id
    }

    var depth = 0

    fun global() = depth == 0

    fun Expression.print() = accept(this@PrintVisitor)

    fun declare(e: DeclarationExpression) = Printer("${if (global()) "" else "local "}${escape(e.id)} = nil")

    fun assign(l: Expression, r: Expression) = Printer {
        val lhs = l.print()
        val rhs = r.print()
        +"(function() $lhs = $rhs; return $lhs end)()"
    }

    fun post(e: UnaryExpression) = Printer {
        val op = "+"
        val tmp = "tmp"
        val x = e.operand.print()
        +"(function() local $tmp = $x; $x = $x $op 1; return $tmp; end)()"
    }

    fun pre(e: UnaryExpression) = Printer {
        val op = "+"
        val x = e.operand.print()
        +"(function() $x = $x $op 1; return $x; end)()"
    }

    override fun default(e: Expression) = Printer("$e")

    val binaryPrecedence = mapOf(
            "::" to 1
            , "()" to 2
            , "[]" to 2
            , "." to 2
            , "->" to 2
            , "*" to 3, "/" to 3, "%" to 3
            , "+" to 6, "-" to 6
            , "<<" to 7, ">>" to 7
            , "<" to 8, "<=" to 8, ">" to 8, ">=" to 8
            , "==" to 9, "!=" to 9
            , "&" to 10
            , "^" to 11
            , "|" to 12
            , "&&" to 13
            , "||" to 14
            , "=" to 15
            , "+=" to 15, "-=" to 15
            , "*=" to 15, "/=" to 15, "%=" to 15
            , "<<=" to 15, ">>=" to 15
            , "&=" to 15, "^=" to 15, "|=" to 15
            , "," to 17
    )

    fun Expression.printPrec(parent: String) = when {
        this is BinaryExpression && binaryPrecedence[op]!! > binaryPrecedence[parent]!! -> Printer("(${print()})")
        else -> print()
    }

    fun bintolua(op: String) = when (op) {
        "!=" -> "~="
        "&&" -> "and"
        "||" -> "or"
        "," -> ";" // FIXME
        else -> op
    }

    override fun visit(e: ReferenceExpression) = Printer(escape(e.refers.id))

    override fun visit(e: BinaryExpression) = "${e.left.printPrec(e.op)} ${bintolua(e.op)} ${e.right.printPrec(e.op)}"
            .let { Printer(it) }

    override fun visit(e: BinaryExpression.Assign) = assign(e.left, e.right)
    override fun visit(e: BinaryExpression.Add.Assign) = assign(e.left, e.left + e.right)
    override fun visit(e: BinaryExpression.BitAnd.Assign) = assign(e.left, e.left and e.right)
    override fun visit(e: BinaryExpression.BitOr.Assign) = assign(e.left, e.left or e.right)
    override fun visit(e: BinaryExpression.BitXor.Assign) = assign(e.left, e.left xor e.right)
    override fun visit(e: BinaryExpression.Divide.Assign) = assign(e.left, e.left / e.right)
    override fun visit(e: BinaryExpression.Lsh.Assign) = assign(e.left, e.left shl e.right)
    override fun visit(e: BinaryExpression.Modulo.Assign) = assign(e.left, e.left % e.right)
    override fun visit(e: BinaryExpression.Multiply.Assign) = assign(e.left, e.left * e.right)
    override fun visit(e: BinaryExpression.Rsh.Assign) = assign(e.left, e.left shr e.right)
    override fun visit(e: BinaryExpression.Subtract.Assign) = assign(e.left, e.left - e.right)

    override fun visit(e: BlockExpression) = Printer {
        +"do"
        +indent { e.children.forEach { +it.print() } }
        +"end"
    }

    override fun visit(e: ConditionalExpression) = Printer {
        if (e.expression) {
            +"${e.test.print()} and ${e.pass.print()} or ${e.fail?.print() ?: "nil"}"
        } else {
            +"if ${e.test.print()} then"
            +indent {
                +"${e.pass.print()}"
            }
            if (e.fail != null) {
                +"else"
                +indent {
                    +"${e.fail.print()}"
                }
            }
            +"end"
        }
    }

    override fun visit(e: ConstantExpression) = e.value.any.let {
        when (it) {
            is Pointer -> "${it.int}"
            is Float -> "${it}"
            is Int -> "${it}"
            is Vector -> "vector(${it.x}, ${it.y}, ${it.z})"
            is Char -> "'${it}'"
            is String -> "\"${it}\""
            else -> throw NoWhenBranchMatchedException()
        }.let { Printer(it) }
    }

    override fun visit(e: ContinueStatement) = Printer("goto continue")

    override fun visit(e: DeclarationExpression) = declare(e)
    override fun visit(e: FunctionExpression): Printer {
        fun func(): String {
            val pars = e.params?.map { it.id } ?: emptyList()
            val vara = e.vararg?.let { listOf("...") } ?: emptyList()
            return "function ${e.id}(${(pars + vara).map { escape(it) }.joinToString(", ")})"
        }
        return Printer {
            depth++
            if (e.children.isNotEmpty()) {
                +func()
                +indent {
                    e.children.forEach {
                        +it.print()
                    }
                }
                +"end"
            } else {
                +"${func()} end"
            }
            depth--
        }
    }


    override fun visit(e: IndexExpression) = Printer("${e.left.print()}[${e.right.print()}]")

    override fun visit(e: LabelExpression) = Printer("::${e.id}::")
    override fun visit(e: LoopExpression): Printer {
        fun Printer.children() = +indent { e.children.forEach { +it.print() } }
        val cond = e.predicate.print()
        fun Printer.whileloop() {
            +"while $cond do"
            children()
            +indent { +"::continue::" }
            +"end"
        }
        return Printer {
            if (!e.checkBefore) {
                +"repeat"
                children()
                +indent { +"::continue::" }
                +"until not $cond"
            } else {
                if (e.initializer == null) {
                    whileloop()
                } else {
                    +"do"
                    +indent {
                        e.initializer.forEach { +it.print() }
                        whileloop()
                    }
                    +"end"
                }
            }
        }
    }

    override fun visit(e: MemberExpression) = Printer("${e.left.print()}.${e.field.id}")
    override fun visit(e: MemberReferenceExpression) = Printer("'${e.id}'")

    override fun visit(e: MethodCallExpression) = Printer("${e.function.print()}(${
    e.args.asSequence().map {
        it.print()
    }.joinToString(", ")})")

    override fun visit(e: Nop) = Printer("-- nop")
    override fun visit(e: ReturnStatement) = Printer {
        val it = e.returnValue
        if (it != null) {
            +"return ${it.print()}"
        } else {
            +"return"
        }
    }

    override fun visit(e: SwitchExpression) = Printer("-- switch")

    fun untolua(op: String) = when (op) {
        "!" -> "not "
        "+" -> ""
        else -> op
    }

    override fun visit(e: UnaryExpression) = Printer("${untolua(e.op)}${e.operand.print()}")
    override fun visit(e: UnaryExpression.Cast) = e.operand.print()
    override fun visit(e: UnaryExpression.PostDecrement) = post(e)
    override fun visit(e: UnaryExpression.PostIncrement) = post(e)
    override fun visit(e: UnaryExpression.PreDecrement) = pre(e)
    override fun visit(e: UnaryExpression.PreIncrement) = pre(e)
}
