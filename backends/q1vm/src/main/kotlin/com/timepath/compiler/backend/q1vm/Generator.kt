package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.impl.GeneratorImpl
import com.timepath.compiler.ir.IR
import com.timepath.q1vm.ProgramData

interface Generator {
    companion object {
        fun invoke(state: Q1VM.State) = GeneratorImpl(state)
    }

    interface ASM {
        val ir: List<IR>
        fun generateProgs(): ProgramData
    }

    fun generate(roots: List<Expression>): ASM
}
