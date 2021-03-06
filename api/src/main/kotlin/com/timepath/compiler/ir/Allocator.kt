package com.timepath.compiler.ir

import com.timepath.compiler.Value
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import java.util.*

interface Allocator {

    interface AllocationMap {
        interface Entry {
            val name: String
            val ref: Instruction.Ref
            val value: Value?
            val type: Type

            fun copy(name: String = this.name,
                     ref: Instruction.Ref = this.ref,
                     value: Value? = this.value,
                     type: Type = this.type): Entry
            fun _asString() = "Entry(name=$name, ref=$ref, value=$value, type=$type)"
        }

        val all: List<Entry>

        fun size(): Int

        operator fun contains(ref: Instruction.Ref): Boolean
        operator fun get(ref: Instruction.Ref): Entry?

        operator fun contains(value: Value): Boolean
        operator fun get(value: Value): Entry?

        operator fun contains(name: String): Boolean
        operator fun get(name: String): Entry?
        operator fun set(name: String, value: Entry)
    }

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
    fun allocateConstant(value: Value, type: Type, id: String? = null): AllocationMap.Entry

    operator fun contains(name: String): Boolean
    operator fun get(name: String): AllocationMap.Entry?
}
