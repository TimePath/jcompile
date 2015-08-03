package com.timepath.compiler.ir

import com.timepath.compiler.Value
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import java.util.Deque

interface Allocator {

    interface AllocationMap {
        interface Entry {
            val name: String
            val ref: Instruction.Ref
            val value: Value?
            val type: Type
        }

        val all: List<Entry>

        fun size(): Int

        fun contains(ref: Instruction.Ref): Boolean
        fun get(ref: Instruction.Ref): Entry?

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

    interface Scope {
        val id: Any
        val lookup: MutableMap<String, AllocationMap.Entry>
    }

    val scope: Deque<Scope>

    fun push(id: Any)
    fun pop()

    fun allocateString(s: String): AllocationMap.Entry
    fun allocateFunction(id: String, type: function_t): AllocationMap.Entry
    fun allocateReference(id: String? = null, type: Type, value: Value? = null, scope: Instruction.Ref.Scope): AllocationMap.Entry
    fun allocateConstant(value: Value, type: Type, id: String = when (value.any) {
        is Int -> "${value.any}i"
        is Float -> "${value.any}f"
        else -> "$value"
    }): AllocationMap.Entry

    fun contains(name: String): Boolean
    fun get(name: String): AllocationMap.Entry?
}
