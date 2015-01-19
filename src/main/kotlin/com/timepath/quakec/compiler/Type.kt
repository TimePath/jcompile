package com.timepath.quakec.compiler

import com.timepath.quakec.vm.Instruction
import java.lang

trait Type {

    class object {
        fun from(any: Any?): Type = when (any) {
            is kotlin.Float -> Float
            is kotlin.Int -> Int()
            is kotlin.String -> String
            else -> Void
        }
    }

    val store: Instruction

    override fun toString(): kotlin.String {
        return javaClass.getSimpleName()
    }

    object Void : Type {
        override val store: Instruction
            get() = throw UnsupportedOperationException()
    }

    object Float : Type {
        override val store = Instruction.STORE_FLOAT
    }

    open class Int : Type {
        override val store: Instruction
            get() = throw UnsupportedOperationException()
    }

    data abstract class Struct(val fields: Map<kotlin.String, Type>) : Type

    object Vector : Struct(mapOf("x" to Float, "y" to Float, "z" to Float)) {
        override val store = Instruction.STORE_VEC
    }

    abstract class Pointer : Int()

    object String : Pointer() {
        override val store = Instruction.STORE_STR
    }

    object Entity : Pointer() {
        override val store = Instruction.STORE_ENT
    }

    data class Field(val type: Type) : Pointer() {
        override val store = Instruction.STORE_FIELD
        override fun toString(): kotlin.String {
            return "${super.toString()}($type)"
        }
    }

    data class Function(val type: Type, val argTypes: List<Type>) : Pointer() {
        override val store = Instruction.STORE_FUNC
        override fun toString(): kotlin.String {
            return "${super.toString()}($type, $argTypes)"
        }
    }
}