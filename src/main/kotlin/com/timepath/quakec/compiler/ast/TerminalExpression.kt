package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.Type
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.ReferenceIR
import org.antlr.v4.runtime.ParserRuleContext

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

open class DeclarationExpression(id: String,
                                 val type: Type,
                                 val value: ConstantExpression? = null,
                                 ctx: ParserRuleContext? = null) : ReferenceExpression(id, ctx) {
    override val attributes: Map<String, Any>
        get() = mapOf("id" to id,
                "type" to type)

    override fun generate(ctx: Generator): List<IR> {
        val global = ctx.allocator.allocateReference(id, value?.evaluate())
        return listOf(ReferenceIR(global.ref))
    }
}

class StructDeclarationExpression(id: String,
                                  val struct: Type.Struct,
                                  ctx: ParserRuleContext? = null) : DeclarationExpression(id, struct, null, ctx) {
    override fun generate(ctx: Generator): List<IR> {
        val fields: List<IR> = struct.fields.flatMap {
            it.value.declare("${id}_${it.key}", null).flatMap {
                it.doGenerate(ctx)
            }
        }
        val allocator = ctx.allocator
        allocator.scope.peek().lookup[id] = allocator.references[fields.first().ret]!!
        return fields
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