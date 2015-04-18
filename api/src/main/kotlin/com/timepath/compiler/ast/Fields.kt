package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

/**
 * dynamic:
 * array[index], entity.(field)
 */
// TODO: arrays
public class IndexExpression(left: Expression, right: Expression, ctx: PRC? = null) : BinaryExpression("[]", left, right, ctx) {
    override val simpleName = "IndexExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    var instr: Any? = null
}

/**
 * static:
 * struct.field
 */
// TODO: structs
public class MemberExpression(left: Expression, val field: String, ctx: PRC? = null) : BinaryExpression(".", left, ConstantExpression(field), ctx) {
    override val simpleName = "MemberExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    var instr: Any? = null
}
