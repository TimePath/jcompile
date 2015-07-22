package com.timepath.compiler.ir

open data class IR(val instr: Instruction? = null,
                   /** Continuation passing */
                   open val ret: Instruction.Ref = Instruction.Ref(0),
                   val name: String) {

    override fun toString() = "$instr /* $name */"

    private abstract class Str(val repr: String) : IR(name = repr) {
        override fun toString() = repr
    }

    class Return(override val ret: Instruction.Ref)
    : Str("/* return = $${Instruction.OFS_STR(ret)} */")

    class Declare(val e: Allocator.AllocationMap.Entry)
    : Str("using ${e.name} = $${Instruction.OFS_STR(e.ref)}") {
        override val ret = e.ref
    }

    class Function(val e: Allocator.AllocationMap.Entry, val function: Any)
    : Str("${e.name}: ; $${e.ref}")

    class EndFunction(ret: Instruction.Ref)
    : IR(ret = ret, name = "endfunction")

    class Label(val id: String)
    : IR(Instruction.LABEL(id), name = "label $id")
}
