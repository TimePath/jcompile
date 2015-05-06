package com.timepath.compiler.ast

import com.timepath.compiler.types.defaults.struct_t
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

public fun Expression.get(field: MemberReferenceExpression): MemberExpression = MemberExpression(this, field)

/**
 * static:
 * struct.field
 */
// TODO: structs
public class MemberExpression(left: Expression, val field: MemberReferenceExpression, ctx: PRC? = null) : BinaryExpression(".", left, ConstantExpression(field), ctx) {
    override val simpleName = "MemberExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    var instr: Any? = null
}

public fun struct_t.get(id: String): MemberReferenceExpression = MemberReferenceExpression(this, id)

/**
 * Pointer to member
 */
public open class MemberReferenceExpression(val owner: struct_t, val id: String, override val ctx: PRC? = null) : Expression() {
    override val simpleName = "MemberReferenceExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = id
}
