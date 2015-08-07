package com.timepath.compiler.types

import com.timepath.compiler.api.Named
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression

public abstract class Type : Named {

    override fun toString() = simpleName

    open fun declare(name: String, value: ConstantExpression?)
            = DeclarationExpression(name, this, value)

    abstract fun handle(op: Operation): Operation.Handler<*, *>?

}
