package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.Value

class ConditionalExpression(val test: Expression,
                            val yes: Expression,
                            val no: Expression) : Expression() {

    override fun evaluate(): Value? {
        val result = test.evaluate()
        if (result == null) return null
        return if (result.toBoolean()) yes.evaluate() else no.evaluate()
    }

    override val text: String
        get() = "(${test.text} ? ${yes.text} : ${no.text})"
}
