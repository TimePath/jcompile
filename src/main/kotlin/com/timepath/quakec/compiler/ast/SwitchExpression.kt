package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.CaseIR
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.LabelIR
import com.timepath.quakec.compiler.gen.ReferenceIR
import com.timepath.quakec.vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

class SwitchExpression(val test: Expression, add: List<Expression>, ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        addAll(add)
    }

    override fun generate(ctx: Generator): List<IR> {
        val ret = linkedListOf<IR>()
        val default = linkedListOf<IR>()
        val body = LoopExpression(ConstantExpression(0), BlockExpression(children))
        val children = body.doGenerate(ctx)
        val cases = children.map {
            when (it) {
                is CaseIR -> {
                    val expr = it.expr
                    val label = (if (expr == null) "default" else "case $expr").toString()
                    val goto = GotoExpression(label)
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
    class Case(
            /**
             * Case expression, null = default
             */
            val expr: Expression?,
            ctx: ParserRuleContext? = null) : Expression(ctx) {

        override val attributes: Map<String, Any?>
            get() = mapOf("id" to expr)

        override fun generate(ctx: Generator): List<IR> {
            return listOf(CaseIR(expr))
        }
    }

}
