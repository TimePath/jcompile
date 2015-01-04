package com.timepath.quakec.vm.defs

import com.timepath.quakec.vm.Instruction

fun Statement(op: Short, a: Short, b: Short, c: Short): Statement = Statement(Instruction.from(op.toInt()), a.toInt(), b.toInt(), c.toInt())
data class Statement(val op: Instruction, val a: Int, val b: Int, val c: Int) {

    var data: ProgramData? = null

    fun invoke(data: ProgramData): Int {
        return op(this, data)
    }

    override fun toString(): String {
        return op.toString(this, data)
    }

}