package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.gen.ReferenceIR
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

// TODO: namespace
open class ReferenceExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {

    override fun type(gen: Generator): Type {
        gen.allocator[id]?.let { return it.type }
        throw NullPointerException("Reference $id not found")
    }

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id)

    override fun toString(): String = id

}

open class DeclarationExpression(id: String,
                                 val type: Type,
                                 val value: ConstantExpression? = null,
                                 ctx: ParserRuleContext? = null) : ReferenceExpression(id, ctx) {

    override fun type(gen: Generator) = type

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id,
                "type" to type)

}

open class ParameterExpression(id: String,
                               type: Type,
                               val index: Int,
                               ctx: ParserRuleContext? = null) : DeclarationExpression(id, type, ctx = ctx) {

}

class StructDeclarationExpression(id: String,
                                  val struct: Type.Struct,
                                  ctx: ParserRuleContext? = null) : DeclarationExpression(id, struct, null, ctx) {
}

class MemoryReference(val ref: Int, val type: Type, ctx: ParserRuleContext? = null) : Expression(ctx) {

    override fun type(gen: Generator): Type = type

    override val attributes: Map<String, Any>
        get() = mapOf("ref" to ref)

    override fun toString(): String = "$$ref"

}
