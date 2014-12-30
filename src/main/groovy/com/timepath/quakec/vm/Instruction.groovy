package com.timepath.quakec.vm

import groovy.transform.CompileStatic

import java.nio.ByteBuffer

@CompileStatic
enum Instruction {
    DONE({ Statement it -> "DONE (\$${it.a}, \$${it.b}, \$${it.c})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(OFS_RETURN, data.getFloat(it.a))
                0
            }),

    MUL_FLO({ Statement it -> "MUL_F (\$${it.c} = \$${it.a} * \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (float) (data.getFloat(it.a) * data.getFloat(it.b)))
                1
            }),
    MUL_VEC({ Statement it -> "MUL_V (\$${it.c} = \$${it.a} * \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (float) (
                        data.getFloat(it.a + 0) * data.getFloat(it.b + 0)
                                + data.getFloat(it.a + 1) * data.getFloat(it.b + 1)
                                + data.getFloat(it.a + 2) * data.getFloat(it.b + 2)
                ))
                1
            }),

    MUL_FLO_VEC({ Statement it -> "MUL_FV (\$${it.c} = \$${it.a} * \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c + 0, (float) (data.getFloat(it.a) * data.getFloat(it.b + 0)))
                data.putFloat(it.c + 1, (float) (data.getFloat(it.a) * data.getFloat(it.b + 1)))
                data.putFloat(it.c + 2, (float) (data.getFloat(it.a) * data.getFloat(it.b + 2)))
                1
            }),
    MUL_VEC_FLO({ Statement it -> "MUL_VF (\$${it.c} = \$${it.a} * \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c + 0, (float) (data.getFloat(it.a + 0) * data.getFloat(it.b)))
                data.putFloat(it.c + 1, (float) (data.getFloat(it.a + 1) * data.getFloat(it.b)))
                data.putFloat(it.c + 2, (float) (data.getFloat(it.a + 2) * data.getFloat(it.b)))
                1
            }),

    DIV_FLO({ Statement it -> "DIV_F (\$${it.c} = \$${it.a} / \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (float) (data.getFloat(it.a) / data.getFloat(it.b)))
                1
            }),

    ADD_FLO({ Statement it -> "ADD_F (\$${it.c} = \$${it.a} + \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (float) (data.getFloat(it.a) + data.getFloat(it.b)))
                1
            }),
    ADD_VEC({ Statement it -> "ADD_V (\$${it.c} = \$${it.a} + \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c + 0, (float) (data.getFloat(it.a + 0) + data.getFloat(it.b + 0)))
                data.putFloat(it.c + 1, (float) (data.getFloat(it.a + 1) + data.getFloat(it.b + 1)))
                data.putFloat(it.c + 2, (float) (data.getFloat(it.a + 2) + data.getFloat(it.b + 2)))
                1
            }),

    SUB_FLO({ Statement it -> "SUB_F (\$${it.c} = \$${it.a} - \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (float) (data.getFloat(it.a) - data.getFloat(it.b)))
                1
            }),
    SUB_VEC({ Statement it -> "SUB_V (\$${it.c} = \$${it.a} - \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c + 0, (float) (data.getFloat(it.a + 0) - data.getFloat(it.b + 0)))
                data.putFloat(it.c + 1, (float) (data.getFloat(it.a + 1) - data.getFloat(it.b + 1)))
                data.putFloat(it.c + 2, (float) (data.getFloat(it.a + 2) - data.getFloat(it.b + 2)))
                1
            }),

    EQ_FLO({ Statement it -> "EQ_F (\$${it.c} = \$${it.a} == \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a) == data.getFloat(it.b) ? 1 : 0))
                1
            }),
    EQ_VEC({ Statement it -> "EQ_V (\$${it.c} = \$${it.a} == \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a + 0) == data.getFloat(it.b + 0)
                        && data.getFloat(it.a + 1) == data.getFloat(it.b + 1)
                        && data.getFloat(it.a + 2) == data.getFloat(it.b + 2)) ? 1 : 0)
                1
            }),
    EQ_STR({ Statement it -> "EQ_S (\$${it.c} = \$${it.a} == \$${it.b})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    EQ_ENT({ Statement it -> "EQ_E (\$${it.c} = \$${it.a} == \$${it.b})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    EQ_FNC({ Statement it -> "EQ_M (\$${it.c} = \$${it.a} == \$${it.b})" },
            { Statement it, ByteBuffer data ->
                1
            }),

    NE_FLO({ Statement it -> "NE_F (\$${it.c} = \$${it.a} != \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a) != data.getFloat(it.b) ? 1 : 0))
                1
            }),
    NE_VEC({ Statement it -> "NE_V (\$${it.c} = \$${it.a} != \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a + 0) != data.getFloat(it.b + 0)
                        || data.getFloat(it.a + 1) != data.getFloat(it.b + 1)
                        || data.getFloat(it.a + 2) != data.getFloat(it.b + 2)) ? 1 : 0)
                1
            }),
    NE_STR({ Statement it -> "NE_S (\$${it.c} = \$${it.a} != \$${it.b})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    NE_ENT({ Statement it -> "NE_E (\$${it.c} = \$${it.a} != \$${it.b})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    NE_FNC({ Statement it -> "NE_M (\$${it.c} = \$${it.a} != \$${it.b})" },
            { Statement it, ByteBuffer data ->
                1
            }),

    LE({ Statement it -> "LE (\$${it.c} = \$${it.a} <= \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a) <= data.getFloat(it.b) ? 1 : 0))
                1
            }),
    GE({ Statement it -> "GE (\$${it.c} = \$${it.a} => \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a) >= data.getFloat(it.b) ? 1 : 0))
                1
            }),
    LT({ Statement it -> "LT (\$${it.c} = \$${it.a} < \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a) < data.getFloat(it.b) ? 1 : 0))
                1
            }),
    GT({ Statement it -> "GT (\$${it.c} = \$${it.a} > \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a) > data.getFloat(it.b) ? 1 : 0))
                1
            }),

    LOAD_FLO({ Statement it -> "LOAD_F (\$${it.a}[\$${it.c}] = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    LOAD_VEC({ Statement it -> "LOAD_V (\$${it.a}[\$${it.c}] = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    LOAD_STR({ Statement it -> "LOAD_S (\$${it.a}[\$${it.c}] = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    LOAD_ENT({ Statement it -> "LOAD_E (\$${it.a}[\$${it.c}] = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    LOAD_FLD({ Statement it -> "LOAD_F (\$${it.a}[\$${it.c}] = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    LOAD_FNC({ Statement it -> "LOAD_M (\$${it.a}[\$${it.c}] = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),

    LOAD_ADDRESS({ Statement it -> "LOAD_ADDRESS (ILLEGAL)" },
            { Statement it, ByteBuffer data ->
                1
            }),

    STORE_FLO({ Statement it -> "STORE_F (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.b, data.getFloat(it.a))
                1
            }),
    STORE_VEC({ Statement it -> "STORE_V (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.b + 0, data.getFloat(it.a + 0))
                data.putFloat(it.b + 1, data.getFloat(it.a + 1))
                data.putFloat(it.b + 2, data.getFloat(it.a + 2))
                1
            }),
    STORE_STR({ Statement it -> "STORE_S (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.b, data.getFloat(it.a))
                1
            }),
    STORE_ENT({ Statement it -> "STORE_E (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.b, data.getFloat(it.a))
                1
            }),
    STORE_FLD({ Statement it -> "STORE_F (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.b, data.getFloat(it.a))
                1
            }),
    STORE_FNC({ Statement it -> "STORE_M (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.b, data.getFloat(it.a))
                1
            }),

    STOREP_FLO({ Statement it -> "STOREP_F (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    STOREP_VEC({ Statement it -> "STOREP_V (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    STOREP_STR({ Statement it -> "STOREP_S (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    STOREP_ENT({ Statement it -> "STOREP_E (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    STOREP_FLD({ Statement it -> "STOREP_F (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    STOREP_FNC({ Statement it -> "STOREP_M (\$${it.b} = \$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),

    RETURN({ Statement it -> "RETURN (\$${it.a}, \$${it.b}, \$${it.c})" },
            { Statement it, ByteBuffer data -> 0 }),

    NOT_FLO({ Statement it -> "NOT_F (!\$${it.a})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.b, !data.getFloat(it.a) ? 1 : 0)
                1
            }),
    NOT_VEC({ Statement it -> "NOT_V (!\$${it.a})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (!data.getFloat(it.a + 0)
                        && !data.getFloat(it.a + 1)
                        && !data.getFloat(it.a + 2)) ? 1 : 0)
                1
            }),
    NOT_STR({ Statement it -> "NOT_S (!\$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    NOT_ENT({ Statement it -> "NOT_E (!\$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),
    NOT_FNC({ Statement it -> "NOT_M (!\$${it.a})" },
            { Statement it, ByteBuffer data ->
                1
            }),

    IF({ Statement it -> "IF (if (\$${it.a}) then goto +\$${it.b})" },
            { Statement it, ByteBuffer data ->
                1 + (data.getFloat(it.a) ? (int) it.b : 0)
            }),
    IFNOT({ Statement it -> "IFNOT (if (!\$${it.a} then goto +\$${it.b})" },
            { Statement it, ByteBuffer data ->
                1 + (!data.getFloat(it.a) ? (int) it.b : 0)
            }),

    CALL0({ Statement it -> "CALL0 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),
    CALL1({ Statement it -> "CALL1 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),
    CALL2({ Statement it -> "CALL2 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),
    CALL3({ Statement it -> "CALL3 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),
    CALL4({ Statement it -> "CALL4 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),
    CALL5({ Statement it -> "CALL5 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),
    CALL6({ Statement it -> "CALL6 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),
    CALL7({ Statement it -> "CALL7 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),
    CALL8({ Statement it -> "CALL8 (\$${it.a}(...))" },
            { Statement it, ByteBuffer data ->
                1
            }),

    STATE({ Statement it -> "STATE (ILLEGAL)" },
            { Statement it, ByteBuffer data ->
                1
            }),

    GOTO({ Statement it -> "GOTO (goto +\$${it.a})" },
            { Statement it, ByteBuffer data ->
                1 + it.a
            }),

    AND({ Statement it -> "AND (\$${it.a} && \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a) && data.getFloat(it.b)) ? 1 : 0)
                1
            }),
    OR({ Statement it -> "OR (\$${it.a} || \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (data.getFloat(it.a) || data.getFloat(it.b)) ? 1 : 0)
                1
            }),

    BITAND({ Statement it -> "BITAND (\$${it.c} = \$${it.a} & \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (float) (data.getFloat(it.a) & data.getFloat(it.b)))
                1
            }),
    BITOR({ Statement it -> "BITOR (\$${it.c} = \$${it.a} | \$${it.b})" },
            { Statement it, ByteBuffer data ->
                data.putFloat(it.c, (float) (data.getFloat(it.a) | data.getFloat(it.b)))
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

    private Closure<String> stringify
    private Closure<Integer> action

    Instruction(Closure stringify, Closure<Integer> action) {
        this.stringify = stringify
        this.action = action
    }

    static Instruction from(short i) {
        values()[i]
    }

    int call(Statement s, Loader data) {
        action(s, data.globalData)
    }

    String toString(Statement s) {
        stringify(s)
    }
}
