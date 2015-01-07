package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.Value
import com.timepath.quakec.ast.Statement

class ConditionalExpression(val test: Expression,
                            val yes: Expression,
                            val no: Expression) : Expression() {

    override val children: MutableList<Statement>
        get() = arrayListOf(test, yes, no)

    override fun evaluate(): Value? {
        val result = test.evaluate()
        if (result == null) return null
        return if (result.toBoolean()) yes.evaluate() else no.evaluate()
    }

}
