package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.types.Type

interface Instruction {

    fun name(f: (Int) -> String) = javaClass.getSimpleName()

    object MUL_FLOAT : Instruction

    object MUL_VEC : Instruction

    object MUL_FLOAT_VEC : Instruction

    object MUL_VEC_FLOAT : Instruction

    object DIV_FLOAT : Instruction

    object ADD_FLOAT : Instruction

    object ADD_VEC : Instruction

    object SUB_FLOAT : Instruction

    object SUB_VEC : Instruction

    class EQ(val type: Class<out Type>) : Instruction {
        override fun name(f: (Int) -> String) = "EQ<${type.getSimpleName()}>"
    }

    class NE(val type: Class<out Type>) : Instruction {
        override fun name(f: (Int) -> String) = "EQ<${type.getSimpleName()}>"
    }

    object LE : Instruction

    object GE : Instruction

    object LT : Instruction

    object GT : Instruction

    class LOAD(val type: Class<out Type>) : Instruction {
        override fun name(f: (Int) -> String) = "LOAD<${type.getSimpleName()}>"
    }

    object ADDRESS : Instruction

    class STORE(val type: Class<out Type>) : Instruction {
        override fun name(f: (Int) -> String) = "STORE<${type.getSimpleName()}>"
    }

    class STOREP(val type: Class<out Type>) : Instruction {
        override fun name(f: (Int) -> String) = "STOREP<${type.getSimpleName()}>"
    }

    object RETURN : Instruction

    class NOT(val type: Class<out Type>) : Instruction {
        override fun name(f: (Int) -> String) = "NOT<${type.getSimpleName()}>"
    }

    class CALL(val args: List<Int>) : Instruction {
        override fun name(f: (Int) -> String) = "CALL<${args.size()}>(${args.joinToString(", ")})"
    }

    object STATE : Instruction

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

    object AND : Instruction

    object OR : Instruction

    object BITAND : Instruction

    object BITOR : Instruction

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
