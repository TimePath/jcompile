package com.timepath.compiler

import com.timepath.compiler.ast.*

object PrintVisitor : ASTVisitor<Map<String, Any?>> {

    public fun render(e: Expression): String = render(e, StringBuilder()).toString()

    private fun render(e: Expression, sb: StringBuilder, indent: String = ""): StringBuilder {
        val name = e.javaClass.getSimpleName()
        sb.append("${indent}<${name}")
        for ((k, v) in e.accept(PrintVisitor)) {
            sb.append(" ${k}=\"${v.toString()
                    .replace("&", "&amp;")
                    .replace("\"", "&quot;")}\"")
        }
        if (e.children.isEmpty()) {
            sb.append("/>\n")
        } else {
            sb.append(">\n")
            val nextIndent = indent + "\t"
            for (c in e.children) {
                render(c, sb, nextIndent)
            }
            sb.append("${indent}</${name}>\n")
        }
        return sb
    }

    override fun default(e: Expression) = emptyMap<String, Any?>()

    override fun visit(e: ConstantExpression) = mapOf(
            "value" to e.value
    )

    override fun visit(e: FunctionExpression): Map<String, Any?> = mapOf(
            "id" to e.id,
            "type" to e.signature
    )

    override fun visit(e: GotoExpression): Map<String, Any?> = mapOf(
            "label" to e.id
    )

    override fun visit(e: LabelExpression): Map<String, Any?> = mapOf(
            "id" to e.id
    )

    override fun visit(e: MemoryReference): Map<String, Any?> = mapOf(
            "ref" to e.ref
    )

    override fun visit(e: MethodCallExpression): Map<String, Any?> = mapOf(
            "id" to e.function
    )

    override fun visit(e: SwitchExpression.Case): Map<String, Any?> = mapOf(
            "id" to e.expr
    )

    override fun visit(e: ReferenceExpression): Map<String, Any?> = mapOf(
            "id" to e.id
    )

    override fun visit(e: DeclarationExpression): Map<String, Any?> = mapOf(
            "id" to e.id,
            "type" to e.type
    )

    override fun visit(e: ParameterExpression): Map<String, Any?> = visit(e : DeclarationExpression)
    override fun visit(e: StructDeclarationExpression): Map<String, Any?> = visit(e : DeclarationExpression)
}
