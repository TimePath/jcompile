package com.timepath.compiler.backend.q1vm

import com.timepath.q1vm.ProgramData

open data class IR(val instr: Instruction? = null,
                   val args: Triple<Int, Int, Int> = Triple(0, 0, 0),
                   /** Continuation passing */
                   open val ret: Int = 0,
                   val name: String) {

    override fun toString() = "$instr(${args}) /* $name */"

    private abstract class Str(val repr: String) : IR(name = repr) {
        override fun toString() = repr
    }

    class Return(override val ret: Int)
    : Str("/* return = $${Instruction.OFS_STR(ret)} */")

    class Declare(val e: Allocator.AllocationMap.Entry)
    : Str("using ${e.name} = $${Instruction.OFS_STR(e.ref)}") {
        override val ret = e.ref
    }

    class Function(val e: Allocator.AllocationMap.Entry, val function: ProgramData.Function)
    : Str("${e.name}: ; $${e.ref}")

    class EndFunction(ret: Int)
    : IR(ret = ret, name = "endfunction")

    class Label(val id: String)
    : IR(Instruction.LABEL(id), name = "label $id")
}
