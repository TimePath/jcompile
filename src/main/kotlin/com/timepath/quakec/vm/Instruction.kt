package com.timepath.quakec.vm

import java.nio.FloatBuffer
import java.nio.IntBuffer
import com.timepath.quakec.times

fun FloatBuffer.set(index: Int, value: Float) = this.put(index, value)
fun Boolean.toFloat(): Float = if (this) 1f else 0f

enum class Instruction(val stringify: (Statement) -> Array<Any>,
                       val action: (Statement, FloatBuffer, IntBuffer) -> Int) {

    DONE : Instruction({ it -> array(it.a, ",", it.b, ",", it.c) },
            { it, f, i ->
                f[OFS_PARAM(-1) + 0] = f[it.a + 0]
                f[OFS_PARAM(-1) + 1] = f[it.a + 1]
                f[OFS_PARAM(-1) + 2] = f[it.a + 2]
                0
            })
    MUL_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "*", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] * f[it.b])
                1
            })
    MUL_VEC : Instruction({ it -> array(it.c, "=", it.a, "*", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a + 0] * f[it.b + 0]
                        + f[it.a + 1] * f[it.b + 1]
                        + f[it.a + 2] * f[it.b + 2])
                1
            })

    MUL_FLOAT_VEC : Instruction({ it -> array(it.c, "=", it.a, "*", it.b) },
            { it, f, i ->
                f[it.c + 0] = (f[it.a] * f[it.b + 0])
                f[it.c + 1] = (f[it.a] * f[it.b + 1])
                f[it.c + 2] = (f[it.a] * f[it.b + 2])
                1
            })
    MUL_VEC_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "*", it.b) },
            { it, f, i ->
                f[it.c + 0] = (f[it.a + 0] * f[it.b])
                f[it.c + 1] = (f[it.a + 1] * f[it.b])
                f[it.c + 2] = (f[it.a + 2] * f[it.b])
                1
            })

    DIV_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "/", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] / f[it.b])
                1
            })

    ADD_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "+", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] + f[it.b])
                1
            })
    ADD_VEC : Instruction({ it -> array(it.c, "=", it.a, "+", it.b) },
            { it, f, i ->
                f[it.c + 0] = (f[it.a + 0] + f[it.b + 0])
                f[it.c + 1] = (f[it.a + 1] + f[it.b + 1])
                f[it.c + 2] = (f[it.a + 2] + f[it.b + 2])
                1
            })

    SUB_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "-", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] - f[it.b])
                1
            })
    SUB_VEC : Instruction({ it -> array(it.c, "=", it.a, "-", it.b) },
            { it, f, i ->
                f[it.c + 0] = (f[it.a + 0] - f[it.b + 0])
                f[it.c + 1] = (f[it.a + 1] - f[it.b + 1])
                f[it.c + 2] = (f[it.a + 2] - f[it.b + 2])
                1
            })

    EQ_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] == f[it.b]).toFloat()
                1
            })
    EQ_VEC : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a + 0] == f[it.b + 0]
                        && f[it.a + 1] == f[it.b + 1]
                        && f[it.a + 2] == f[it.b + 2]).toFloat()
                1
            })
    // TODO
    EQ_STR : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) }, { it, f, i -> 1 })
    EQ_ENT : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) }, { it, f, i -> 1 })
    EQ_FUNC : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) }, { it, f, i -> 1 })

    NE_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] != f[it.b]).toFloat()
                1
            })
    NE_VEC : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a + 0] != f[it.b + 0]
                        || f[it.a + 1] != f[it.b + 1]
                        || f[it.a + 2] != f[it.b + 2]).toFloat()
                1
            })
    // TODO
    NE_STR : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) }, { it, f, i -> 1 })
    NE_ENT : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) }, { it, f, i -> 1 })
    NE_FUNC : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) }, { it, f, i -> 1 })

    LE : Instruction({ it -> array(it.c, "=", it.a, "<=", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] <= f[it.b]).toFloat()
                1
            })
    GE : Instruction({ it -> array(it.c, "=", it.a, ">=", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] >= f[it.b]).toFloat()
                1
            })
    LT : Instruction({ it -> array(it.c, "=", it.a, "<", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] < f[it.b]).toFloat()
                1
            })
    GT : Instruction({ it -> array(it.c, "=", it.a, ">", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] > f[it.b]).toFloat()
                1
            })

    // TODO
    LOAD_FLOAT : Instruction({ it -> array(it.a, "->", it.c, "=", it.a) }, { it, f, i -> 1 })
    LOAD_VEC : Instruction({ it -> array(it.a, "->", it.c, "=", it.a) }, { it, f, i -> 1 })
    LOAD_STR : Instruction({ it -> array(it.a, "->", it.c, "=", it.a) }, { it, f, i -> 1 })
    LOAD_ENT : Instruction({ it -> array(it.a, "->", it.c, "=", it.a) }, { it, f, i -> 1 })
    LOAD_FIELD : Instruction({ it -> array(it.a, "->", it.c, "=", it.a) }, { it, f, i -> 1 })
    LOAD_FUNC : Instruction({ it -> array(it.a, "->", it.c, "=", it.a) }, { it, f, i -> 1 })

    ADDRESS : Instruction({ it -> array("ILLEGAL") }, { it, f, i -> 1 })

    STORE_FLOAT : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f[it.b] = f[it.a]
                1
            })
    STORE_VEC : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f[it.b + 0] = f[it.a + 0]
                f[it.b + 1] = f[it.a + 1]
                f[it.b + 2] = f[it.a + 2]
                1
            })
    STORE_STR : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f[it.b] = f[it.a]
                1
            })
    STORE_ENT : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f[it.b] = f[it.a]
                1
            })
    STORE_FIELD : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f[it.b] = f[it.a]
                1
            })
    STORE_FUNC : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f[it.b] = f[it.a]
                1
            })

    // TODO
    STOREP_FLOAT : Instruction({ it -> array(it.b, "=", it.a) }, { it, f, i -> 1 })
    STOREP_VEC : Instruction({ it -> array(it.b, "=", it.a) }, { it, f, i -> 1 })
    STOREP_STR : Instruction({ it -> array(it.b, "=", it.a) }, { it, f, i -> 1 })
    STOREP_ENT : Instruction({ it -> array(it.b, "=", it.a) }, { it, f, i -> 1 })
    STOREP_FIELD : Instruction({ it -> array(it.b, "=", it.a) }, { it, f, i -> 1 })
    STOREP_FUNC : Instruction({ it -> array(it.b, "=", it.a) }, { it, f, i -> 1 })

    RETURN : Instruction({ it -> array(it.a, ",", it.b, ",", it.c) },
            { it, f, i ->
                f[OFS_PARAM(-1)] = f[it.a]
                0
            })

    NOT_FLOAT : Instruction({ it -> array("!", it.a) },
            { it, f, i ->
                f[it.b] = (f[it.a] == 0f).toFloat()
                1
            })
    NOT_VEC : Instruction({ it -> array("!", it.a) },
            { it, f, i ->
                f[it.c] = (f[it.a + 0] == 0f
                        && f[it.a + 1] == 0f
                        && f[it.a + 2] == 0f).toFloat()
                1
            })
    // TODO
    NOT_STR : Instruction({ it -> array("!", it.a) }, { it, f, i -> 1 })
    NOT_ENT : Instruction({ it -> array("!", it.a) }, { it, f, i -> 1 })
    NOT_FUNC : Instruction({ it -> array("!", it.a) }, { it, f, i -> 1 })

    IF : Instruction({ it -> array("if", it.a, "then jmp rel", it.b) },
            { it, f, i ->
                (if (f[it.a] != 0f) it.b else 1)
            })
    IFNOT : Instruction({ it -> array("if not", it.a, "then jmp rel", it.b) },
            { it, f, i ->
                (if (f[it.a] == 0f) it.b else 1)
            })

    CALL0 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })
    CALL1 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })
    CALL2 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })
    CALL3 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })
    CALL4 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })
    CALL5 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })
    CALL6 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })
    CALL7 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })
    CALL8 : Instruction({ it -> array(it.a, "(...)") },
            { it, f, i -> 1 })

    STATE : Instruction({ it -> array("ILLEGAL") }, { it, f, i -> 1 })

    GOTO : Instruction({ it -> array("jmp rel", it.a) },
            { it, f, i ->
                it.a
            })

    AND : Instruction({ it -> array(it.c, "=", it.a, "&&", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] != 0f && f[it.b] != 0f).toFloat()
                1
            })
    OR : Instruction({ it -> array(it.c, "=", it.a, "||", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a] != 0f || f[it.b] != 0f).toFloat()
                1
            })

    BITAND : Instruction({ it -> array(it.c, "=", it.a, "&", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a].toInt() and f[it.b].toInt()).toFloat()
                1
            })
    BITOR : Instruction({ it -> array(it.c, "=", it.a, "|", it.b) },
            { it, f, i ->
                f[it.c] = (f[it.a].toInt() or f[it.b].toInt()).toFloat()
                1
            })

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
                else -> "\$$it (${get(it) ?: "?"})"
            }
        }
        return "${this.name()}${" " * (11 - name().length())}\t(${stringified.joinToString(" ")})"
    }

}

