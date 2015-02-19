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
    override fun visit(e: BinaryExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Add): Value? = e.left.evaluate()?.plus(e.right.evaluate())

    override fun visit(e: BinaryExpression.AddAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.And): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.AndAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Assign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.BitAnd): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.BitOr): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Comma): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Divide): Value? = e.left.evaluate()?.div(e.right.evaluate())

    override fun visit(e: BinaryExpression.DivideAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Eq): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.ExclusiveOr): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.ExclusiveOrAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Ge): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Gt): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Le): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Lsh): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.LshAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Lt): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Modulo): Value? = e.left.evaluate()?.mod(e.right.evaluate())

    override fun visit(e: BinaryExpression.ModuloAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Multiply): Value? = e.left.evaluate()?.times(e.right.evaluate())

    override fun visit(e: BinaryExpression.MultiplyAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Ne): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Or): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.OrAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Rsh): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.RshAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BinaryExpression.Subtract): Value? = e.left.evaluate()?.minus(e.right.evaluate())

    override fun visit(e: BinaryExpression.SubtractAssign): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BlockExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: BreakStatement): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: ConditionalExpression): Value? {
        val result = e.test.evaluate()
        if (result == null) return null
        val eval = @lambda {(it: Expression?): Value? ->
            return@lambda if (it is Expression) it.evaluate() else null
        }
        return if (result.toBoolean()) eval(e.pass) else eval(e.fail)
    }

    override fun visit(e: ConstantExpression): Value? = e.value

    override fun visit(e: ContinueStatement): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: DeclarationExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: FunctionExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: GotoExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: IndexExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: LabelExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: LoopExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: MemberExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: MemoryReference): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: MethodCallExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: Nop): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: ParameterExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: ReferenceExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: ReturnStatement): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: StructDeclarationExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: SwitchExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: SwitchExpression.Case): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.Address): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.BitNot): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.Cast): Value? = e.operand.evaluate()

    override fun visit(e: UnaryExpression.Dereference): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.Minus): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.Not): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.Plus): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.Post): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.PostDecrement): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.PostIncrement): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.PreDecrement): Value? {
        throw UnsupportedOperationException()
    }

    override fun visit(e: UnaryExpression.PreIncrement): Value? {
        throw UnsupportedOperationException()
    }

}
