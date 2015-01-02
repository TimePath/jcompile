package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression as rvalue
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.impl.ReferenceExpression as lvalue
import com.timepath.quakec.vm.Instruction
import groovy.transform.InheritConstructors
import groovy.transform.TupleConstructor

@TupleConstructor(excludes = 'op')
abstract class BinaryExpression<L extends rvalue, R extends rvalue> implements rvalue {

    L left
    R right

    @Override
    String getText() { "(${left.text} $op ${right.text})" }

    @Override
    def generate(GenerationContext ctx) {
        def genL = left.generate(ctx)
        def genR = right.generate(ctx)
        def allocate = ctx.allocate("${left.text} $op ${right.text}")
        [instr, genL, genR, allocate, allocate]
    }
    @Lazy
    abstract String op

    abstract Instruction getInstr()

    @InheritConstructors
    static class Assign extends BinaryExpression<lvalue, rvalue> {
        String op = '='

        Instruction getInstr() { Instruction.STORE_FLOAT }

        def generate(GenerationContext ctx) {
            // left = right
            def genL = left.generate(ctx)
            def genR = right.generate(ctx)
            def allocate = ctx.allocate("${left.text} $op ${right.text}")
            [instr, genR, genL, allocate]
        }
    }

    @InheritConstructors
    static class Add extends BinaryExpression<rvalue, rvalue> {
        String op = '+'

        Instruction getInstr() { Instruction.ADD_FLOAT }
    }

    @InheritConstructors
    static class Sub extends BinaryExpression<rvalue, rvalue> {
        String op = '-'

        Instruction getInstr() { Instruction.SUB_FLOAT }
    }

    @InheritConstructors
    static class Mul extends BinaryExpression<rvalue, rvalue> {
        String op = '*'

        Instruction getInstr() { Instruction.MUL_FLOAT }
    }

    @InheritConstructors
    static class Div extends BinaryExpression<rvalue, rvalue> {
        String op = '/'

        Instruction getInstr() { Instruction.DIV_FLOAT }
    }

    @InheritConstructors
    static class Eq extends BinaryExpression<rvalue, rvalue> {
        String op = '=='

        Instruction getInstr() { Instruction.EQ_FLOAT }
    }

    @InheritConstructors
    static class Ne extends BinaryExpression<rvalue, rvalue> {
        String op = '!='

        Instruction getInstr() { Instruction.NE_FLOAT }
    }

    @InheritConstructors
    static class Lt extends BinaryExpression<rvalue, rvalue> {
        String op = '<'

        Instruction getInstr() { Instruction.LT }
    }

    @InheritConstructors
    static class Le extends BinaryExpression<rvalue, rvalue> {
        String op = '<='

        Instruction getInstr() { Instruction.LE }
    }

    @InheritConstructors
    static class Gt extends BinaryExpression<rvalue, rvalue> {
        String op = '>'

        Instruction getInstr() { Instruction.GT }
    }

    @InheritConstructors
    static class Ge extends BinaryExpression<rvalue, rvalue> {
        String op = '>='

        Instruction getInstr() { Instruction.GE }
    }

    @InheritConstructors
    static class And extends BinaryExpression<rvalue, rvalue> {
        String op = '&&'

        Instruction getInstr() { Instruction.AND }
    }

    @InheritConstructors
    static class BitAnd extends BinaryExpression<rvalue, rvalue> {
        String op = '&'

        Instruction getInstr() { Instruction.BITAND }
    }

    @InheritConstructors
    static class Or extends BinaryExpression<rvalue, rvalue> {
        String op = '||'

        Instruction getInstr() { Instruction.OR }
    }

    @InheritConstructors
    static class BitOr extends BinaryExpression<rvalue, rvalue> {
        String op = '|'

        Instruction getInstr() { Instruction.BITOR }
    }

}
