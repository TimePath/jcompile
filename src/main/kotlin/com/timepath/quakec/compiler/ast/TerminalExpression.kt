package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.ReferenceIR
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.QCParser

open class ReferenceExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id)

    override fun toString(): String = id

    override fun generate(ctx: Generator): List<IR> {
        if (id !in ctx.allocator) {
            Generator.logger.severe("unknown reference $id")
        }
        // FIXME: null references
        val global = ctx.allocator[id]
        return listOf(ReferenceIR(global?.ref ?: 0))
    }
}

class EntityFieldReference(id: String, ctx: ParserRuleContext? = null) : ReferenceExpression(id, ctx) {
    override fun generate(ctx: Generator): List<IR> {
        return listOf(ReferenceIR(0)) // TODO: field by name
    }
}

class DeclarationExpression(id: String,
                            val value: ConstantExpression? = null,
                            ctx: ParserRuleContext? = null) : ReferenceExpression(id, ctx) {
    override fun generate(ctx: Generator): List<IR> {
        val global = ctx.allocator.allocateReference(id, this.value?.evaluate())
        return listOf(ReferenceIR(global.ref))
    }
}

class MemoryReference(val ref: Int, ctx: ParserRuleContext? = null) : Expression(ctx) {

    override val attributes: Map<String, Any>
        get() = mapOf("ref" to ref)

    override fun toString(): String = "$$ref"

    override fun generate(ctx: Generator): List<IR> {
        return listOf(ReferenceIR(ref))
    }
}