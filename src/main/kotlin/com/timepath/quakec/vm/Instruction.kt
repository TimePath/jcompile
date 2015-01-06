package com.timepath.quakec.vm

import java.nio.FloatBuffer
import java.nio.IntBuffer
import com.timepath.quakec.times

fun FloatBuffer.set(index: Int, value: Float) = this.put(index, value)
fun Boolean.toFloat(): Float = if (this) 1f else 0f

enum class Instruction {

    private abstract fun stringify(it: Statement): Array<Any>
    private abstract fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int

    DONE {
        override fun stringify(it: Statement): Array<Any> = array(it.a, ",", it.b, ",", it.c)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[OFS_PARAM(-1) + 0] = f[it.a + 0]
            f[OFS_PARAM(-1) + 1] = f[it.a + 1]
            f[OFS_PARAM(-1) + 2] = f[it.a + 2]
            return 0
        }
    }

    MUL_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] * f[it.b])
            return 1
        }
    }
    MUL_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a + 0] * f[it.b + 0]
                    + f[it.a + 1] * f[it.b + 1]
                    + f[it.a + 2] * f[it.b + 2])
            return 1
        }
    }
    MUL_FLOAT_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c + 0] = (f[it.a] * f[it.b + 0])
            f[it.c + 1] = (f[it.a] * f[it.b + 1])
            f[it.c + 2] = (f[it.a] * f[it.b + 2])
            return 1
        }
    }
    MUL_VEC_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c + 0] = (f[it.a + 0] * f[it.b])
            f[it.c + 1] = (f[it.a + 1] * f[it.b])
            f[it.c + 2] = (f[it.a + 2] * f[it.b])
            return 1
        }
    }
    DIV_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "/", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] / f[it.b])
            return 1
        }
    }

    ADD_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "+", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] + f[it.b])
            return 1
        }
    }
    ADD_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "+", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c + 0] = (f[it.a + 0] + f[it.b + 0])
            f[it.c + 1] = (f[it.a + 1] + f[it.b + 1])
            f[it.c + 2] = (f[it.a + 2] + f[it.b + 2])
            return 1
        }
    }
    SUB_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "-", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] - f[it.b])
            return 1
        }
    }
    SUB_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "-", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c + 0] = (f[it.a + 0] - f[it.b + 0])
            f[it.c + 1] = (f[it.a + 1] - f[it.b + 1])
            f[it.c + 2] = (f[it.a + 2] - f[it.b + 2])
            return 1
        }
    }

    EQ_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] == f[it.b]).toFloat()
            return 1
        }
    }
    EQ_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a + 0] == f[it.b + 0]
                    && f[it.a + 1] == f[it.b + 1]
                    && f[it.a + 2] == f[it.b + 2]).toFloat()
            return 1
        }
    }
    // TODO
    EQ_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    // TODO
    EQ_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    // TODO
    EQ_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }

    NE_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] != f[it.b]).toFloat()
            return 1
        }
    }
    NE_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a + 0] != f[it.b + 0]
                    || f[it.a + 1] != f[it.b + 1]
                    || f[it.a + 2] != f[it.b + 2]).toFloat()
            return 1
        }
    }
    // TODO
    NE_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    // TODO
    NE_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    // TODO
    NE_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }

    LE {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "<=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] <= f[it.b]).toFloat()
            return 1
        }
    }
    GE {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, ">=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] >= f[it.b]).toFloat()
            return 1
        }
    }
    LT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "<", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] < f[it.b]).toFloat()
            return 1
        }
    }
    GT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, ">", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] > f[it.b]).toFloat()
            return 1
        }
    }

    // TODO
    LOAD_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    LOAD_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    LOAD_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    LOAD_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    LOAD_FIELD {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    LOAD_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }

    ADDRESS {
        override fun stringify(it: Statement): Array<Any> = array("ILLEGAL")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }

    STORE_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.b] = f[it.a]
            return 1
        }
    }
    STORE_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.b + 0] = f[it.a + 0]
            f[it.b + 1] = f[it.a + 1]
            f[it.b + 2] = f[it.a + 2]
            return 1
        }
    }
    STORE_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.b] = f[it.a]
            return 1
        }
    }
    STORE_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.b] = f[it.a]
            return 1
        }
    }
    STORE_FIELD {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.b] = f[it.a]
            return 1
        }
    }
    STORE_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.b] = f[it.a]
            return 1
        }
    }

    // TODO
    STOREP_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    STOREP_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    STOREP_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    STOREP_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    STOREP_FIELD {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    STOREP_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }

    RETURN {
        override fun stringify(it: Statement): Array<Any> = array(it.a, ",", it.b, ",", it.c)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[OFS_PARAM(-1)] = f[it.a]
            return 0
        }
    }

    NOT_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.b] = (f[it.a] == 0f).toFloat()
            return 1
        }
    }
    NOT_VEC {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a + 0] == 0f
                    && f[it.a + 1] == 0f
                    && f[it.a + 2] == 0f).toFloat()
            return 1
        }
    }
    // TODO
    NOT_STR {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    NOT_ENT {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    NOT_FUNC {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }

    IF {
        override fun stringify(it: Statement): Array<Any> = array("if", it.a, "then jmp rel", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return (if (f[it.a] != 0f) it.b else 1)
        }
    }
    IFNOT {
        override fun stringify(it: Statement): Array<Any> = array("if not", it.a, "then jmp rel", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return (if (f[it.a] == 0f) it.b else 1)
        }
    }

    CALL0 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    CALL1 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    CALL2 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    CALL3 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    CALL4 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    CALL5 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    CALL6 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    CALL7 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }
    CALL8 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }

    STATE {
        override fun stringify(it: Statement): Array<Any> = array("ILLEGAL")
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return 1
        }
    }

    GOTO {
        override fun stringify(it: Statement): Array<Any> = array("jmp rel", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            return it.a
        }
    }

    AND {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "&&", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] != 0f && f[it.b] != 0f).toFloat()
            return 1
        }
    }
    OR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "||", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a] != 0f || f[it.b] != 0f).toFloat()
            return 1
        }
    }

    BITAND {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "&", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a].toInt() and f[it.b].toInt()).toFloat()
            return 1
        }
    }
    BITOR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "|", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer): Int {
            f[it.c] = (f[it.a].toInt() or f[it.b].toInt()).toFloat()
            return 1
        }
    }

    class object {

        /**
         * RETURN = 1, n = -1
         * PARAM0 = 4, n = 0
         * PARAM1 = 7
         * ...
         * PARMA7 = 25
         */
        fun OFS_PARAM(n: Int) = 4 + n * 3

        fun from(i: Int) = Instruction.values()[i]

    }

    fun invoke(s: Statement, data: ProgramData): Int = action(s, data.globalFloatData, data.globalIntData)

    fun toString(s: Statement, data: ProgramData?): String {
        val get = {(it: Int) ->
            try {
                data?.globalFloatData?.get(it)
            } catch (e: IndexOutOfBoundsException) {
                null
            }
        }
        val stringified = stringify(s).map {
            when (it) {
                !is Int -> it
                else -> "\$$it (${get(it) ?: "?"}}"
            }
        }
        return "${this.name()}${" " * (11 - name().length())}\t(${stringified.joinToString(" ")}}"
    }

}

