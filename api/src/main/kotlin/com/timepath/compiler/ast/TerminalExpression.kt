package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.compiler.types.struct_t

// TODO: namespace
open class ReferenceExpression(val id: String, override val ctx: ParserRuleContext? = null) : Expression() {

    override fun toString(): String = id

}

open class DeclarationExpression(id: String,
                                 val type: Type,
                                 val value: ConstantExpression? = null,
                                 override val ctx: ParserRuleContext? = null) : ReferenceExpression(id)

class ParameterExpression(id: String,
                               type: Type,
                               val index: Int,
                               override val ctx: ParserRuleContext? = null) : DeclarationExpression(id, type)

class StructDeclarationExpression(id: String,
                                  val struct: struct_t,
                                  override val ctx: ParserRuleContext? = null) : DeclarationExpression(id, struct, null)

class MemoryReference(val ref: Int, val type: Type, override val ctx: ParserRuleContext? = null) : Expression() {

    override fun toString(): String = "$$ref"

}
