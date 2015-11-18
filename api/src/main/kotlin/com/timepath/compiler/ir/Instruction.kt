package com.timepath.compiler.ir

import com.timepath.compiler.types.Type

interface Instruction {

    data class Ref(val i: Int, val scope: Ref.Scope) {
        enum class Scope(val sym: Char) {
            Local('%'), Global('@')
        }

        companion object {
            val Null = Ref(0, Scope.Global) // TODO: -1
        }

        override fun toString() = "${scope.sym}$i"

        operator fun plus(i: Int) = Ref(this.i + i, this.scope)
    }

    data class Args(val a: Ref = Ref.Null, val b: Ref = Ref.Null, val c: Ref = Ref.Null)

    abstract class WithArgs(val args: Args) : Instruction

    open class Factory(private val new: (Args) -> Instruction) {
        operator fun invoke(a: Ref = Ref.Null, b: Ref = Ref.Null, c: Ref = Ref.Null) = new(Args(a, b, c))
    }

    fun name(f: (Ref) -> String) = javaClass.simpleName

    class MUL_FLOAT(args: Args) : WithArgs(args) {
        companion object : Factory({ MUL_FLOAT(it) })
    }

    class MUL_VEC(args: Args) : WithArgs(args) {
        companion object : Factory({ MUL_VEC(it) })
    }

    class MUL_FLOAT_VEC(args: Args) : WithArgs(args) {
        companion object : Factory({ MUL_FLOAT_VEC(it) })
    }

    class MUL_VEC_FLOAT(args: Args) : WithArgs(args) {
        companion object : Factory({ MUL_VEC_FLOAT(it) })
    }

    class DIV_FLOAT(args: Args) : WithArgs(args) {
        companion object : Factory({ DIV_FLOAT(it) })
    }

    class ADD_FLOAT(args: Args) : WithArgs(args) {
        companion object : Factory({ ADD_FLOAT(it) })
    }

    class ADD_VEC(args: Args) : WithArgs(args) {
        companion object : Factory({ ADD_VEC(it) })
    }

    class SUB_FLOAT(args: Args) : WithArgs(args) {
        companion object : Factory({ SUB_FLOAT(it) })
    }

    class SUB_VEC(args: Args) : WithArgs(args) {
        companion object : Factory({ SUB_VEC(it) })
    }

    class EQ(val type: Class<out Type>, args: Args) : WithArgs(args) {
        companion object {
            operator fun get(type: Class<out Type>) = Factory { EQ(type, it) }
        }

        override fun name(f: (Ref) -> String) = "EQ<${type.simpleName}>"
    }

    class NE(val type: Class<out Type>, args: Args) : WithArgs(args) {
        companion object {
            operator fun get(type: Class<out Type>) = Factory { NE(type, it) }
        }

        override fun name(f: (Ref) -> String) = "NE<${type.simpleName}>"
    }

    class LE(args: Args) : WithArgs(args) {
        companion object : Factory({ LE(it) })
    }

    class GE(args: Args) : WithArgs(args) {
        companion object : Factory({ GE(it) })
    }

    class LT(args: Args) : WithArgs(args) {
        companion object : Factory({ LT(it) })
    }

    class GT(args: Args) : WithArgs(args) {
        companion object : Factory({ GT(it) })
    }

    class LOAD(val type: Class<out Type>, args: Args) : WithArgs(args) {
        companion object {
            operator fun get(type: Class<out Type>) = Factory { LOAD(type, it) }
        }

        override fun name(f: (Ref) -> String) = "LOAD<${type.simpleName}>"
    }

    class ADDRESS(args: Args) : WithArgs(args) {
        companion object : Factory({ ADDRESS(it) })
    }

    class STORE(val type: Class<out Type>, args: Args) : WithArgs(args) {
        companion object {
            operator fun get(type: Class<out Type>) = Factory { STORE(type, it) }
        }

        override fun name(f: (Ref) -> String) = "STORE<${type.simpleName}>"
    }

    class STOREP(val type: Class<out Type>, args: Args) : WithArgs(args) {
        companion object {
            operator fun get(type: Class<out Type>) = Factory { STOREP(type, it) }
        }

        override fun name(f: (Ref) -> String) = "STOREP<${type.simpleName}>"
    }

    class RETURN(args: Args) : WithArgs(args) {
        companion object : Factory({ RETURN(it) })
    }

    class NOT(val type: Class<out Type>, args: Args) : WithArgs(args) {
        companion object {
            operator fun get(type: Class<out Type>) = Factory { NOT(type, it) }
        }

        override fun name(f: (Ref) -> String) = "NOT<${type.simpleName}>"
    }

    class CALL(val params: List<Pair<Ref, Class<out Type>>>, args: Args) : WithArgs(args) {
        companion object {
            operator fun get(params: List<Pair<Ref, Class<out Type>>>) = Factory { CALL(params, it) }
        }

        override fun name(f: (Ref) -> String) = "CALL<${params.size}>(${params.joinToString(", ")})"
    }

    class STATE(args: Args) : WithArgs(args) {
        companion object : Factory({ STATE(it) })
    }

    class LABEL(val id: String) : Instruction {
        override fun name(f: (Ref) -> String) = "label $id:"
    }

    interface GOTO : Instruction {
        /** Unconditional */
        class Label(val id: String) : GOTO {
            override fun name(f: (Ref) -> String) = "goto $id"
        }

        /** Conditional */
        class If(val id: String, val condition: Ref, val expect: Boolean = true) : GOTO {
            override fun name(f: (Ref) -> String) = "if${if (expect) "" else "not"} ${f(condition)}, goto $id"
        }

        /** Temporary marker, replaced with unconditional goto */
        object Break : Instruction

        /** Temporary marker, replaced with unconditional goto */
        object Continue : Instruction
    }

    class AND(args: Args) : WithArgs(args) {
        companion object : Factory({ AND(it) })
    }

    class OR(args: Args) : WithArgs(args) {
        companion object : Factory({ OR(it) })
    }

    class BITAND(args: Args) : WithArgs(args) {
        companion object : Factory({ BITAND(it) })
    }

    class BITOR(args: Args) : WithArgs(args) {
        companion object : Factory({ BITOR(it) })
    }

    companion object {

        /**
         * RETURN = 1, n = -1
         * PARAM0 = 4, n = 0
         * PARAM1 = 7, n = 1
         * ...
         * PARAM7 = 25
         */
        fun OFS_PARAM(n: Int) = Ref(4 + n * 3, Ref.Scope.Global)

        fun OFS_STR(ref: Ref): Any = when {
            ref.scope == Ref.Scope.Local -> ref
            ref.i in 1..3 -> "@RETURN(${ref.i - 1})"
            ref.i in 4..27 -> "@PARAM(${(ref.i - 4) / 3}${((ref.i - 4) % 3).let {
                when {
                    it != 0 -> ", $it"
                    else -> ""
                }
            }})"
            else -> ref
        }
    }

}
