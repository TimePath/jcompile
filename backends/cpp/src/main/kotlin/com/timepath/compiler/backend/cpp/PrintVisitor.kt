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
    override fun visit(e: UnaryExpression) = "(${e.op}${e.operand.print()})"
    override fun visit(e: UnaryExpression.Cast) = "((${typename(e.type)}) ${e.operand.print()})"
    override fun visit(e: BinaryExpression) = "(${e.left.print()} ${e.op} ${e.right.print()})"
    override fun visit(e: MemberExpression) = "(${e.left.print()}.${e.field.id})"
    override fun visit(e: MemberReferenceExpression) = "(${typename(e.owner)}::${e.id})"
    override fun visit(e: IndexExpression) = when {
        e.right.type() is field_t -> "(${e.left.print()}.*${e.right.print()})"
        else -> "(${e.left.print()}[${e.right.print()}])"
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

    fun declare(e: DeclarationExpression) = when (e.type) {
        is array_t -> _declare(e.type, e.id, e.type.sizeExpr)
        is function_t -> _declare(e.type, e.id, null)
        else -> _declare(e.type, e.id, e.value)
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

    override fun visit(e: ConditionalExpression) = when {
        e.expression -> "${e.test.print()} ? ${e.pass.print()} : ${e.fail!!.print()}"
        else -> "if (${e.test.print()}) ${compound(e.pass, true)}" +
                (if (e.fail != null) " else " + compound(e.fail, false) else "")
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
        +_declare((e.type as function_t).type, e.id + "(" +
                (if (e.params != null) e.params.map { it.print() }.join(", ")
                else e.type.argTypes.map { _declare(it, null, null) }.join(", ")) +
                (if ((e.params != null || e.type.argTypes.isNotEmpty())
                        && (e.vararg != null || e.type.vararg != null)) ", "
                else "") +
                (if (e.vararg != null) e.vararg.print()
                else if (e.type.vararg != null) _declare(e.type.vararg, null, null)
                else "") +
                ")"
                , null)
        if (e.children.isNotEmpty()) {
            +block(e.children)
        }
    }.toString()

    override fun visit(e: GotoExpression) = "goto ${e.id}"

    override fun visit(e: LabelExpression) = "${e.id}:"

    override fun visit(e: LoopExpression) = when {
        !e.checkBefore -> "do ${block(e.children)} while (${e.predicate.print()})"
        e.initializer != null && e.update != null ->
            "for (" +
                    e.initializer.map { it.print() }.join(", ") + "; " +
                    e.predicate.print() + "; " +
                    e.update.map { it.print() }.join(", ") +
                    ") " +
                    block(e.children)
        else -> "while (${e.predicate.print()}) ${block(e.children)}"
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
