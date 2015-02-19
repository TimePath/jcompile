package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.gen.ReferenceIR
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

// TODO: namespace
open class ReferenceExpression(val id: String, override val ctx: ParserRuleContext? = null) : Expression() {

    override fun type(gen: Generator): Type {
        gen.allocator[id]?.let { return it.type }
        throw NullPointerException("Reference $id not found")
    }

    override fun toString(): String = id

}

open class DeclarationExpression(id: String,
                                 val type: Type,
                                 val value: ConstantExpression? = null,
                                 override val ctx: ParserRuleContext? = null) : ReferenceExpression(id) {

    override fun type(gen: Generator) = type

}

open class ParameterExpression(id: String,
                               type: Type,
                               val index: Int,
                               override val ctx: ParserRuleContext? = null) : DeclarationExpression(id, type) {

}

class StructDeclarationExpression(id: String,
                                  val struct: Type.Struct,
                                  override val ctx: ParserRuleContext? = null) : DeclarationExpression(id, struct, null) {
}

class MemoryReference(val ref: Int, val type: Type, override val ctx: ParserRuleContext? = null) : Expression() {

    override fun type(gen: Generator): Type = type

    override fun toString(): String = "$$ref"

}
