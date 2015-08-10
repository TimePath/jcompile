package com.timepath.compiler.frontend.quakec

import com.timepath.compiler.ast.*

public object PrintVisitor : ASTVisitor<Pair<Map<String, Any?>, List<Expression>>> {

    public fun render(e: Expression): String = render(e, StringBuilder()).toString()

    private fun render(e: Expression, sb: StringBuilder, indent: String = ""): StringBuilder {
        val name = e.simpleName
        sb.append("${indent}<${name}")
        val (attributes, children) = e.accept(PrintVisitor)
        for ((k, v) in attributes) {
            sb.append(" ${k}=\"${v.toString()
                    .replace("&", "&amp;")
                    .replace("\"", "&quot;")}\"")
        }
        if (children.isEmpty()) {
            sb.append("/>\n")
        } else {
            sb.append(">\n")
            val nextIndent = indent + "    "
            for (c in children) {
                render(c, sb, nextIndent)
            }
            sb.append("${indent}</${name}>\n")
        }
        return sb
    }

    override fun default(e: Expression) = emptyMap<String, Any?>() to e.children

    override fun visit(e: ConstantExpression) = mapOf(
            "value" to e.value
    ) to e.children

    override fun visit(e: FunctionExpression) = mapOf(
            "id" to e.id,
            "type" to e.type
    ) to e.children

    override fun visit(e: GotoExpression) = mapOf(
            "label" to e.id
    ) to e.children

    override fun visit(e: LabelExpression) = mapOf(
            "id" to e.id
    ) to e.children

    override fun visit(e: MemberReferenceExpression) = mapOf(
            "id" to e.id
    ) to e.children

    override fun visit(e: MemoryReference) = mapOf(
            "ref" to e.ref
    ) to e.children

    override fun visit(e: MethodCallExpression) = mapOf(
            "id" to e.function
    ) to e.children

    override fun visit(e: SwitchExpression.Case) = mapOf(
            "id" to e.expr
    ) to e.children

    override fun visit(e: ReferenceExpression) = mapOf(
            "id" to e.refers.id
    ) to e.children

    override fun visit(e: DeclarationExpression) = mapOf(
            "id" to e.id,
            "type" to e.type
    ) to (e.value?.let { listOf(it) } ?: emptyList())
}
