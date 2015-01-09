package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.Value
import com.timepath.quakec.ast.Statement

class ConditionalExpression(val test: Expression,
                            val yes: Statement,
                            val no: Statement? = null) : Expression() {

    {
        mutableChildren.add(test)
        mutableChildren.add(yes)
        if (no != null) {
            mutableChildren.add(no)
        }
    }

    override fun evaluate(): Value? {
        val result = test.evaluate()
        if (result == null) return null
        val eval = @lambda {(it: Statement?): Value? ->
            return@lambda if (it is Expression) it.evaluate() else null
        }
        return if (result.toBoolean()) eval(yes) else eval(no)
    }

    override fun toString(): String = "($test ? $yes : $no)"

}
