package com.timepath.compiler.ast

fun main(args: Array<String>) {
    org.reflections.Reflections("com.timepath")
            .getSubTypesOf(javaClass<Expression>())
            .sortBy { it.getName() }
            .forEach {
                println("fun visit(e: " + it.toString()
                        .replace("$", ".")
                        .replace("class com.timepath.compiler.ast.", "") + "): T")
            }
}

fun <T> ASTVisitor<T>.visit(e: Expression): T {
    val method = javaClass.getMethod("visit", e.javaClass)
    try {
        val result = method.invoke(this, e)
        [suppress("UNCHECKED_CAST")]
        return result as T
    } catch (t: Throwable) {
        throw t
    }
}

trait ASTVisitor<T> {
    fun visit(e: BinaryExpression): T
    fun visit(e: BinaryExpression.Add): T
    fun visit(e: BinaryExpression.AddAssign): T
    fun visit(e: BinaryExpression.And): T
    fun visit(e: BinaryExpression.AndAssign): T
    fun visit(e: BinaryExpression.Assign): T
    fun visit(e: BinaryExpression.BitAnd): T
    fun visit(e: BinaryExpression.BitOr): T
    fun visit(e: BinaryExpression.Comma): T
    fun visit(e: BinaryExpression.Divide): T
    fun visit(e: BinaryExpression.DivideAssign): T
    fun visit(e: BinaryExpression.Eq): T
    fun visit(e: BinaryExpression.ExclusiveOr): T
    fun visit(e: BinaryExpression.ExclusiveOrAssign): T
    fun visit(e: BinaryExpression.Ge): T
    fun visit(e: BinaryExpression.Gt): T
    fun visit(e: BinaryExpression.Le): T
    fun visit(e: BinaryExpression.Lsh): T
    fun visit(e: BinaryExpression.LshAssign): T
    fun visit(e: BinaryExpression.Lt): T
    fun visit(e: BinaryExpression.Modulo): T
    fun visit(e: BinaryExpression.ModuloAssign): T
    fun visit(e: BinaryExpression.Multiply): T
    fun visit(e: BinaryExpression.MultiplyAssign): T
    fun visit(e: BinaryExpression.Ne): T
    fun visit(e: BinaryExpression.Or): T
    fun visit(e: BinaryExpression.OrAssign): T
    fun visit(e: BinaryExpression.Rsh): T
    fun visit(e: BinaryExpression.RshAssign): T
    fun visit(e: BinaryExpression.Subtract): T
    fun visit(e: BinaryExpression.SubtractAssign): T
    fun visit(e: BlockExpression): T
    fun visit(e: BreakStatement): T
    fun visit(e: ConditionalExpression): T
    fun visit(e: ConstantExpression): T
    fun visit(e: ContinueStatement): T
    fun visit(e: DeclarationExpression): T
    fun visit(e: FunctionExpression): T
    fun visit(e: GotoExpression): T
    fun visit(e: IndexExpression): T
    fun visit(e: LabelExpression): T
    fun visit(e: LoopExpression): T
    fun visit(e: MemberExpression): T
    fun visit(e: MemoryReference): T
    fun visit(e: MethodCallExpression): T
    fun visit(e: Nop): T
    fun visit(e: ParameterExpression): T
    fun visit(e: ReferenceExpression): T
    fun visit(e: ReturnStatement): T
    fun visit(e: StructDeclarationExpression): T
    fun visit(e: SwitchExpression): T
    fun visit(e: SwitchExpression.Case): T
    fun visit(e: UnaryExpression): T
    fun visit(e: UnaryExpression.Address): T
    fun visit(e: UnaryExpression.BitNot): T
    fun visit(e: UnaryExpression.Cast): T
    fun visit(e: UnaryExpression.Dereference): T
    fun visit(e: UnaryExpression.Minus): T
    fun visit(e: UnaryExpression.Not): T
    fun visit(e: UnaryExpression.Plus): T
    fun visit(e: UnaryExpression.Post): T
    fun visit(e: UnaryExpression.PostDecrement): T
    fun visit(e: UnaryExpression.PostIncrement): T
    fun visit(e: UnaryExpression.PreDecrement): T
    fun visit(e: UnaryExpression.PreIncrement): T
}
