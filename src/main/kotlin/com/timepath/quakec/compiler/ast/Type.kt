package com.timepath.quakec.compiler.ast

import com.timepath.quakec.vm.Instruction

enum class Type {
    Void
    String
    Float
    Vector
    Entity
    Field
    Function

    class object {
        val instr: Map<Class<*>, Instruction> = mapOf(
                javaClass<String>() to Instruction.STORE_STR,
                javaClass<Float>() to Instruction.STORE_FLOAT
        )
    }

}