package com.timepath.compiler.types.defaults

import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.types.Type

public abstract class struct_t(vararg fields: Pair<String, Type>) : Type() {
    val fields: MutableMap<String, Type> = linkedMapOf(*fields)
    override fun declare(name: String, value: ConstantExpression?): DeclarationExpression {
        require(value == null) { "Constexpr structs not supported" }
        return super.declare(name, value)
    }

    open fun sizeOf(): Int = fields.values.sumBy { it.sizeOf() }
    fun offsetOf(id: String): Int {
        return fields.entries
                .takeWhile { it.key != id }
                .sumBy { it.value.sizeOf() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as struct_t

        if (simpleName != other.simpleName) return false

        return true
    }

    override fun hashCode(): Int {
        return simpleName.hashCode()
    }

}

fun Type.sizeOf() = when {
    this is struct_t -> this.sizeOf()
    else -> 1
}
