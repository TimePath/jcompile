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
    fun visit(e: BinaryExpression.Add) = default(e)
    fun visit(e: BinaryExpression.AddAssign) = default(e)
    fun visit(e: BinaryExpression.And) = default(e)
    fun visit(e: BinaryExpression.AndAssign) = default(e)
    fun visit(e: BinaryExpression.Assign) = default(e)
    fun visit(e: BinaryExpression.BitAnd) = default(e)
    fun visit(e: BinaryExpression.BitOr) = default(e)
    fun visit(e: BinaryExpression.Comma) = default(e)
    fun visit(e: BinaryExpression.Divide) = default(e)
    fun visit(e: BinaryExpression.DivideAssign) = default(e)
    fun visit(e: BinaryExpression.Eq) = default(e)
    fun visit(e: BinaryExpression.ExclusiveOr) = default(e)
    fun visit(e: BinaryExpression.ExclusiveOrAssign) = default(e)
    fun visit(e: BinaryExpression.Ge) = default(e)
    fun visit(e: BinaryExpression.Gt) = default(e)
    fun visit(e: BinaryExpression.Le) = default(e)
    fun visit(e: BinaryExpression.Lsh) = default(e)
    fun visit(e: BinaryExpression.LshAssign) = default(e)
    fun visit(e: BinaryExpression.Lt) = default(e)
    fun visit(e: BinaryExpression.Modulo) = default(e)
    fun visit(e: BinaryExpression.ModuloAssign) = default(e)
    fun visit(e: BinaryExpression.Multiply) = default(e)
    fun visit(e: BinaryExpression.MultiplyAssign) = default(e)
    fun visit(e: BinaryExpression.Ne) = default(e)
    fun visit(e: BinaryExpression.Or) = default(e)
    fun visit(e: BinaryExpression.OrAssign) = default(e)
    fun visit(e: BinaryExpression.Rsh) = default(e)
    fun visit(e: BinaryExpression.RshAssign) = default(e)
    fun visit(e: BinaryExpression.Subtract) = default(e)
    fun visit(e: BinaryExpression.SubtractAssign) = default(e)
    fun visit(e: BlockExpression) = default(e)
    fun visit(e: BreakStatement) = default(e)
    fun visit(e: ConditionalExpression) = default(e)
    fun visit(e: ConstantExpression) = default(e)
    fun visit(e: ContinueStatement) = default(e)
    fun visit(e: DeclarationExpression) = default(e)
    fun visit(e: DynamicReferenceExpression) = default(e)
    fun visit(e: FunctionExpression) = default(e)
    fun visit(e: GotoExpression) = default(e)
    fun visit(e: IndexExpression) = default(e)
    fun visit(e: LabelExpression) = default(e)
    fun visit(e: LoopExpression) = default(e)
    fun visit(e: MemberExpression) = default(e)
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
    fun visit(e: UnaryExpression.Address) = default(e)
    fun visit(e: UnaryExpression.BitNot) = default(e)
    fun visit(e: UnaryExpression.Cast) = default(e)
    fun visit(e: UnaryExpression.Dereference) = default(e)
    fun visit(e: UnaryExpression.Minus) = default(e)
    fun visit(e: UnaryExpression.Not) = default(e)
    fun visit(e: UnaryExpression.Plus) = default(e)
    fun visit(e: UnaryExpression.Post) = default(e)
    fun visit(e: UnaryExpression.PostDecrement) = default(e)
    fun visit(e: UnaryExpression.PostIncrement) = default(e)
    fun visit(e: UnaryExpression.PreDecrement) = default(e)
    fun visit(e: UnaryExpression.PreIncrement) = default(e)
}
