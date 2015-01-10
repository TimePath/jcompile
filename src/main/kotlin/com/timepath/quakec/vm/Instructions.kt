package com.timepath.quakec.vm

import java.nio.FloatBuffer
import java.nio.IntBuffer

fun FloatBuffer.set(index: Int, value: Float) = this.put(index, value)
fun Boolean.toFloat(): Float = if (this) 1f else 0f

enum class Instruction {

    protected open fun stringify(it: Statement): Array<Any> = array("ILLEGAL")
    protected open fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
    }

    protected open fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = 1

    DONE {
        override fun stringify(it: Statement): Array<Any> = array(it.a, ",", it.b, ",", it.c)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[OFS_PARAM(-1) + 0] = f[it.a + 0]
            f[OFS_PARAM(-1) + 1] = f[it.a + 1]
            f[OFS_PARAM(-1) + 2] = f[it.a + 2]
        }

        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = 0
    }

    MUL_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] * f[it.b])
        }
    }
    MUL_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a + 0] * f[it.b + 0]
                    + f[it.a + 1] * f[it.b + 1]
                    + f[it.a + 2] * f[it.b + 2])
        }
    }
    MUL_FLOAT_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c + 0] = (f[it.a] * f[it.b + 0])
            f[it.c + 1] = (f[it.a] * f[it.b + 1])
            f[it.c + 2] = (f[it.a] * f[it.b + 2])
        }
    }
    MUL_VEC_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c + 0] = (f[it.a + 0] * f[it.b])
            f[it.c + 1] = (f[it.a + 1] * f[it.b])
            f[it.c + 2] = (f[it.a + 2] * f[it.b])
        }
    }
    DIV_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "/", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] / f[it.b])
        }
    }

    ADD_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "+", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] + f[it.b])
        }
    }
    ADD_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "+", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c + 0] = (f[it.a + 0] + f[it.b + 0])
            f[it.c + 1] = (f[it.a + 1] + f[it.b + 1])
            f[it.c + 2] = (f[it.a + 2] + f[it.b + 2])
        }
    }
    SUB_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "-", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] - f[it.b])
        }
    }
    SUB_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "-", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c + 0] = (f[it.a + 0] - f[it.b + 0])
            f[it.c + 1] = (f[it.a + 1] - f[it.b + 1])
            f[it.c + 2] = (f[it.a + 2] - f[it.b + 2])
        }
    }

    EQ_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] == f[it.b]).toFloat()
        }
    }
    EQ_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a + 0] == f[it.b + 0]
                    && f[it.a + 1] == f[it.b + 1]
                    && f[it.a + 2] == f[it.b + 2]).toFloat()
        }
    }
    // TODO
    EQ_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
    }
    // TODO
    EQ_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
    }
    // TODO
    EQ_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
    }

    NE_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] != f[it.b]).toFloat()
        }
    }
    NE_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a + 0] != f[it.b + 0]
                    || f[it.a + 1] != f[it.b + 1]
                    || f[it.a + 2] != f[it.b + 2]).toFloat()
        }
    }
    // TODO
    NE_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
    }
    // TODO
    NE_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
    }
    // TODO
    NE_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
    }

    LE {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "<=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] <= f[it.b]).toFloat()
        }
    }
    GE {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, ">=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] >= f[it.b]).toFloat()
        }
    }
    LT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "<", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] < f[it.b]).toFloat()
        }
    }
    GT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, ">", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] > f[it.b]).toFloat()
        }
    }

    // TODO
    LOAD_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
    }
    // TODO
    LOAD_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
    }
    // TODO
    LOAD_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
    }
    // TODO
    LOAD_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
    }
    // TODO
    LOAD_FIELD {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
    }
    // TODO
    LOAD_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "->", it.c, "=", it.a)
    }

    ADDRESS {
        override fun stringify(it: Statement): Array<Any> = array("ILLEGAL")
    }

    STORE_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.b] = f[it.a]
        }
    }
    STORE_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.b + 0] = f[it.a + 0]
            f[it.b + 1] = f[it.a + 1]
            f[it.b + 2] = f[it.a + 2]
        }
    }
    STORE_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.b] = f[it.a]
        }
    }
    STORE_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.b] = f[it.a]
        }
    }
    STORE_FIELD {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.b] = f[it.a]
        }
    }
    STORE_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.b] = f[it.a]
        }
    }

    // TODO
    STOREP_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
    }
    STOREP_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
    }
    STOREP_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
    }
    STOREP_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
    }
    STOREP_FIELD {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
    }
    STOREP_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
    }

    RETURN {
        override fun stringify(it: Statement): Array<Any> = array(it.a, ",", it.b, ",", it.c)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[OFS_PARAM(-1)] = f[it.a]
        }

        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = 0
    }

    NOT_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.b] = (f[it.a] == 0f).toFloat()
        }
    }
    NOT_VEC {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a + 0] == 0f
                    && f[it.a + 1] == 0f
                    && f[it.a + 2] == 0f).toFloat()
        }
    }
    // TODO
    NOT_STR {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
    }
    NOT_ENT {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
    }
    NOT_FUNC {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
    }

    IF {
        override fun stringify(it: Statement): Array<Any> = array("if", it.a, "then jmp rel", it.b)
        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = (if (f[it.a] != 0f) it.b else 1)
    }
    IFNOT {
        override fun stringify(it: Statement): Array<Any> = array("if not", it.a, "then jmp rel", it.b)
        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = (if (f[it.a] == 0f) it.b else 1)
    }

    CALL0 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }
    CALL1 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }
    CALL2 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }
    CALL3 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }
    CALL4 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }
    CALL5 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }
    CALL6 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }
    CALL7 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }
    CALL8 {
        override fun stringify(it: Statement): Array<Any> = array(it.a, "(...)")
    }

    STATE {
        override fun stringify(it: Statement): Array<Any> = array("ILLEGAL")
    }

    GOTO {
        override fun stringify(it: Statement): Array<Any> = array("jmp rel", it.a)
        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = it.a
    }

    AND {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "&&", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] != 0f && f[it.b] != 0f).toFloat()
        }
    }
    OR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "||", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a] != 0f || f[it.b] != 0f).toFloat()
        }
    }

    BITAND {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "&", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a].toInt() and f[it.b].toInt()).toFloat()
        }
    }
    BITOR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "|", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer) {
            f[it.c] = (f[it.a].toInt() or f[it.b].toInt()).toFloat()
        }
    }

    class object {

        /**
         * RETURN = 1, n = -1
         * PARAM0 = 4, n = 0
         * PARAM1 = 7, n = 1
         * ...
         * PARMA7 = 25
         */
        fun OFS_PARAM(n: Int) = 4 + n * 3

        fun from(i: Int) = Instruction.values()[i]

    }

    fun invoke(s: Statement, data: ProgramData): Int {
        action(s, data.globalFloatData, data.globalIntData)
        return advance(s, data.globalFloatData, data.globalIntData)
    }

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
        return "${this.name()}${" ".repeat(11 - name().length())}\t(${stringified.joinToString(" ")}}"
    }

}