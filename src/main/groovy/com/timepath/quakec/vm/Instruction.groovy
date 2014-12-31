package com.timepath.quakec.vm

import groovy.transform.CompileStatic

import java.nio.FloatBuffer
import java.nio.IntBuffer

@CompileStatic
enum Instruction {
    DONE({ Statement it -> [it.a, ',', it.b, ',', it.c] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(OFS_RETURN, f.get(it.a))
                0
            }),

    MUL_FLO({ Statement it -> [it.c, '=', it.a, '*', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (float) (f.get(it.a) * f.get(it.b)))
                1
            }),
    MUL_VEC({ Statement it -> [it.c, '=', it.a, '*', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (float) (
                        f.get(it.a + 0) * f.get(it.b + 0)
                                + f.get(it.a + 1) * f.get(it.b + 1)
                                + f.get(it.a + 2) * f.get(it.b + 2)
                ))
                1
            }),

    MUL_FLO_VEC({ Statement it -> [it.c, '=', it.a, '*', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c + 0, (float) (f.get(it.a) * f.get(it.b + 0)))
                f.put(it.c + 1, (float) (f.get(it.a) * f.get(it.b + 1)))
                f.put(it.c + 2, (float) (f.get(it.a) * f.get(it.b + 2)))
                1
            }),
    MUL_VEC_FLO({ Statement it -> [it.c, '=', it.a, '*', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c + 0, (float) (f.get(it.a + 0) * f.get(it.b)))
                f.put(it.c + 1, (float) (f.get(it.a + 1) * f.get(it.b)))
                f.put(it.c + 2, (float) (f.get(it.a + 2) * f.get(it.b)))
                1
            }),

    DIV_FLO({ Statement it -> [it.c, '=', it.a, '/', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (float) (f.get(it.a) / f.get(it.b)))
                1
            }),

    ADD_FLO({ Statement it -> [it.c, '=', it.a, '+', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (float) (f.get(it.a) + f.get(it.b)))
                1
            }),
    ADD_VEC({ Statement it -> [it.c, '=', it.a, '+', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c + 0, (float) (f.get(it.a + 0) + f.get(it.b + 0)))
                f.put(it.c + 1, (float) (f.get(it.a + 1) + f.get(it.b + 1)))
                f.put(it.c + 2, (float) (f.get(it.a + 2) + f.get(it.b + 2)))
                1
            }),

    SUB_FLO({ Statement it -> [it.c, '=', it.a, '-', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (float) (f.get(it.a) - f.get(it.b)))
                1
            }),
    SUB_VEC({ Statement it -> [it.c, '=', it.a, '-', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c + 0, (float) (f.get(it.a + 0) - f.get(it.b + 0)))
                f.put(it.c + 1, (float) (f.get(it.a + 1) - f.get(it.b + 1)))
                f.put(it.c + 2, (float) (f.get(it.a + 2) - f.get(it.b + 2)))
                1
            }),

