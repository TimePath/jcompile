package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.struct_t
import org.antlr.v4.runtime.ParserRuleContext

open class ReferenceExpression(val refers: DeclarationExpression, override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "ReferenceExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = refers.id

}

open class DynamicReferenceExpression(val id: String, override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "DynamicReferenceExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = id

}

open class DeclarationExpression(val id: String,
                                 val type: Type,
                                 val value: ConstantExpression? = null,
                                 override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "DeclarationExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = id

}

class ParameterExpression(id: String,
                          type: Type,
                          val index: Int,
                          override val ctx: ParserRuleContext? = null) : DeclarationExpression(id, type) {
    override val simpleName = "ParameterExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

}

class StructDeclarationExpression(id: String,
                                  val struct: struct_t,
                                  override val ctx: ParserRuleContext? = null) : DeclarationExpression(id, struct, null) {
    override val simpleName = "StructDeclarationExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

}

class MemoryReference(val ref: Int, val type: Type, override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "MemoryReference"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = "$$ref"

}
