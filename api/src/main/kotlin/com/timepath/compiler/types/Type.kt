package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Named
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.Expression

public abstract class Type : Named {

    override fun toString() = javaClass.getSimpleName().toLowerCase()

    abstract fun declare(name: String, value: ConstantExpression? = null, state: CompileState): List<Expression>

    abstract fun handle(op: Operation): OperationHandler<*, *>?

}