    EQ_FLO({ Statement it -> [it.c, '=', it.a, '==', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a) == f.get(it.b) ? 1 : 0))
                1
            }),
    EQ_VEC({ Statement it -> [it.c, '=', it.a, '==', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a + 0) == f.get(it.b + 0)
                        && f.get(it.a + 1) == f.get(it.b + 1)
                        && f.get(it.a + 2) == f.get(it.b + 2)) ? 1 : 0)
                1
            }),
    // TODO
    EQ_STR({ Statement it -> [it.c, '=', it.a, '==', it.b] }, { 1 }),
    EQ_ENT({ Statement it -> [it.c, '=', it.a, '==', it.b] }, { 1 }),
    EQ_FNC({ Statement it -> [it.c, '=', it.a, '==', it.b] }, { 1 }),

    NE_FLO({ Statement it -> [it.c, '=', it.a, '!=', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a) != f.get(it.b) ? 1 : 0))
                1
            }),
    NE_VEC({ Statement it -> [it.c, '=', it.a, '!=', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a + 0) != f.get(it.b + 0)
                        || f.get(it.a + 1) != f.get(it.b + 1)
                        || f.get(it.a + 2) != f.get(it.b + 2)) ? 1 : 0)
                1
            }),
    // TODO
    NE_STR({ Statement it -> [it.c, '=', it.a, '!=', it.b] }, { 1 }),
    NE_ENT({ Statement it -> [it.c, '=', it.a, '!=', it.b] }, { 1 }),
    NE_FNC({ Statement it -> [it.c, '=', it.a, '!=', it.b] }, { 1 }),

    LE({ Statement it -> [it.c, '=', it.a, '<=', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a) <= f.get(it.b) ? 1 : 0))
                1
            }),
    GE({ Statement it -> [it.c, '=', it.a, '>=', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a) >= f.get(it.b) ? 1 : 0))
                1
            }),
    LT({ Statement it -> [it.c, '=', it.a, '<', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a) < f.get(it.b) ? 1 : 0))
                1
            }),
    GT({ Statement it -> [it.c, '=', it.a, '>', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a) > f.get(it.b) ? 1 : 0))
                1
            }),

    // TODO
    LOAD_FLO({ Statement it -> [it.a, '->', it.c, '=', it.a] }, { 1 }),
    LOAD_VEC({ Statement it -> [it.a, '->', it.c, '=', it.a] }, { 1 }),
    LOAD_STR({ Statement it -> [it.a, '->', it.c, '=', it.a] }, { 1 }),
    LOAD_ENT({ Statement it -> [it.a, '->', it.c, '=', it.a] }, { 1 }),
    LOAD_FLD({ Statement it -> [it.a, '->', it.c, '=', it.a] }, { 1 }),
    LOAD_FNC({ Statement it -> [it.a, '->', it.c, '=', it.a] }, { 1 }),

    LOAD_ADDRESS({ Statement it -> ["ILLEGAL"] }, { 1 }),

    STORE_FLO({ Statement it -> [it.b, '=', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.b, f.get(it.a))
                1
            }),
    STORE_VEC({ Statement it -> [it.b, '=', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.b + 0, f.get(it.a + 0))
                f.put(it.b + 1, f.get(it.a + 1))
                f.put(it.b + 2, f.get(it.a + 2))
                1
            }),
    STORE_STR({ Statement it -> [it.b, '=', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.b, f.get(it.a))
                1
            }),
    STORE_ENT({ Statement it -> [it.b, '=', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.b, f.get(it.a))
                1
            }),
    STORE_FLD({ Statement it -> [it.b, '=', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.b, f.get(it.a))
                1
            }),
    STORE_FNC({ Statement it -> [it.b, '=', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.b, f.get(it.a))
                1
            }),

    // TODO
    STOREP_FLO({ Statement it -> [it.b, '=', it.a] }, { 1 }),
    STOREP_VEC({ Statement it -> [it.b, '=', it.a] }, { 1 }),
    STOREP_STR({ Statement it -> [it.b, '=', it.a] }, { 1 }),
    STOREP_ENT({ Statement it -> [it.b, '=', it.a] }, { 1 }),
    STOREP_FLD({ Statement it -> [it.b, '=', it.a] }, { 1 }),
    STOREP_FNC({ Statement it -> [it.b, '=', it.a] }, { 1 }),

    RETURN({ Statement it -> [it.a, ',', it.b, ',', it.c] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(OFS_RETURN, f.get(it.a))
                0
            }),

    NOT_FLO({ Statement it -> ['!', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.b, !f.get(it.a) ? 1 : 0)
                1
            }),
    NOT_VEC({ Statement it -> ['!', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (!f.get(it.a + 0)
                        && !f.get(it.a + 1)
                        && !f.get(it.a + 2)) ? 1 : 0)
                1
            }),
    // TODO
    NOT_STR({ Statement it -> ['!', it.a] }, { 1 }),
    NOT_ENT({ Statement it -> ['!', it.a] }, { 1 }),
    NOT_FNC({ Statement it -> ['!', it.a] }, { 1 }),

    IF({ Statement it -> ['if', it.a, 'then jmp rel', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                (f.get(it.a) ? (int) it.b : 1)
            }),
    IFNOT({ Statement it -> ['if not', it.a, 'then jmp rel', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                (!f.get(it.a) ? (int) it.b : 1)
            }),

    CALL0({ Statement it -> [it.a, '(...)'] }, { 1 }),
    CALL1({ Statement it -> [it.a, '(...)'] }, { 1 }),
    CALL2({ Statement it -> [it.a, '(...)'] }, { 1 }),
    CALL3({ Statement it -> [it.a, '(...)'] }, { 1 }),
    CALL4({ Statement it -> [it.a, '(...)'] }, { 1 }),
    CALL5({ Statement it -> [it.a, '(...)'] }, { 1 }),
    CALL6({ Statement it -> [it.a, '(...)'] }, { 1 }),
    CALL7({ Statement it -> [it.a, '(...)'] }, { 1 }),
    CALL8({ Statement it -> [it.a, '(...)'] }, { 1 }),

    STATE({ Statement it -> ["ILLEGAL"] }, { 1 }),

    GOTO({ Statement it -> ['jmp rel', it.a] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                it.a
            }),

    AND({ Statement it -> [it.a, '&&', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a) && f.get(it.b)) ? 1 : 0)
                1
            }),
    OR({ Statement it -> [it.a, '||', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (f.get(it.a) || f.get(it.b)) ? 1 : 0)
                1
            }),

    BITAND({ Statement it -> [it.c, '=', it.a, '&', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (float) (f.get(it.a) & f.get(it.b)))
                1
            }),
    BITOR({ Statement it -> [it.c, '=', it.a, '|', it.b] },
            { Statement it, FloatBuffer f, IntBuffer i ->
                f.put(it.c, (float) (f.get(it.a) | f.get(it.b)))
                1
            })

    static int OFS_RETURN = 1;
    static int OFS_PARM0 = 4;
    static int OFS_PARM1 = 7;
    static int OFS_PARM2 = 10;
    static int OFS_PARM3 = 13;
    static int OFS_PARM4 = 16;
    static int OFS_PARM5 = 19;
    static int OFS_PARM6 = 22;
    static int OFS_PARM7 = 25;

    private Closure<List> stringify
    private Closure<Integer> action

    Instruction(Closure stringify, Closure<Integer> action) {
        this.stringify = stringify
        this.action = action
    }

    static Instruction from(short i) {
        values()[i]
    }

    int call(Statement s, Loader data) {
        action(s, data.globalFloatData, data.globalIntData)
    }

    String toString(Statement s, Loader data) {
        def template = stringify(s)
        int col = 9
        def each = template.collect {
            if (it instanceof Short) {
                def val
                try {
                    val = data.globalFloatData.get((int) it)
                } catch (ignored) {
                    val = '?'
                }
                return "\$$it ($val)"
            } else {
                return it
            }
        }
        "${this.name()}${' ' * (col - name().length())}\t(${each.join(' ')})"
    }
}
