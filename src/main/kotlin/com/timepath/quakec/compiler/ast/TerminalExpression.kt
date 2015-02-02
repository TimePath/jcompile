package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.Type
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.ReferenceIR
import com.timepath.quakec.vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.compiler.gen.Allocator

// TODO: namespace
open class ReferenceExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {

    override fun type(gen: Generator): Type {
        return gen.allocator[id]?.type!!
    }

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id)

    override fun toString(): String = id

    override fun generate(gen: Generator): List<IR> {
        if (id !in gen.allocator) {
            Generator.logger.severe("unknown reference $id")
        }
        // FIXME: null references
        val global = gen.allocator[id]
        return listOf(ReferenceIR(global?.ref ?: 0))
    }
}

open class DeclarationExpression(id: String,
                                 val type: Type,
                                 val value: ConstantExpression? = null,
                                 ctx: ParserRuleContext? = null) : ReferenceExpression(id, ctx) {

    override fun type(gen: Generator) = type

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id,
                "type" to type)

    override fun generate(gen: Generator): List<IR> {
        if (id in gen.allocator.scope.peek().lookup) {
            Generator.logger.warning("redeclaring $id")
        }
        val global = gen.allocator.allocateReference(id, type(gen), value?.evaluate())
        return listOf(ReferenceIR(global.ref))
    }
}

open class ParameterExpression(id: String,
                               type: Type,
                               val index: Int,
                               ctx: ParserRuleContext? = null) : DeclarationExpression(id, type, ctx = ctx) {

    override fun generate(gen: Generator): List<IR> {
        val memoryReference = MemoryReference(Instruction.OFS_PARAM(index), type)
        return with(linkedListOf<IR>()) {
            addAll(super.generate(gen))
            addAll(BinaryExpression.Assign(ReferenceExpression(id), memoryReference).doGenerate(gen))
            this
        }
    }
}

class StructDeclarationExpression(id: String,
                                  val struct: Type.Struct,
                                  ctx: ParserRuleContext? = null) : DeclarationExpression(id, struct, null, ctx) {
    override fun generate(gen: Generator): List<IR> {
        val fields: List<IR> = struct.fields.flatMap {
            it.value.declare("${id}_${it.key}", null).flatMap {
                it.doGenerate(gen)
            }
        }
        val allocator = gen.allocator
        allocator.scope.peek().lookup[id] = allocator.references[fields.first().ret]!!
        return fields
    }
}

class MemoryReference(val ref: Int, val type: Type, ctx: ParserRuleContext? = null) : Expression(ctx) {

    override fun type(gen: Generator): Type = type

    override val attributes: Map<String, Any>
        get() = mapOf("ref" to ref)

    override fun toString(): String = "$$ref"

    override fun generate(gen: Generator): List<IR> {
        return listOf(ReferenceIR(ref))
    }
}
