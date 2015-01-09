package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import kotlin.properties.Delegates
import com.timepath.quakec.ast.Statement

class FunctionCall(val function: Expression? = null, newChildren: List<Statement> = emptyList()) : Expression() {

    {
        mutableChildren.addAll(newChildren)
    }

    val args: List<Expression> by Delegates.lazy {
        children.filterIsInstance<Expression>()
    }

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to function)

}
