package com.timepath.quakec.vm

import java.nio.FloatBuffer
import java.nio.IntBuffer

fun FloatBuffer.set(index: Int, value: Float) = this.put(index, value)
fun IntBuffer.set(index: Int, value: Int) = this.put(index, value)
fun Float.not(): Boolean = this == 0f
fun Int.not(): Boolean = this == 0
fun Boolean.toFloat(): Float = if (this) 1f else 0f

enum class Instruction {

    protected open fun stringify(it: Statement): Array<Any> = array("ILLEGAL")
    protected open fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
    }

    protected open fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = 1

    /**
     * Same as RETURN, used for disassembly
     */
    DONE {
        override fun stringify(it: Statement): Array<Any> = array(it.a, ",", it.b, ",", it.c)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[OFS_PARAM(-1) + 0] = f[it.a + 0]
            f[OFS_PARAM(-1) + 1] = f[it.a + 1]
            f[OFS_PARAM(-1) + 2] = f[it.a + 2]
        }

        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = 0
    }

    MUL_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = f[it.a] * f[it.b]
        }
    }
    MUL_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a + 0] * f[it.b + 0]
                    + f[it.a + 1] * f[it.b + 1]
                    + f[it.a + 2] * f[it.b + 2])
        }
    }
    MUL_FLOAT_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c + 0] = f[it.a] * f[it.b + 0]
            f[it.c + 1] = f[it.a] * f[it.b + 1]
            f[it.c + 2] = f[it.a] * f[it.b + 2]
        }
    }
    MUL_VEC_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "*", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c + 0] = f[it.a + 0] * f[it.b]
            f[it.c + 1] = f[it.a + 1] * f[it.b]
            f[it.c + 2] = f[it.a + 2] * f[it.b]
        }
    }
    DIV_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "/", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = f[it.a] / f[it.b]
        }
    }

    ADD_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "+", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = f[it.a] + f[it.b]
        }
    }
    ADD_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "+", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c + 0] = f[it.a + 0] + f[it.b + 0]
            f[it.c + 1] = f[it.a + 1] + f[it.b + 1]
            f[it.c + 2] = f[it.a + 2] + f[it.b + 2]
        }
    }
    SUB_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "-", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = f[it.a] - f[it.b]
        }
    }
    SUB_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "-", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c + 0] = f[it.a + 0] - f[it.b + 0]
            f[it.c + 1] = f[it.a + 1] - f[it.b + 1]
            f[it.c + 2] = f[it.a + 2] - f[it.b + 2]
        }
    }

    EQ_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a] == f[it.b]).toFloat()
        }
    }
    EQ_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a + 0] == f[it.b + 0]
                    && f[it.a + 1] == f[it.b + 1]
                    && f[it.a + 2] == f[it.b + 2]).toFloat()
        }
    }
    EQ_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (s[it.a] == s[it.b]).toFloat()
        }
    }
    EQ_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (i[it.a] == i[it.b]).toFloat()
        }
    }
    EQ_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "==", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (i[it.a] == i[it.b]).toFloat()
        }
    }

    NE_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a] != f[it.b]).toFloat()
        }
    }
    NE_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a + 0] != f[it.b + 0]
                    || f[it.a + 1] != f[it.b + 1]
                    || f[it.a + 2] != f[it.b + 2]).toFloat()
        }
    }
    NE_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (s[it.a] != s[it.b]).toFloat()
        }
    }
    NE_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (i[it.a] != i[it.b]).toFloat()
        }
    }
    NE_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "!=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (i[it.a] != i[it.b]).toFloat()
        }
    }

    LE {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "<=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a] <= f[it.b]).toFloat()
        }
    }
    GE {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, ">=", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a] >= f[it.b]).toFloat()
        }
    }
    LT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "<", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a] < f[it.b]).toFloat()
        }
    }
    GT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, ">", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a] > f[it.b]).toFloat()
        }
    }

    LOAD_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "->", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = e.readFloat(i[it.a], i[it.b])
        }
    }
    LOAD_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "->", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            val (x, y, z) = e.readVector(i[it.a], i[it.b])
            f[it.c + 0] = x
            f[it.c + 1] = y
            f[it.c + 2] = z
        }
    }
    LOAD_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "->", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            i[it.c] = e.readInt(i[it.a], i[it.b])
        }
    }
    LOAD_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "->", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            i[it.c] = e.readInt(i[it.a], i[it.b])
        }
    }
    LOAD_FIELD {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "->", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            i[it.c] = e.readInt(i[it.a], i[it.b])
        }
    }
    LOAD_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "->", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            i[it.c] = e.readInt(i[it.a], i[it.b])
        }
    }

    ADDRESS {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", "&", it.a, "->", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            i[it.c] = e.getAddress(i[it.a], i[it.b])
        }
    }

    STORE_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.b] = f[it.a]
        }
    }
    STORE_VEC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.b + 0] = f[it.a + 0]
            f[it.b + 1] = f[it.a + 1]
            f[it.b + 2] = f[it.a + 2]
        }
    }
    STORE_STR {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.b] = f[it.a]
        }
    }
    STORE_ENT {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.b] = f[it.a]
        }
    }
    STORE_FIELD {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.b] = f[it.a]
        }
    }
    STORE_FUNC {
        override fun stringify(it: Statement): Array<Any> = array(it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.b] = f[it.a]
        }
    }

    STOREP_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array("*", it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            e.writeFloat(i[it.b], f[it.a])
        }
    }
    STOREP_VEC {
        override fun stringify(it: Statement): Array<Any> = array("*", it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            val vec = array(f[it.a + 0], f[it.a + 1], f[it.a + 2])
            e.writeVector(i[it.b], vec)
        }
    }
    STOREP_STR {
        override fun stringify(it: Statement): Array<Any> = array("*", it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            e.writeInt(i[it.b], f[it.a])
        }
    }
    STOREP_ENT {
        override fun stringify(it: Statement): Array<Any> = array("*", it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            e.writeInt(i[it.b], f[it.a])
        }
    }
    STOREP_FIELD {
        override fun stringify(it: Statement): Array<Any> = array("*", it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            e.writeInt(i[it.b], f[it.a])
        }
    }
    STOREP_FUNC {
        override fun stringify(it: Statement): Array<Any> = array("*", it.b, "=", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            e.writeInt(i[it.b], f[it.a])
        }
    }

    RETURN {
        override fun stringify(it: Statement): Array<Any> = array(it.a, ",", it.b, ",", it.c)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[OFS_PARAM(-1) + 0] = f[it.a + 0]
            f[OFS_PARAM(-1) + 1] = f[it.a + 1]
            f[OFS_PARAM(-1) + 2] = f[it.a + 2]
        }

        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = 0
    }

    NOT_FLOAT {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.b] = (!f[it.a]).toFloat()
        }
    }
    NOT_VEC {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (!f[it.a + 0]
                    && !f[it.a + 1]
                    && !f[it.a + 2]).toFloat()
        }
    }
    NOT_STR {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (!i[it.a]).toFloat()
        }
    }
    NOT_ENT {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (!i[it.a]).toFloat()
        }
    }
    NOT_FUNC {
        override fun stringify(it: Statement): Array<Any> = array("!", it.a)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (!i[it.a]).toFloat()
        }
    }

    IF {
        override fun stringify(it: Statement): Array<Any> = array("if", it.a, "then jmp rel", it.b)
        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = (if (!!f[it.a]) it.b else 1)
    }
    IFNOT {
        override fun stringify(it: Statement): Array<Any> = array("if not", it.a, "then jmp rel", it.b)
        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = (if (!f[it.a]) it.b else 1)
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
        override fun stringify(it: Statement): Array<Any> = array(
                "self.nextthink", "=", "time + 0.1", ";",
                "self.frame", "=", it.a, ";", // f[it.a]
                "self.think", "=", it.b) // i[it.b]
    }

    GOTO {
        override fun stringify(it: Statement): Array<Any> = array("jmp rel", it.a)
        override fun advance(it: Statement, f: FloatBuffer, i: IntBuffer): Int = it.a
    }

    AND {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "&&", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (!!f[it.a] && !!f[it.b]).toFloat()
        }
    }
    OR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "||", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (!!f[it.a] || !!f[it.b]).toFloat()
        }
    }

    BITAND {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "&", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
            f[it.c] = (f[it.a].toInt() and f[it.b].toInt()).toFloat()
        }
    }
    BITOR {
        override fun stringify(it: Statement): Array<Any> = array(it.c, "=", it.a, "|", it.b)
        override fun action(it: Statement, f: FloatBuffer, i: IntBuffer, s: StringManager, e: EntityManager) {
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

    fun invoke(it: Statement, data: ProgramData): Int {
        val f = data.globalFloatData
        val i = data.globalIntData
        val s = data.strings
        val e = data.entities
        action(it, f, i, s, e)
        return advance(it, f, i)
    }

    fun toString(s: Statement, data: ProgramData?): String {
        val stringified = stringify(s).map {
            when (it) {
                !is Int -> it
                else -> "\$$it {${if (data != null) {
                    "i: ${data.globalIntData[it]}, f: ${data.globalFloatData[it]}"
                } else {
                    null
                } ?: "?"}}"
            }
        }
        return "${this.name()}${" ".repeat(13 - name().length())}\t[${stringified.joinToString(" ")}]"
    }

}