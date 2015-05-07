package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.Value
import com.timepath.compiler.backend.q1vm.impl.AllocatorImpl
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import java.util.Deque
import java.util.Stack

trait Allocator {

    companion object {
        fun invoke(opts: CompilerOptions) = AllocatorImpl(opts)
    }

    trait AllocationMap {
        trait Entry {
            val name: String
            val ref: Int
            val value: Value?
            val type: Type
            fun dup(name: String = name, ref: Int = ref, value: Value? = value, type: Type = type): Entry
        }

        val all: List<Entry>

        fun size(): Int

        fun contains(ref: Int): Boolean
        fun get(ref: Int): Entry?

        fun contains(value: Value): Boolean
        fun get(value: Value): Entry?

        fun contains(name: String): Boolean
        fun get(name: String): Entry?
        fun set(name: String, value: Entry)
    }

    val functions: AllocationMap
    val references: AllocationMap
    val constants: AllocationMap
    val strings: AllocationMap

    trait Scope {
        val id: Any
        val lookup: MutableMap<String, AllocationMap.Entry>
    }

    val scope: Deque<Scope>

    fun push(id: Any)
    fun pop()

    fun allocateString(s: String): AllocationMap.Entry
    fun allocateFunction(id: String, type: function_t): AllocationMap.Entry
    fun allocateReference(id: String? = null, type: Type, value: Value? = null): AllocationMap.Entry
    fun allocateConstant(value: Value, type: Type, id: String = when (value.any) {
        is Int -> "${value.any}i"
        is Float -> "${value.any}f"
        else -> "$value"
    }): AllocationMap.Entry

    fun contains(name: String): Boolean
    fun get(name: String): AllocationMap.Entry?
}
