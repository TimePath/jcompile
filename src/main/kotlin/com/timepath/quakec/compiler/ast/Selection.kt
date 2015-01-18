package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.CaseIR
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.LabelIR
import com.timepath.quakec.compiler.gen.ReferenceIR
import com.timepath.quakec.vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

class ConditionalExpression(val test: Expression,
                            val pass: Statement,
                            val fail: Statement? = null,
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
        val eval = @lambda {(it: Statement?): Value? ->
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
            ret.add(IR(Instruction.IFNOT, array(genPred.last().ret, (trueCount + 2) + 1, 0)))
            ret.addAll(genTrue)
            ret.add(IR(Instruction.STORE_FLOAT, array(genTrue.last().ret, temp.ref)))
            // end if, jump to the instruction after the else
            ret.add(IR(Instruction.GOTO, array(falseCount + 2, 0, 0)))
            // else
            ret.addAll(genFalse)
            ret.add(IR(Instruction.STORE_FLOAT, array(genTrue.last().ret, temp.ref)))
            ret.add(ReferenceIR(temp.ref))
        }
        return ret
    }
}

class SwitchStatement(val test: Expression, add: List<Statement>, ctx: ParserRuleContext? = null) : Statement(ctx) {

    {
        addAll(add)
    }

    override fun generate(ctx: Generator): List<IR> {
        val ret = linkedListOf<IR>()
        val default = linkedListOf<IR>()
        val body = Loop(ConstantExpression(0), BlockStatement(children))
        val children = body.doGenerate(ctx)
        val cases = children.map {
            when (it) {
                is CaseIR -> {
                    val expr = it.expr
                    val label = (if (expr == null) "default" else "case $expr").toString()
                    val goto = GotoStatement(label)
                    if (expr == null) {
                        default.addAll(goto.doGenerate(ctx))
                    } else {
                        val test = BinaryExpression.Eq(test, expr)
                        val jump = ConditionalExpression(test, goto)
                        ret.addAll(jump.doGenerate(ctx))
                    }
                    LabelIR(label) // replace with a label so goto will be filled in later
                }
                else -> it
            }
        }
        ret.addAll(default)
        ret.addAll(cases)
        return ret
    }
}