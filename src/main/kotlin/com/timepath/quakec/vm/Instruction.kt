package com.timepath.quakec.vm

import com.timepath.quakec.vm.defs.Statement
import java.nio.FloatBuffer
import java.nio.IntBuffer
import com.timepath.quakec.vm.defs.ProgramData
import com.timepath.quakec.times

enum class Instruction(val stringify: (Statement) -> Array<Any>,
                       val action: (Statement, FloatBuffer, IntBuffer) -> Int) {

    DONE : Instruction({ it -> array(it.a, ",", it.b, ",", it.c) },
            { it, f, i ->
                f.put(OFS_RETURN + 0, f.get(it.a + 0))
                f.put(OFS_RETURN + 1, f.get(it.a + 1))
                f.put(OFS_RETURN + 2, f.get(it.a + 2))
                0
            })
    MUL_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "*", it.b) },
            { it, f, i ->
                f.put(it.c, (f.get(it.a) * f.get(it.b)))
                1
            })
    MUL_VEC : Instruction({ it -> array(it.c, "=", it.a, "*", it.b) },
            { it, f, i ->
                f.put(it.c, (
                        f.get(it.a + 0) * f.get(it.b + 0)
                                + f.get(it.a + 1) * f.get(it.b + 1)
                                + f.get(it.a + 2) * f.get(it.b + 2)
                        ))
                1
            })

    MUL_FLOAT_VEC : Instruction({ it -> array(it.c, "=", it.a, "*", it.b) },
            { it, f, i ->
                f.put(it.c + 0, (f.get(it.a) * f.get(it.b + 0)))
                f.put(it.c + 1, (f.get(it.a) * f.get(it.b + 1)))
                f.put(it.c + 2, (f.get(it.a) * f.get(it.b + 2)))
                1
            })
    MUL_VEC_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "*", it.b) },
            { it, f, i ->
                f.put(it.c + 0, (f.get(it.a + 0) * f.get(it.b)))
                f.put(it.c + 1, (f.get(it.a + 1) * f.get(it.b)))
                f.put(it.c + 2, (f.get(it.a + 2) * f.get(it.b)))
                1
            })

    DIV_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "/", it.b) },
            { it, f, i ->
                f.put(it.c, (f.get(it.a) / f.get(it.b)))
                1
            })

    ADD_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "+", it.b) },
            { it, f, i ->
                f.put(it.c, (f.get(it.a) + f.get(it.b)))
                1
            })
    ADD_VEC : Instruction({ it -> array(it.c, "=", it.a, "+", it.b) },
            { it, f, i ->
                f.put(it.c + 0, (f.get(it.a + 0) + f.get(it.b + 0)))
                f.put(it.c + 1, (f.get(it.a + 1) + f.get(it.b + 1)))
                f.put(it.c + 2, (f.get(it.a + 2) + f.get(it.b + 2)))
                1
            })

    SUB_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "-", it.b) },
            { it, f, i ->
                f.put(it.c, (f.get(it.a) - f.get(it.b)))
                1
            })
    SUB_VEC : Instruction({ it -> array(it.c, "=", it.a, "-", it.b) },
            { it, f, i ->
                f.put(it.c + 0, (f.get(it.a + 0) - f.get(it.b + 0)))
                f.put(it.c + 1, (f.get(it.a + 1) - f.get(it.b + 1)))
                f.put(it.c + 2, (f.get(it.a + 2) - f.get(it.b + 2)))
                1
            })

    EQ_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a) == f.get(it.b)) 1f else 0f)
                1
            })
    EQ_VEC : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a + 0) == f.get(it.b + 0)
                        && f.get(it.a + 1) == f.get(it.b + 1)
                        && f.get(it.a + 2) == f.get(it.b + 2)) 1f else 0f)
                1
            })
    // TODO
    EQ_STR : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) }, { it, f, i -> 1 })
    EQ_ENT : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) }, { it, f, i -> 1 })
    EQ_FUNC : Instruction({ it -> array(it.c, "=", it.a, "==", it.b) }, { it, f, i -> 1 })

    NE_FLOAT : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a) != f.get(it.b)) 1f else 0f)
                1
            })
    NE_VEC : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a + 0) != f.get(it.b + 0)
                        || f.get(it.a + 1) != f.get(it.b + 1)
                        || f.get(it.a + 2) != f.get(it.b + 2)) 1f else 0f)
                1
            })
    // TODO
    NE_STR : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) }, { it, f, i -> 1 })
    NE_ENT : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) }, { it, f, i -> 1 })
    NE_FUNC : Instruction({ it -> array(it.c, "=", it.a, "!=", it.b) }, { it, f, i -> 1 })

    LE : Instruction({ it -> array(it.c, "=", it.a, "<=", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a) <= f.get(it.b)) 1f else 0f)
                1
            })
    GE : Instruction({ it -> array(it.c, "=", it.a, ">=", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a) >= f.get(it.b)) 1f else 0f)
                1
            })
    LT : Instruction({ it -> array(it.c, "=", it.a, "<", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a) < f.get(it.b)) 1f else 0f)
                1
            })
    GT : Instruction({ it -> array(it.c, "=", it.a, ">", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a) > f.get(it.b)) 1f else 0f)
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
                f.put(it.b, f.get(it.a))
                1
            })
    STORE_VEC : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f.put(it.b + 0, f.get(it.a + 0))
                f.put(it.b + 1, f.get(it.a + 1))
                f.put(it.b + 2, f.get(it.a + 2))
                1
            })
    STORE_STR : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f.put(it.b, f.get(it.a))
                1
            })
    STORE_ENT : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f.put(it.b, f.get(it.a))
                1
            })
    STORE_FIELD : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f.put(it.b, f.get(it.a))
                1
            })
    STORE_FUNC : Instruction({ it -> array(it.b, "=", it.a) },
            { it, f, i ->
                f.put(it.b, f.get(it.a))
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
                f.put(OFS_RETURN, f.get(it.a))
                0
            })

    NOT_FLOAT : Instruction({ it -> array("!", it.a) },
            { it, f, i ->
                f.put(it.b, if (f.get(it.a) == 0f) 1f else 0f)
                1
            })
    NOT_VEC : Instruction({ it -> array("!", it.a) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a + 0) == 0f
                        && f.get(it.a + 1) == 0f
                        && f.get(it.a + 2) == 0f) 1f else 0f)
                1
            })
    // TODO
    NOT_STR : Instruction({ it -> array("!", it.a) }, { it, f, i -> 1 })
    NOT_ENT : Instruction({ it -> array("!", it.a) }, { it, f, i -> 1 })
    NOT_FUNC : Instruction({ it -> array("!", it.a) }, { it, f, i -> 1 })

    IF : Instruction({ it -> array("if", it.a, "then jmp rel", it.b) },
            { it, f, i ->
                (if (f.get(it.a) != 0f) it.b else 1)
            })
    IFNOT : Instruction({ it -> array("if not", it.a, "then jmp rel", it.b) },
            { it, f, i ->
                (if (f.get(it.a) == 0f) it.b else 1)
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
                f.put(it.c, if (f.get(it.a) != 0f && f.get(it.b) != 0f) 1f else 0f)
                1
            })
    OR : Instruction({ it -> array(it.c, "=", it.a, "||", it.b) },
            { it, f, i ->
                f.put(it.c, if (f.get(it.a) != 0f || f.get(it.b) != 0f) 1f else 0f)
                1
            })

    BITAND : Instruction({ it -> array(it.c, "=", it.a, "&", it.b) },
            { it, f, i ->
                f.put(it.c, (f.get(it.a).toInt() and f.get(it.b).toInt()).toFloat())
                1
            })
    BITOR : Instruction({ it -> array(it.c, "=", it.a, "|", it.b) },
            { it, f, i ->
                f.put(it.c, (f.get(it.a).toInt() or f.get(it.b).toInt()).toFloat())
                1
            })

    class object {

        val OFS_RETURN = 1
        val OFS_PARM0 = 4
        val OFS_PARM1 = 7
        val OFS_PARM2 = 10
        val OFS_PARM3 = 13
        val OFS_PARM4 = 16
        val OFS_PARM5 = 19
        val OFS_PARM6 = 22
        val OFS_PARM7 = 25

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

