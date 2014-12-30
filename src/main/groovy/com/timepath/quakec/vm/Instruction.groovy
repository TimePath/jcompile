package com.timepath.quakec.vm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
enum Instruction {
    DONE({ Statement stmt -> "DONE (${stmt.a}, ${stmt.b}, ${stmt.c})" }),

    MUL_F({ Statement stmt -> "MUL_F (${stmt.c} = ${stmt.a} * ${stmt.b})" }),
    MUL_V({ Statement stmt -> "MUL_V (${stmt.c} = ${stmt.a} * ${stmt.b})" }),

    MUL_FV({ Statement stmt -> "MUL_FV (${stmt.c} = ${stmt.a} * ${stmt.b})" }),
    MUL_VF({ Statement stmt -> "MUL_VF (${stmt.c} = ${stmt.a} * ${stmt.b})" }),

    DIV_F({ Statement stmt -> "DIV_F (${stmt.c} = ${stmt.a} / ${stmt.b})" }),

    ADD_F({ Statement stmt -> "ADD_F (${stmt.c} = ${stmt.a} + ${stmt.b})" }),
    ADD_V({ Statement stmt -> "ADD_V (${stmt.c} = ${stmt.a} + ${stmt.b})" }),

    SUB_F({ Statement stmt -> "SUB_F (${stmt.c} = ${stmt.a} - ${stmt.b})" }),
    SUB_V({ Statement stmt -> "SUB_V (${stmt.c} = ${stmt.a} - ${stmt.b})" }),

    EQ_F({ Statement stmt -> "EQ_F (${stmt.c} = ${stmt.a} == ${stmt.b})" }),
    EQ_V({ Statement stmt -> "EQ_V (${stmt.c} = ${stmt.a} == ${stmt.b})" }),
    EQ_S({ Statement stmt -> "EQ_S (${stmt.c} = ${stmt.a} == ${stmt.b})" }),
    EQ_E({ Statement stmt -> "EQ_E (${stmt.c} = ${stmt.a} == ${stmt.b})" }),
    EQ_M({ Statement stmt -> "EQ_M (${stmt.c} = ${stmt.a} == ${stmt.b})" }),

    NE_F({ Statement stmt -> "NE_F (${stmt.c} = ${stmt.a} != ${stmt.b})" }),
    NE_V({ Statement stmt -> "NE_V (${stmt.c} = ${stmt.a} != ${stmt.b})" }),
    NE_S({ Statement stmt -> "NE_S (${stmt.c} = ${stmt.a} != ${stmt.b})" }),
    NE_E({ Statement stmt -> "NE_E (${stmt.c} = ${stmt.a} != ${stmt.b})" }),
    NE_M({ Statement stmt -> "NE_M (${stmt.c} = ${stmt.a} != ${stmt.b})" }),

    LE({ Statement stmt -> "LE (${stmt.c} = ${stmt.a} <= ${stmt.b})" }),
    GE({ Statement stmt -> "GE (${stmt.c} = ${stmt.a} => ${stmt.b})" }),
    LT({ Statement stmt -> "LT (${stmt.c} = ${stmt.a} < ${stmt.b})" }),
    GT({ Statement stmt -> "GT (${stmt.c} = ${stmt.a} > ${stmt.b})" }),

    LOAD_F({ Statement stmt -> "LOAD_F (${stmt.a}[${stmt.c}] = ${stmt.a})" }),
    LOAD_V({ Statement stmt -> "LOAD_V (${stmt.a}[${stmt.c}] = ${stmt.a})" }),
    LOAD_S({ Statement stmt -> "LOAD_S (${stmt.a}[${stmt.c}] = ${stmt.a})" }),
    LOAD_E({ Statement stmt -> "LOAD_E (${stmt.a}[${stmt.c}] = ${stmt.a})" }),
    LOAD_FLD({ Statement stmt -> "LOAD_F (${stmt.a}[${stmt.c}] = ${stmt.a})" }),
    LOAD_M({ Statement stmt -> "LOAD_M (${stmt.a}[${stmt.c}] = ${stmt.a})" }),

