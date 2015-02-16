package com.timepath.compiler.ast

import java.util.concurrent.atomic.AtomicInteger
import com.timepath.compiler.Type
import com.timepath.compiler.gen.CaseIR
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import org.antlr.v4.runtime.ParserRuleContext

class SwitchExpression(val test: Expression, add: List<Expression>, ctx: ParserRuleContext? = null) : Expression(ctx) {

    override fun type(gen: Generator) = test.type(gen)

    class object {
        val uid = AtomicInteger()
    }

    {
        addAll(add)
    }

    override fun reduce(): Expression {
        val jumps = linkedListOf<Expression>()
        val default = linkedListOf<Expression>()
        val cases = LoopExpression(checkBefore = false, predicate = ConstantExpression(0), body = BlockExpression(transform {
            when (it) {
                is SwitchExpression -> it.reduce()
                is Case -> {
                    val expr = it.expr
                    fun String.sanitizeLabel(): String = "__switch_${uid.getAndIncrement()}_${replaceAll("[^a-zA-Z_0-9]", "_")}"
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
        override fun type(gen: Generator) = expr?.type(gen) ?: Type.Void

        override val attributes: Map<String, Any?>
            get() = mapOf("id" to expr)

        override fun generate(gen: Generator): List<IR> {
            return listOf(CaseIR(expr))
        }
    }

}
