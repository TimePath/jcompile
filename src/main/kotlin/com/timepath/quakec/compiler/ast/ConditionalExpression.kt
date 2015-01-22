package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.compiler.gen.ReferenceIR
import com.timepath.quakec.compiler.Value

class ConditionalExpression(val test: Expression,
                            val expression: Boolean,
                            val pass: Expression,
                            val fail: Expression? = null,
                            ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        add(test)
        add(pass)
        if (fail != null) {
            add(fail)
        }
    }

    override fun evaluate(): Value? {
        val result = test.evaluate()
        if (result == null) return null
        val eval = @lambda {(it: Expression?): Value? ->
            return@lambda if (it is Expression) it.evaluate() else null
        }
        return if (result.toBoolean()) eval(pass) else eval(fail)
    }

    override fun toString(): String = "($test ? $pass : $fail)"

    override fun generate(ctx: Generator): List<IR> {
        val ret = linkedListOf<IR>()
        val genPred = test.doGenerate(ctx)
        ret.addAll(genPred)
        val genTrue = pass.doGenerate(ctx)
        val trueCount = genTrue.count { it.real }
        val genFalse = fail?.doGenerate(ctx)
        if (genFalse == null) {
            // No else, jump to the instruction after the body
            ret.add(IR(Instruction.IFNOT, array(genPred.last().ret, trueCount + 1, 0)))
            ret.addAll(genTrue)
        } else {
            val falseCount = genFalse.count { it.real }
            val temp = ctx.allocator.allocateReference()
            // The if body has a goto, include it in the count
            val jumpTrue = IR(Instruction.IFNOT, array(genPred.last().ret, (trueCount + 2) + 1, 0))
            ret.add(jumpTrue)
            // if
            ret.addAll(genTrue)
            if (genTrue.isNotEmpty())
                ret.add(IR(Instruction.STORE_FLOAT, array(genTrue.last().ret, temp.ref)))
            else
                jumpTrue.args[1]--
            // end if, jump to the instruction after the else
            val jumpFalse = IR(Instruction.GOTO, array(falseCount + 2, 0, 0))
            ret.add(jumpFalse)
            // else
            ret.addAll(genFalse)
            if (genFalse.isNotEmpty())
                ret.add(IR(Instruction.STORE_FLOAT, array(genFalse.last().ret, temp.ref)))
            else
                jumpFalse.args[1]--
            // return
            ret.add(ReferenceIR(temp.ref))
        }
        return ret
    }
}