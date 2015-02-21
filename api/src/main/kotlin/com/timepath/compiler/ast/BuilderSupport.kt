package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import com.timepath.compiler.types.function_t

fun ast(configure: (BlockExpression.() -> Unit)? = null): BlockExpression {
    val block = BlockExpression()
    if (configure != null)
        block.configure()
    return block
}

fun BlockExpression.block(configure: (BlockExpression.() -> Unit)? = null): BlockExpression {
    return initChild(BlockExpression(), configure)
}

fun BlockExpression.const(value: Any): ConstantExpression {
    return ConstantExpression(value)
}

fun BlockExpression.def(name: String, any: Any): DeclarationExpression {
    return initChild(DeclarationExpression(name, Type.from(any), ConstantExpression(any)))
}

fun BlockExpression.ref(id: String): ReferenceExpression {
    return ReferenceExpression(id)
}

fun BlockExpression.func(returnType: function_t, name: String,
                         configure: (BlockExpression.() -> Unit)? = null): FunctionExpression {
    val functionLiteral = initChild(FunctionExpression(name, returnType))
    functionLiteral.initChild(BlockExpression(), configure)
    return functionLiteral
}

fun BlockExpression.ret(returnValue: Expression? = null): ReturnStatement {
    return initChild(ReturnStatement(returnValue))
}

fun BlockExpression.call(function: Expression, configure: (MethodCallExpression.() -> Unit)? = null): MethodCallExpression {
    return initChild(MethodCallExpression(function), configure)
}

fun MethodCallExpression.arg(expr: Expression): Expression {
    return initChild(expr)
}
