package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval

fun main(args: Array<String>) {
    org.reflections.Reflections("com.timepath")
            .getSubTypesOf(Expression::class.java)
            .sortedBy { it.name }
            .forEach {
                println("fun visit(e: " + it.toString()
                        .replace("$", ".")
                        .replace("class com.timepath.compiler.ast.", "") + ") = default(e)")
            }
}

@Deprecated("", replaceWith = ReplaceWith("e.accept(this)"))
fun ASTVisitor<T>.visitReflective<T>(e: Expression): T {
    val method = javaClass.getMethod("visit", e.javaClass)
    try {
        val result = method(this, e)
        @Suppress("UNCHECKED_CAST")
        return result as T
    } catch (t: Throwable) {
        val rule: ParserRuleContext? = e.ctx
        if (rule != null) {
            val source = rule.start.tokenSource
            println("e: ${source.sourceName}:${source.line}:${source.charPositionInLine}")
            val charStream = rule.start.inputStream
            val interval = Interval.of(rule.start.startIndex, rule.stop.stopIndex)
            val text = charStream.getText(interval)
            println("e: $text")
        }
        throw t
    }
}

interface ASTVisitor<T> {
    val simpleName: String get() = this.javaClass.simpleName
    fun default(e: Expression): T = throw UnsupportedOperationException("$simpleName: $e")
    fun visit(e: AliasExpression) = visit(e as DeclarationExpression)
    fun visit(e: BinaryExpression) = default(e)
    fun visit(e: BinaryExpression.Add) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Add.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.And) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.BitAnd) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.BitAnd.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.BitOr) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.BitOr.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.BitXor) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.BitXor.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Comma) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Divide) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Divide.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Eq) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Ge) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Gt) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Le) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Lsh) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Lsh.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Lt) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Modulo) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Modulo.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Multiply) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Multiply.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Ne) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Or) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Rsh) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Rsh.Assign) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Subtract) = visit(e as BinaryExpression)
    fun visit(e: BinaryExpression.Subtract.Assign) = visit(e as BinaryExpression)
    fun visit(e: BlockExpression) = default(e)
    fun visit(e: BreakStatement) = default(e)
    fun visit(e: ConditionalExpression) = default(e)
    fun visit(e: ConstantExpression) = default(e)
    fun visit(e: ContinueStatement) = default(e)
    fun visit(e: DeclarationExpression) = default(e)
    fun visit(e: FunctionExpression) = default(e)
    fun visit(e: GotoExpression) = default(e)
    fun visit(e: IndexExpression) = visit(e as BinaryExpression)
    fun visit(e: LabelExpression) = default(e)
    fun visit(e: LoopExpression) = default(e)
    fun visit(e: MemberExpression) = visit(e as BinaryExpression)
    fun visit(e: MemberReferenceExpression) = default(e)
    fun visit(e: MemoryReference) = default(e)
    fun visit(e: MethodCallExpression) = default(e)
    fun visit(e: Nop) = default(e)
    fun visit(e: ParameterExpression) = visit(e as DeclarationExpression)
    fun visit(e: ReferenceExpression) = default(e)
    fun visit(e: ReturnStatement) = default(e)
    fun visit(e: SwitchExpression) = default(e)
    fun visit(e: SwitchExpression.Case) = default(e)
    fun visit(e: UnaryExpression) = default(e)
    fun visit(e: UnaryExpression.Address) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.BitNot) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.Cast) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.Dereference) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.Minus) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.Not) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.Plus) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.Post) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.PostDecrement) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.PostIncrement) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.PreDecrement) = visit(e as UnaryExpression)
    fun visit(e: UnaryExpression.PreIncrement) = visit(e as UnaryExpression)
}
