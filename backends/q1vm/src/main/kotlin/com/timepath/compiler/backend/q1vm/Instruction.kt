package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.types.Type

interface Instruction {

    abstract class WithArgs(val args: Triple<Int, Int, Int>) : Instruction

    open class Factory(private val new: (Triple<Int, Int, Int>) -> Instruction) {
        fun invoke(a: Int, b: Int, c: Int) = new(Triple(a, b, c))
    }

    fun name(f: (Int) -> String) = javaClass.getSimpleName()

    class MUL_FLOAT(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ MUL_FLOAT(it) })
    }

    class MUL_VEC(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ MUL_VEC(it) })
    }

    class MUL_FLOAT_VEC(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ MUL_FLOAT_VEC(it) })
    }

    class MUL_VEC_FLOAT(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ MUL_VEC_FLOAT(it) })
    }

    class DIV_FLOAT(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ DIV_FLOAT(it) })
    }

    class ADD_FLOAT(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ ADD_FLOAT(it) })
    }

    class ADD_VEC(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ ADD_VEC(it) })
    }

    class SUB_FLOAT(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ SUB_FLOAT(it) })
    }

    class SUB_VEC(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ SUB_VEC(it) })
    }

    class EQ(val type: Class<out Type>, args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object {
            fun get(type: Class<out Type>) = Factory { EQ(type, it) }
        }

        override fun name(f: (Int) -> String) = "EQ<${type.getSimpleName()}>"
    }

    class NE(val type: Class<out Type>, args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object {
            fun get(type: Class<out Type>) = Factory { NE(type, it) }
        }

        override fun name(f: (Int) -> String) = "NE<${type.getSimpleName()}>"
    }

    class LE(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ LE(it) })
    }

    class GE(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ GE(it) })
    }

    class LT(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ LT(it) })
    }

    class GT(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ GT(it) })
    }

    class LOAD(val type: Class<out Type>, args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object {
            fun get(type: Class<out Type>) = Factory { LOAD(type, it) }
        }

        override fun name(f: (Int) -> String) = "LOAD<${type.getSimpleName()}>"
    }

    class ADDRESS(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ ADDRESS(it) })
    }

    class STORE(val type: Class<out Type>, args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object {
            fun get(type: Class<out Type>) = Factory { STORE(type, it) }
        }

        override fun name(f: (Int) -> String) = "STORE<${type.getSimpleName()}>"
    }

    class STOREP(val type: Class<out Type>, args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object {
            fun get(type: Class<out Type>) = Factory { STOREP(type, it) }
        }

        override fun name(f: (Int) -> String) = "STOREP<${type.getSimpleName()}>"
    }

    class RETURN(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ RETURN(it) })
    }

    class NOT(val type: Class<out Type>, args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object {
            fun get(type: Class<out Type>) = Factory { NOT(type, it) }
        }

        override fun name(f: (Int) -> String) = "NOT<${type.getSimpleName()}>"
    }

    class CALL(val params: List<Int>, args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object {
            fun get(params: List<Int>) = Factory { CALL(params, it) }
        }

        override fun name(f: (Int) -> String) = "CALL<${params.size()}>(${params.joinToString(", ")})"
    }

    class STATE(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ STATE(it) })
    }

    class LABEL(val id: String) : Instruction {
        override fun name(f: (Int) -> String) = "label ${id}:"
    }

    interface GOTO : Instruction {
        /** Unconditional */
        class Label(val id: String) : GOTO {
            override fun name(f: (Int) -> String) = "goto ${id}"
        }

        /** Conditional */
        class If(val id: String, val condition: Int, val expect: Boolean = true) : GOTO {
            override fun name(f: (Int) -> String) = "if${if (expect) "" else "not"} ${f(condition)}, goto ${id}"
        }

        /** Temporary marker, replaced with unconditional goto */
        object Break : Instruction

        /** Temporary marker, replaced with unconditional goto */
        object Continue : Instruction
    }

    class AND(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ AND(it) })
    }

    class OR(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ OR(it) })
    }

    class BITAND(args: Triple<Int, Int, Int>) : WithArgs(args) {
        companion object : Factory({ BITAND(it) })
    }

    class BITOR(args: Triple<Int, Int, Int>) : WithArgs(args) {
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
        fun OFS_PARAM(n: Int) = 4 + n * 3

        fun OFS_STR(ret: Int) = when (ret) {
            in 1..3 -> "RETURN(${ret - 1})"
            in 4..27 -> "PARAM(${(ret - 4) / 3}${((ret - 4) % 3).let {
                when {
                    it != 0 -> ", $it"
                    else -> ""
                }
            }})"
            else -> ret
        }
    }

}
