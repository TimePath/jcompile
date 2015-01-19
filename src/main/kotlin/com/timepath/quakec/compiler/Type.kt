package com.timepath.quakec.compiler

import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.compiler.ast.ConstantExpression
import com.timepath.quakec.compiler.ast.DeclarationExpression
import com.timepath.quakec.compiler.ast.StructDeclarationExpression

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

    open fun declare(name: kotlin.String, value: ConstantExpression? = null): List<DeclarationExpression> {
        throw UnsupportedOperationException()
    }

    override fun toString(): kotlin.String {
        return javaClass.getSimpleName()
    }

    object Void : Type {
        override val store: Instruction
            get() = throw UnsupportedOperationException()
    }

    object Float : Type {
        override val store = Instruction.STORE_FLOAT

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    open class Int : Type {
        override val store: Instruction
            get() = throw UnsupportedOperationException()
    }

    data abstract class Struct(val fields: Map<kotlin.String, Type>) : Type {
        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(StructDeclarationExpression(name, this))
        }
    }

    object Vector : Struct(mapOf("x" to Float, "y" to Float, "z" to Float)) {
        override val store = Instruction.STORE_VEC
    }

    abstract class Pointer : Int()

    object String : Pointer() {
        override val store = Instruction.STORE_STR

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    object Entity : Pointer() {
        override val store = Instruction.STORE_ENT

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
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

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }
}