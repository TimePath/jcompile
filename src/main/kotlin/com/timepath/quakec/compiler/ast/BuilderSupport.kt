package com.timepath.quakec.compiler.ast

fun ast(configure: (BlockStatement.() -> Unit)? = null): BlockStatement {
    val block = BlockStatement()
    if (configure != null)
        block.configure()
    return block
}

fun BlockStatement.block(configure: (BlockStatement.() -> Unit)? = null): BlockStatement {
    return initChild(BlockStatement(), configure)
}

fun BlockStatement.const(value: Any): ConstantExpression {
    return ConstantExpression(value)
}

fun BlockStatement.def(name: String, any: Any): DeclarationExpression {
    return initChild(DeclarationExpression(name, ConstantExpression(any)))
}

fun BlockStatement.ref(id: String): ReferenceExpression {
    return ReferenceExpression(id)
}

fun BlockStatement.func(returnType: Type, name: String, argTypes: Array<Type>,
                        configure: (BlockStatement.() -> Unit)? = null): FunctionLiteral {
    val functionLiteral = initChild(FunctionLiteral(name, returnType, argTypes))
    functionLiteral.initChild(BlockStatement(), configure)
    return functionLiteral
}

fun BlockStatement.ret(returnValue: Expression? = null): ReturnStatement {
    return initChild(ReturnStatement(returnValue))
}

fun BlockStatement.call(function: Expression, configure: (FunctionCall.() -> Unit)? = null): FunctionCall {
    return initChild(FunctionCall(function), configure)
}

fun FunctionCall.arg(expr: Expression): Expression {
    return initChild(expr)
}