    LOAD_ADDRESS({ Statement stmt -> "LOAD_ADDRESS (ILLEGAL)" }),

    STORE_F({ Statement stmt -> "STORE_F (${stmt.b} = ${stmt.a})" }),
    STORE_V({ Statement stmt -> "STORE_V (${stmt.b} = ${stmt.a})" }),
    STORE_S({ Statement stmt -> "STORE_S (${stmt.b} = ${stmt.a})" }),
    STORE_E({ Statement stmt -> "STORE_E (${stmt.b} = ${stmt.a})" }),
    STORE_FLD({ Statement stmt -> "STORE_F (${stmt.b} = ${stmt.a})" }),
    STORE_M({ Statement stmt -> "STORE_M (${stmt.b} = ${stmt.a})" }),

    STOREP_F({ Statement stmt -> "STOREP_F (${stmt.b} = ${stmt.a})" }),
    STOREP_V({ Statement stmt -> "STOREP_V (${stmt.b} = ${stmt.a})" }),
    STOREP_S({ Statement stmt -> "STOREP_S (${stmt.b} = ${stmt.a})" }),
    STOREP_E({ Statement stmt -> "STOREP_E (${stmt.b} = ${stmt.a})" }),
    STOREP_FLD({ Statement stmt -> "STOREP_F (${stmt.b} = ${stmt.a})" }),
    STOREP_M({ Statement stmt -> "STOREP_M (${stmt.b} = ${stmt.a})" }),

    RETURN({ Statement stmt -> "RETURN (${stmt.a}, ${stmt.b}, ${stmt.c})" }),

    NOT_F({ Statement stmt -> "NOT_F (!${stmt.a})" }),
    NOT_V({ Statement stmt -> "NOT_V (!${stmt.a})" }),
    NOT_S({ Statement stmt -> "NOT_S (!${stmt.a})" }),
    NOT_E({ Statement stmt -> "NOT_E (!${stmt.a})" }),
    NOT_M({ Statement stmt -> "NOT_M (!${stmt.a})" }),

    IF({ Statement stmt -> "IF (if (${stmt.a}) then goto +${stmt.b})" }),
    IFNOT({ Statement stmt -> "IFNOT (if (!${stmt.a} then goto +${stmt.b})" }),

    CALL0({ Statement stmt -> "CALL0 (${stmt.a}(...))" }),
    CALL1({ Statement stmt -> "CALL1 (${stmt.a}(...))" }),
    CALL2({ Statement stmt -> "CALL2 (${stmt.a}(...))" }),
    CALL3({ Statement stmt -> "CALL3 (${stmt.a}(...))" }),
    CALL4({ Statement stmt -> "CALL4 (${stmt.a}(...))" }),
    CALL5({ Statement stmt -> "CALL5 (${stmt.a}(...))" }),
    CALL6({ Statement stmt -> "CALL6 (${stmt.a}(...))" }),
    CALL7({ Statement stmt -> "CALL7 (${stmt.a}(...))" }),
    CALL8({ Statement stmt -> "CALL8 (${stmt.a}(...))" }),

    STATE({ Statement stmt -> "STATE (ILLEGAL)" }),

    GOTO({ Statement stmt -> "GOTO (goto +${stmt.a})" }),

    AND({ Statement stmt -> "AND (${stmt.a} && ${stmt.b})" }),
    OR({ Statement stmt -> "OR (${stmt.a} || ${stmt.b})" }),

    BITAND({ Statement stmt -> "BITAND (${stmt.c} = ${stmt.a} & ${stmt.b})" }),
    BITOR({ Statement stmt -> "BITOR (${stmt.c} = ${stmt.a} | ${stmt.b})" })

    private Closure action

    Instruction(Closure action) {
        this.action = action
    }

    @CompileDynamic
    static Instruction from(short i) {
        values()[i]
    }

    String call(Statement s) {
        def ret = action(s)
        println ret
        ret
    }
}
