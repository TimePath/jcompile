package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.ReferenceIR

class ConstantExpression(any: Any) : Expression() {

    val value = Value(any)

    override val attributes: Map<String, Any?>
        get() = mapOf("value" to value)

    override fun evaluate(): Value = value

    override fun toString(): String = value.toString()

    override fun generate(ctx: Generator): List<IR> {
        val constant = ctx.allocator.allocateConstant(value)
        return listOf(ReferenceIR(constant.ref))
    }
}

open class ReferenceExpression(val id: String) : Expression() {

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

class EntityFieldReference(id: String) : ReferenceExpression(id) {
    override fun generate(ctx: Generator): List<IR> {
        return listOf(ReferenceIR(0)) // TODO: field by name
    }
}

class DeclarationExpression(id: String,
                            val value: ConstantExpression? = null) : ReferenceExpression(id) {
    override fun generate(ctx: Generator): List<IR> {
        val global = ctx.allocator.allocateReference(id, this.value?.evaluate())
        return listOf(ReferenceIR(global.ref))
    }
}

class MemoryReference(val ref: Int) : Expression() {

    override val attributes: Map<String, Any>
        get() = mapOf("ref" to ref)

    override fun toString(): String = "$$ref"

    override fun generate(ctx: Generator): List<IR> {
        return listOf(ReferenceIR(ref))
    }
}