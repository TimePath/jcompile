package com.timepath.compiler.ast

import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Type
import org.antlr.v4.runtime.ParserRuleContext as PRC

public fun DeclarationExpression.ref(): ReferenceExpression = ReferenceExpression(this, null)

public open class ReferenceExpression(val refers: DeclarationExpression, override val ctx: PRC?) : Expression() {
    override val simpleName = "ReferenceExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = refers.id

}

@deprecated("")
public open class DynamicReferenceExpression(val id: String, override val ctx: PRC? = null) : Expression() {
    override val simpleName = "DynamicReferenceExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = id

}

public open class DeclarationExpression(val id: String,
                                        open val type: Type,
                                        val value: ConstantExpression? = null,
                                        override val ctx: PRC? = null) : Expression() {
    override val simpleName = "DeclarationExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = id

}

public class AliasExpression(id: String, val alias: DeclarationExpression) : DeclarationExpression(id, alias.type) {
    override val simpleName = "AliasExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

}

public class ParameterExpression(id: String,
                                 type: Type,
                                 val index: Int,
                                 override val ctx: PRC? = null) : DeclarationExpression(id, type) {
    override val simpleName = "ParameterExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

}

public class MemoryReference(val ref: Instruction.Ref, val type: Type, override val ctx: PRC? = null) : Expression() {
    override val simpleName = "MemoryReference"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = "$$ref"

}
