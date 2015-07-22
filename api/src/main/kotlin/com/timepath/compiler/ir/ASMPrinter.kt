package com.timepath.compiler.ir

import com.timepath.Printer
import java.util.LinkedList

class ASMPrinter(val ir: List<IR>) {

    override fun toString() = Printer { globals(ir.listIterator()) }.toString()

    private fun Printer.globals(iter: Iterator<IR>) {
        push()
        loop@for (it in iter) {
            when (it) {
                is IR.Function -> function(it, iter)
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
        funcs[0].push(it.ref to it.name)
        +".using ${it.name} = $${Instruction.OFS_STR(it.ref)}"
    }

    private fun Printer.function(func: IR.Function, iter: Iterator<IR>) {
        push()
        +".func ${func.e.name} = $${func.e.ref} {"
        +"    " {
            loop@for (it in iter) {
                when (it) {
                    is IR.Declare -> declare(it)
                    is IR.EndFunction -> {
                        pop()
                        break@loop
                    }
                    is IR.Return -> Unit
                    else -> statement(it)
                }
            }
        }
        +"}"
    }

    private fun Printer.statement(stmt: IR) {
        val f: (Instruction.Ref) -> String = {
            Instruction.OFS_STR(it).let {
                when (it) {
                    is String -> "@" + it
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
                        id ?: "$" + it
                    }
                    else -> throw NoWhenBranchMatchedException()
                }
            }.padEnd(12)
        }
        val s = (stmt.instr?.name(f) ?: "")
        +when (stmt.instr) {
            is Instruction.GOTO, is Instruction.LABEL -> s
            is Instruction.WithArgs -> {
                val (a, b, c) = stmt.instr.args
                "${s.padEnd(18)} | ${listOf(a, b, c).map(f).join(" ")}"
            }
            else -> "${s.padEnd(18)} | ???"
        }
    }
}
