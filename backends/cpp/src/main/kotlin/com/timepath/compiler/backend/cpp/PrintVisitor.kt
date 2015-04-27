package com.timepath.compiler.backend.cpp

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.compiler.backend.q1vm.data.Vector
import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t

class PrintVisitor(val state: Q1VM.State, val indent: String = "    ") : ASTVisitor<String> {

    fun block(l: List<Expression>) = Printer {
        +"{"
        +indent { l.forEach { +(it.print() + ";") } }
        +"}"
    }

    override fun default(e: Expression) = throw NullPointerException("${e.javaClass}")

    override fun visit(e: Nop) = ";"
    override fun visit(e: UnaryExpression) = "${e.op}${e.operand.print()}"
    override fun visit(e: UnaryExpression.Cast) = "(${typename(e.type)}) ${e.operand.print()}"
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
        this is BinaryExpression && binaryPrecedence[op]!! > binaryPrecedence[parent]!! -> "(${print()})"
        else -> print()
    }

    override fun visit(e: BinaryExpression) = "${e.left.printPrec(e.op)} ${e.op} ${e.right.printPrec(e.op)}"

    override fun visit(e: MemberExpression) = "${e.left.print()}.${e.field.id}"
    override fun visit(e: MemberReferenceExpression) = "${typename(e.owner)}::${e.id}"
    override fun visit(e: IndexExpression) = when {
        e.right.type() is field_t -> "${e.left.print()}.*${e.right.print()}"
        else -> "${e.left.print()}[${e.right.print()}]"
    }

    suppress("NOTHING_TO_INLINE") inline fun Expression.type() = accept(state.typeVisitor)
    suppress("NOTHING_TO_INLINE") inline fun Expression.print() = accept(this@PrintVisitor)

    val typename: Map<Type, String> = mapOf(
            void_t to "void"
            , bool_t to "bool"
            , int_t to "int"
            , float_t to "float"
            , vector_t to "vector"
            , entity_t to "entity"
            , string_t to "string"
    )

    fun typename(t: Type): String = typename.getOrElse(t) { t.simpleName }

    fun _declare_field(t: field_t, id: String?)
            = _declare(t.type, "entity::*${id ?: ""}", null)

    fun _declare(t: Type, id: String?, v: Expression?): String = when (t) {
        is array_t -> _declare(t.type, id, null) +
                "[${v!!.print()}]"
        is field_t -> _declare_field(t, id) +
                (if (v != null) " = (${_declare_field(t, null)}) ${v.print()}" else "")
        is function_t -> "${_declare(t.type, "(*${id ?: ""})", null)}" +
                "(${t.argTypes.map { _declare(it, null, null) }.join(", ")}" +
                (if (t.argTypes.isNotEmpty() && t.vararg != null) ", " else "") +
                (t.vararg ?: "") +
                ")"
        else -> typename(t) +
                (if (id != null) " ${id}" else "") +
                (if (v != null) " = ${v.print()}"
                else "")
    }

    fun declare(e: DeclarationExpression): String {
        val t = e.type
        return when (t) {
            is array_t -> _declare(t, e.id, t.sizeExpr)
            is function_t -> _declare(t, e.id, null)
            else -> _declare(t, e.id, e.value)
        }
    }

    override fun visit(e: BlockExpression) = block(e.children).toString()

    override fun visit(e: BreakStatement) = "break"

    fun compound(e: Expression, s: Boolean) = when (e) {
        is BlockExpression -> e.print()
        is ConditionalExpression -> (if (s) "\n" else "") + e.print()
        else -> Printer {
            +"{"
            +indent { +(e.print() + ";") }
            +"}"
        }.toString()
    }

    override fun visit(e: ConditionalExpression): String {
        val pred = e.test.print()
        return when {
            e.expression -> {
                val pass = e.pass.print()
                check(e.fail != null, "f is null")
                val fail = e.fail!!.print()
                "$pred ? $pass : $fail"
            }
            else -> "if ($pred) ${compound(e.pass, true)}" + run {
                if (e.fail != null) " else ${compound(e.fail, false)}" else ""
            }
        }
    }

    override fun visit(e: ConstantExpression) = e.value.any.let {
        when (it) {
            is Pointer -> "${it.int}"
            is Float -> "${it}f"
            is Int -> "${it}"
            is Vector -> "(vector) { ${it.x}f, ${it.y}f, ${it.z}f }"
            is Char -> "'${it}'"
            is String -> "\"${it}\""
            else -> throw NoWhenBranchMatchedException()
        }
    }

    override fun visit(e: ContinueStatement) = "continue"

    override fun visit(e: DeclarationExpression) = declare(e)
    override fun visit(e: StructDeclarationExpression) = declare(e)
    override fun visit(e: ParameterExpression) = declare(e)

    override fun visit(e: FunctionExpression) = Printer {
        val pars = e.params?.map { it.print() }
                ?: e.type.argTypes.map { _declare(it, null, null) }
        val vara = e.vararg?.let { it.print().let { listOf(it) } }
                ?: e.type.vararg?.let { _declare(it, null, null).let { listOf(it) } }
                ?: emptyList()
        +_declare(e.type.type, "${e.id}(${(pars + vara).join(", ")})", null)
        if (e.children.isNotEmpty()) {
            +block(e.children)
        }
    }.toString()

    override fun visit(e: GotoExpression) = "goto ${e.id}"

    override fun visit(e: LabelExpression) = "${e.id}:"

    override fun visit(e: LoopExpression): String {
        val init = e.initializer?.let { it.map { it.print() }.join(", ") }
        val pred = e.predicate.print()
        val body = block(e.children)
        val update = e.update?.let { it.map { it.print() }.join(", ") }
        return when {
            !e.checkBefore -> "do $body while ($pred)"
            e.initializer != null && e.update != null -> "for ($init; $pred; $update) $body"
            else -> "while ($pred) $body"
        }
    }

    override fun visit(e: MethodCallExpression) = "${e.function.print()}(${e.args.map { it.print() }.join(", ")})"

    override fun visit(e: ReferenceExpression) = "${e.refers.id}"

    override fun visit(e: ReturnStatement) = when {
        e.returnValue == null -> "return"
        else -> "return ${e.returnValue.print()}"
    }

    override fun visit(e: SwitchExpression) = Printer {
        +"switch (${e.test.print()})"
        +block(e.children)
    }.toString()

    override fun visit(e: SwitchExpression.Case) = when (e.expr) {
        null -> "default:"
        else -> "case ${e.expr.print()}:"
    }
}
