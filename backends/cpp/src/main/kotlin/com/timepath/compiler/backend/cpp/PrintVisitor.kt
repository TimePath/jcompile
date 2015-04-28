package com.timepath.compiler.backend.cpp

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.compiler.backend.q1vm.data.Vector
import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t

class PrintVisitor(val state: Q1VM.State, val indent: String = "    ") : ASTVisitor<Printer> {

    companion object {
        fun term(e: Expression) = when {
            e is BlockExpression
                , e is ConditionalExpression && !e.expression
                , e is FunctionExpression && e.children.isNotEmpty()
                , e is LabelExpression
                , e is LoopExpression
                , e is SwitchExpression
                , e is SwitchExpression.Case
            -> ""
            else -> ";"
        }
    }

    fun block(l: List<Expression>) = Printer {
        val single = l.singleOrNull()
        when (single) {
            is BlockExpression -> +single.print()
            else -> {
                +"{"
                +indent {
                    l.forEach { +"${it.print()}${term(it)}" }
                }
                +"}"
            }
        }
    }

    override fun default(e: Expression) = throw UnsupportedOperationException("${e.javaClass}")

    val String.p: Printer get() = Printer(this)

    override fun visit(e: Nop) = ";".p
    override fun visit(e: UnaryExpression) = "${e.op}${e.operand.print()}".p
    override fun visit(e: UnaryExpression.Cast) = "(${e.type.typename()}) ${e.operand.print()}".p
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
        this is BinaryExpression && binaryPrecedence[op]!! > binaryPrecedence[parent]!! -> "(${print()})".p
        else -> print()
    }

    override fun visit(e: BinaryExpression) = "${e.left.printPrec(e.op)} ${e.op} ${e.right.printPrec(e.op)}".p

    fun deref(t: Type) = when (t) {
        is class_t -> "->"
        else -> "."
    }

    override fun visit(e: MemberExpression) = "${e.left.print()}${deref(e.left.type())}${e.field.id}".p
    override fun visit(e: MemberReferenceExpression) = "${e.owner.typename()}::${e.id}".p
    override fun visit(e: IndexExpression) = when {
        e.right.type() is field_t -> "${e.left.print()}${deref(e.left.type())}*${e.right.print()}"
        else -> "${e.left.print()}[${e.right.print()}]"
    }.p

    suppress("NOTHING_TO_INLINE") inline fun Expression.type() = accept(state.typeVisitor)
    suppress("NOTHING_TO_INLINE") inline fun Expression.print() = accept(this@PrintVisitor)

    val typename = mapOf(
            void_t to "void"
            , bool_t to "bool"
            , int_t to "int"
            , float_t to "float"
            , vector_t to "vector"
            , entity_t to "entity"
            , string_t to "string"
    )

    fun Type.typename(): String = typename.getOrElse(this) { simpleName }

    fun array_t.declareArray(id: String?) = (id ?: "").let {
        type.declareVar(it, null)
    }

    fun field_t.declareField(id: String?) = (id ?: "").let {
        type.declareVar("entity_s::*$it", null)
    }

    fun function_t.declareFunc(id: String?) = (id ?: "").let { if (true) "(*$it)" else it }.let {
        type.declareVar(it, null).let {
            val sig = this.argTypes.map { it.declareVar(null, null).toString() } +
                    (this.vararg?.let { listOf("...") } ?: emptyList())
            "$it(${sig.join(", ")})"
        }
    }

    fun Type.declareVar(id: String?, v: Expression?): Printer = when (this) {
        is array_t -> declareArray(id).let {
            it.toString() + "[${v?.print() ?: ""}]"
        }
        is field_t -> declareField(id).let {
            it.toString() + (v?.let { " = (${declareField(null)}) ${it.print()}" } ?: "")
        }
        is function_t -> declareFunc(id).let {
            it.toString()
        }
        else -> typename() + (id?.let { " $it" } ?: "") + when {
            v != null -> " = " + v.print()
            else -> ""
        }
    }.p

    fun declare(e: DeclarationExpression) = e.type.let {
        when (it) {
            is array_t -> it.declareVar(e.id, it.sizeExpr)
            is function_t -> it.declareVar(e.id, null)
            else -> it.declareVar(e.id, e.value)
        }
    }

    override fun visit(e: BlockExpression) = block(e.children).toString().p

    override fun visit(e: BreakStatement) = "break".p

    fun compound(e: Expression) = when (e) {
        is BlockExpression -> e.print()
        is ConditionalExpression -> e.print()
        else -> block(listOf(e))
    }

    override fun visit(e: ConditionalExpression): Printer {
        val pred = e.test.print()
        return when {
            e.expression -> {
                val pass = e.pass.print()
                val fail = e.fail!!.print()
                "$pred ? $pass : $fail".p
            }
            else -> Printer {
                +("if ($pred) ${compound(e.pass)}" + when {
                    e.fail != null -> " else ${compound(e.fail)}"
                    else -> ""
                })
            }
        }
    }

    override fun visit(e: ConstantExpression) = e.value.any.let {
        when (it) {
            is Pointer -> "${it.int}"
            is Float -> "${it}f"
            is Int -> "${it}"
            is Vector -> "vector ( ${it.x}f, ${it.y}f, ${it.z}f )"
            is Char -> "'${it}'"
            is String -> "\"${it}\""
            else -> throw NoWhenBranchMatchedException()
        }.p
    }

    override fun visit(e: ContinueStatement) = "continue".p

    override fun visit(e: DeclarationExpression) = declare(e)
    override fun visit(e: StructDeclarationExpression) = declare(e)
    override fun visit(e: ParameterExpression) = declare(e)

    override fun visit(e: FunctionExpression) = Printer {
        val pars = e.params?.map { it.print() }
                ?: e.type.argTypes.map { it.declareVar(null, null) }
        val vara = e.vararg?.let { it.print(); "..." }?.let { listOf(it) }
                ?: e.type.vararg?.let { it.declareVar(null, null); "..." }?.let { listOf(it) }
                ?: emptyList()
        +(e.type.type.declareVar("${e.id}(${(pars + vara).joinToString(", ")})", null).toString() + when {
            e.children.isNotEmpty() -> " ${block(e.children)}"
            else -> ""
        })
    }

    override fun visit(e: GotoExpression) = "goto ${e.id}".p

    override fun visit(e: LabelExpression) = "${e.id}:".p

    override fun visit(e: LoopExpression): Printer {
        val init = e.initializer?.let { it.map { it.print() }.joinToString(", ") }
        val pred = e.predicate.print()
        val body = block(e.children)
        val update = e.update?.let { it.map { it.print() }.joinToString(", ") }
        return when {
            !e.checkBefore -> "do $body while ($pred)"
            init != null && update != null -> "for ($init; $pred; $update) $body"
            else -> "while ($pred) $body"
        }.p
    }

    override fun visit(e: MethodCallExpression) = "${e.function.print()}(${e.args.map { it.print() }.joinToString(", ")})".p

    override fun visit(e: ReferenceExpression) = "${e.refers.id}".p

    override fun visit(e: ReturnStatement) = when {
        e.returnValue == null -> "return"
        else -> "return ${e.returnValue.print()}"
    }.p

    override fun visit(e: SwitchExpression) = Printer {
        +"switch (${e.test.print()}) ${block(e.children)}"
    }

    override fun visit(e: SwitchExpression.Case) = when (e.expr) {
        null -> "default:"
        else -> "case ${e.expr.print()}:"
    }.p
}
