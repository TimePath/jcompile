package com.timepath.compiler.backend.q1vm.gen

import com.timepath.compiler.ast.*
import com.timepath.compiler.ast.SwitchExpression.Case
import java.util.concurrent.atomic.AtomicInteger

public fun Expression.reduce(): Expression? = accept(ReduceVisitor)

object ReduceVisitor : ASTVisitor<Expression?> {

    fun Expression.reduce(): Expression? = accept(this@ReduceVisitor)

    override fun default(e: Expression) = e

    val uid = AtomicInteger()

    override fun visit(e: SwitchExpression): Expression? {
        with(e) {
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
            return BlockExpression(jumps + default + listOf(cases))
        }
    }

    override fun visit(e: UnaryExpression.Cast) = e.operand.reduce()
}
