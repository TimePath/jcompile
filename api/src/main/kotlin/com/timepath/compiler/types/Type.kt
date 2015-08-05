package com.timepath.compiler.types

import com.timepath.compiler.api.Named
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.ast.Expression

public abstract class Type : Named {

    override fun toString() = simpleName

    open fun declare(name: String, value: ConstantExpression?): Expression
            = DeclarationExpression(name, this, value)

    abstract fun handle(op: Operation): Operation.Handler<*, *>?

}
