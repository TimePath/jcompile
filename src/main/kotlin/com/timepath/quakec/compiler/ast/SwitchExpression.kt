package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.CaseIR
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import org.antlr.v4.runtime.ParserRuleContext

class SwitchExpression(val test: Expression, add: List<Expression>, ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        addAll(add)
    }

    override fun generate(ctx: Generator): List<IR> {
        return reduce().doGenerate(ctx)
    }

    override fun reduce(): Expression {
        val jumps = linkedListOf<Expression>()
        val default = linkedListOf<Expression>()
        val cases = LoopExpression(checkBefore = false, predicate = ConstantExpression(0), body = BlockExpression(transform {
            when (it) {
                is SwitchExpression -> it.reduce()
                is Case -> {
                    val expr = it.expr
                    fun String.sanitizeLabel(): String = "__switch_${replaceAll("[^a-zA-Z_0-9]", "_")}"
                    val label = (if (expr == null) "default" else "case $expr").sanitizeLabel()
                    val goto = GotoExpression(label)
                    if (expr == null) {
                        default.add(goto)
                    } else {
                        val test = BinaryExpression.Eq(test, expr)
                        val jump = ConditionalExpression(test, false, goto)
                        jumps.add(jump)
                    }
                    LabelExpression(label) // replace with a label so goto will be filled in later
                }
                else -> it
            }
        }))
        return BlockExpression(jumps + default + listOf(cases));
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
