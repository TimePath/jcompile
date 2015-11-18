package com.timepath.compiler.ir

import com.timepath.Printer
import java.util.*

class ASMPrinter(val ir: List<IR>) {

    override fun toString() = Printer { globals() }.toString()

    private fun Printer.globals() {
        push()
        for (it in ir) {
            when (it) {
                is IR.Function -> function(it)
                is IR.Declare -> declare(it)
                else -> throw NoWhenBranchMatchedException()
            }
        }
        pop()
    }

    val funcs = LinkedList<LinkedList<Pair<Instruction.Ref, String>>>()

    fun push() = funcs.push(linkedListOf())
    fun pop() = funcs.pop()

    private fun Printer.declare(decl: IR.Declare) {
        val it = decl.e
        funcs.first().push(it.ref to it.name)
        +".using ${it.name} = ${Instruction.OFS_STR(it.ref)}"
    }

    private fun Printer.function(func: IR.Function) {
        push()
        +".func ${func.e.name} = ${func.e.ref} {"
        +"    " {
            for (it in func.children) {
                when (it) {
                    is IR.Declare -> declare(it)
                    is IR.Return -> Unit
                    else -> statement(it)
                }
            }
        }
        +"}"
        pop()
    }

    private fun Printer.statement(stmt: IR) {
        val maxInstrLen = 19
        val maxVarLen = 12
        val f: (Instruction.Ref) -> String = {

            Instruction.OFS_STR(it).let {
                when (it) {
                    is String -> it.toString()
                    is Instruction.Ref -> {
                        var id: String? = null
                        loop@for (scope in funcs) {
                            for (pair in scope) {
                                if (pair.first == it) {
                                    id = pair.second
                                    break@loop
                                }
                            }
                        }
                        id ?: it.toString()
                    }
                    else -> throw NoWhenBranchMatchedException()
                }
            }
        }
        val s = (stmt.instr?.name(f) ?: "")
        +(when (stmt.instr) {
            is Instruction.GOTO, is Instruction.LABEL -> s
            is Instruction.WithArgs -> {
                val (a, b, c) = stmt.instr.args
                "${s.padEnd(maxInstrLen)} | ${listOf(a, b, c).map { f(it).padEnd(maxVarLen) }.joinToString(" ")}"
            }
            else -> "${s.padEnd(maxInstrLen)} | ???"
        }).trimEnd()
    }
}
