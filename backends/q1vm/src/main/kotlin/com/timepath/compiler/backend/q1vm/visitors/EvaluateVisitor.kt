package com.timepath.compiler.backend.q1vm.visitors

import com.timepath.compiler.Value
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM

/**
 * Used in constant folding
 *
 * @return A constant or null if it could change at runtime
 */
fun Expression.evaluate(state: CompileState) = accept(EvaluateVisitor(state as Q1VM.State))

private class EvaluateVisitor(val state: Q1VM.State) : ASTVisitor<Value?> {

    fun Expression.evaluate() = accept(this@EvaluateVisitor)

    override fun default(e: Expression) = null

    inline fun eval(e: BinaryExpression, action: (l: Value, r: Value) -> Value): Value? {
        val l = e.left.evaluate()
        val r = e.right.evaluate()
        return when {
            l == null || r == null -> null
            else -> action(l, r)
        }
    }

    override fun visit(e: BinaryExpression.Add) = eval(e) { l, r -> l + r }
    override fun visit(e: BinaryExpression.Divide) = eval(e) { l, r -> l / r }
    override fun visit(e: BinaryExpression.Modulo) = eval(e) { l, r -> l % r }
    override fun visit(e: BinaryExpression.Multiply) = eval(e) { l, r -> l * r }
    override fun visit(e: BinaryExpression.Subtract) = eval(e) { l, r -> l - r }

    inline fun eval(e: UnaryExpression, action: (v: Value) -> Value): Value? {
        val v = e.operand.evaluate()
        return when (v) {
            null -> null
            else -> action(v)
        }
    }

    override fun visit(e: UnaryExpression.Cast) = eval(e) { it.cast(e.type) }

    override fun visit(e: UnaryExpression.Minus) = eval(e) { -it }

    override fun visit(e: ConditionalExpression) = e.test.evaluate()?.let {
        when {
            it.toBoolean() -> e.pass.evaluate()
            else -> e.fail?.evaluate()
        }
    }

    override fun visit(e: ConstantExpression) = e.value

    override fun visit(e: MemberReferenceExpression) = state.fields[e.id].evaluate() // TODO: other types

    override fun visit(e: ReferenceExpression) = e.refers.evaluate()

}
