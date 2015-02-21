package com.timepath.compiler.gen

import com.timepath.compiler.Value
import com.timepath.compiler.ast.*

/**
 * Used in constant folding
 *
 * @return A constant or null if it could change at runtime
 */
fun Expression.evaluate(): Value? = accept(EvaluateVisitor)

object EvaluateVisitor : ASTVisitor<Value?> {

    [suppress("NOTHING_TO_INLINE")]
    inline fun Expression.evaluate(): Value? = accept(this@EvaluateVisitor)

    override fun visit(e: BinaryExpression.Add): Value? = e.left.evaluate()?.plus(e.right.evaluate())

    override fun visit(e: BinaryExpression.Divide): Value? = e.left.evaluate()?.div(e.right.evaluate())

    override fun visit(e: BinaryExpression.Modulo): Value? = e.left.evaluate()?.mod(e.right.evaluate())

    override fun visit(e: BinaryExpression.Multiply): Value? = e.left.evaluate()?.times(e.right.evaluate())

    override fun visit(e: BinaryExpression.Subtract): Value? = e.left.evaluate()?.minus(e.right.evaluate())

    override fun visit(e: ConditionalExpression): Value? {
        val result = e.test.evaluate()
        if (result == null) return null
        val eval = @lambda {(it: Expression?): Value? ->
            return@lambda if (it is Expression) it.evaluate() else null
        }
        return if (result.toBoolean()) eval(e.pass) else eval(e.fail)
    }

    override fun visit(e: ConstantExpression): Value? = e.value

    override fun visit(e: UnaryExpression.Cast): Value? = e.operand.evaluate()

}
