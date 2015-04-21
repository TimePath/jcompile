package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.impl.GeneratorImpl
import com.timepath.q1vm.ProgramData

trait Generator {
    companion object {
        fun new(state: Q1VM.State) = GeneratorImpl(state)
    }

    val gotoLabels: MutableMap<IR, String>

    trait ASM {
        val ir: List<IR>
        fun generateProgs(): ProgramData
    }

    fun generate(roots: Sequence<Expression>): ASM
}
