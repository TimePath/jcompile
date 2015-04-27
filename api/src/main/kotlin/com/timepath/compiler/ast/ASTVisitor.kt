package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval

fun main(args: Array<String>) {
    org.reflections.Reflections("com.timepath")
            .getSubTypesOf(javaClass<Expression>())
            .sortBy { it.getName() }
            .forEach {
                println("fun visit(e: " + it.toString()
                        .replace("$", ".")
                        .replace("class com.timepath.compiler.ast.", "") + ") = default(e)")
            }
}

/**
 * Prefer Expression.accept(this)
 */
fun ASTVisitor<T>.visitReflective<T>(e: Expression): T {
    val method = javaClass.getMethod("visit", e.javaClass)
    try {
        val result = method.invoke(this, e)
        [suppress("UNCHECKED_CAST")]
        return result as T
    } catch (t: Throwable) {
        val rule: ParserRuleContext? = e.ctx
        if (rule != null) {
            val source = rule.start.getTokenSource()
            println("e: ${source.getSourceName()}:${source.getLine()}:${source.getCharPositionInLine()}")
            val charStream = rule.start.getInputStream()
            val interval = Interval.of(rule.start.getStartIndex(), rule.stop.getStopIndex())
            val text = charStream.getText(interval)
            println("e: ${text}")
        }
        throw t
    }
}

trait ASTVisitor<T> {
    fun default(e: Expression): T = throw UnsupportedOperationException()
    fun visit(e: BinaryExpression) = default(e)
    fun visit(e: BinaryExpression.Add) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.AddAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.And) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.AndAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Assign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.BitAnd) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.BitOr) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Comma) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Divide) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.DivideAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Eq) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.ExclusiveOr) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.ExclusiveOrAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Ge) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Gt) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Le) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Lsh) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.LshAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Lt) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Modulo) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.ModuloAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Multiply) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.MultiplyAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Ne) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Or) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.OrAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Rsh) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.RshAssign) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.Subtract) = visit(e : BinaryExpression)
    fun visit(e: BinaryExpression.SubtractAssign) = visit(e : BinaryExpression)
    fun visit(e: BlockExpression) = default(e)
    fun visit(e: BreakStatement) = default(e)
    fun visit(e: ConditionalExpression) = default(e)
    fun visit(e: ConstantExpression) = default(e)
    fun visit(e: ContinueStatement) = default(e)
    fun visit(e: DeclarationExpression) = default(e)
    fun visit(e: DynamicReferenceExpression) = default(e)
    fun visit(e: FunctionExpression) = default(e)
    fun visit(e: GotoExpression) = default(e)
    fun visit(e: IndexExpression) = visit(e : BinaryExpression)
    fun visit(e: LabelExpression) = default(e)
    fun visit(e: LoopExpression) = default(e)
    fun visit(e: MemberExpression) = visit(e : BinaryExpression)
    fun visit(e: MemberReferenceExpression) = default(e)
    fun visit(e: MemoryReference) = default(e)
    fun visit(e: MethodCallExpression) = default(e)
    fun visit(e: Nop) = default(e)
    fun visit(e: ParameterExpression) = default(e)
    fun visit(e: ReferenceExpression) = default(e)
    fun visit(e: ReturnStatement) = default(e)
    fun visit(e: StructDeclarationExpression) = default(e)
    fun visit(e: SwitchExpression) = default(e)
    fun visit(e: SwitchExpression.Case) = default(e)
    fun visit(e: UnaryExpression) = default(e)
    fun visit(e: UnaryExpression.Address) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.BitNot) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.Cast) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.Dereference) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.Minus) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.Not) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.Plus) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.Post) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.PostDecrement) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.PostIncrement) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.PreDecrement) = visit(e : UnaryExpression)
    fun visit(e: UnaryExpression.PreIncrement) = visit(e : UnaryExpression)
}
