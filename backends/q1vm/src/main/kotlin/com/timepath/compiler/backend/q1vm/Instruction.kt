package com.timepath.compiler.backend.q1vm

import com.timepath.q1vm.ProgramData
import com.timepath.q1vm.StringManager
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.properties.Delegates

enum class Instruction {

    DONE,

    MUL_FLOAT,
    MUL_VEC,
    MUL_FLOAT_VEC,
    MUL_VEC_FLOAT,
    DIV_FLOAT,

    ADD_FLOAT,
    ADD_VEC,
    SUB_FLOAT,
    SUB_VEC,

    EQ_FLOAT,
    EQ_VEC,
    EQ_STR,
    EQ_ENT,
    EQ_FUNC,

    NE_FLOAT,
    NE_VEC,
    NE_STR,
    NE_ENT,
    NE_FUNC,

    LE,
    GE,
    LT,
    GT,

    LOAD_FLOAT,
    LOAD_VEC,
    LOAD_STR,
    LOAD_ENT,
    LOAD_FIELD,
    LOAD_FUNC,

    ADDRESS,

    STORE_FLOAT,
    STORE_VEC,
    STORE_STR,
    STORE_ENT,
    STORE_FIELD,
    STORE_FUNC,

    STOREP_FLOAT,
    STOREP_VEC,
    STOREP_STR,
    STOREP_ENT,
    STOREP_FIELD,
    STOREP_FUNC,

    RETURN,

    NOT_FLOAT,
    NOT_VEC,
    NOT_STR,
    NOT_ENT,
    NOT_FUNC,

    IF,
    IFNOT,

    CALL0,
    CALL1,
    CALL2,
    CALL3,
    CALL4,
    CALL5,
    CALL6,
    CALL7,
    CALL8,

    STATE,

    GOTO,

    AND,
    OR,

    BITAND,
    BITOR;

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

        private val instructions by Delegates.lazy(::values)
        fun from(i: Int) = instructions[i]

    }

}
