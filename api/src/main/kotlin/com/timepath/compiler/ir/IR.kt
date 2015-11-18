package com.timepath.compiler.ir

sealed class IR(val instr: Instruction? = null,
                /** Continuation passing */
                open val ret: Instruction.Ref = Instruction.Ref.Null,
                val name: String) {

    abstract fun copy(instr: Instruction? = this.instr, ret: Instruction.Ref = this.ret, name: String = this.name): IR

    class Basic(instr: Instruction? = null,
                ret: Instruction.Ref = Instruction.Ref.Null,
                name: String) : IR(instr, ret, name) {
        override fun copy(instr: Instruction?, ret: Instruction.Ref, name: String): IR {
            return Basic(instr, ret, name)
        }
    }

    override fun toString() = "$instr /* $name */"

    abstract class Str(val repr: String) : IR(name = repr) {
        override fun toString() = repr
    }

    class Return(override val ret: Instruction.Ref)
    : Str("/* return = $${Instruction.OFS_STR(ret)} */") {
        override fun copy(instr: Instruction?, ret: Instruction.Ref, name: String): IR {
            throw UnsupportedOperationException()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Return

            if (ret != other.ret) return false

            return true
        }

        override fun hashCode(): Int {
            return ret.hashCode()
        }
    }

    class Declare(val e: Allocator.AllocationMap.Entry)
    : Str("using ${e.name} = $${Instruction.OFS_STR(e.ref)}") {
        override fun copy(instr: Instruction?, ret: Instruction.Ref, name: String): IR {
            throw UnsupportedOperationException()
        }

        override val ret = e.ref
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Declare

            if (e != other.e) return false
            if (ret != other.ret) return false

            return true
        }

        override fun hashCode(): Int {
            var result = e.hashCode()
            result += 31 * result + ret.hashCode()
            return result
        }

    }

    class Function(val e: Allocator.AllocationMap.Entry, val function: Any, val children: List<IR>)
    : Str("${e.name}: ; $${e.ref}") {
        override fun copy(instr: Instruction?, ret: Instruction.Ref, name: String): IR {
            throw UnsupportedOperationException()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Function

            if (e != other.e) return false
            if (function != other.function) return false
            if (children != other.children) return false

            return true
        }

        override fun hashCode(): Int {
            var result = e.hashCode()
            result += 31 * result + function.hashCode()
            result += 31 * result + children.hashCode()
            return result
        }
    }

    class Label(val id: String)
    : IR(Instruction.LABEL(id), name = "label $id") {
        override fun copy(instr: Instruction?, ret: Instruction.Ref, name: String): IR {
            throw UnsupportedOperationException()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Label

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }
}
