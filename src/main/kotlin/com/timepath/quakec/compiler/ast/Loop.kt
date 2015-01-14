package com.timepath.quakec.compiler.ast

class Loop(val predicate: Expression,
           body: Statement,
           val checkBefore: Boolean = true,
           val initializer: Statement? = null,
           val update: Statement? = null) : Statement() {
    {
        add(body)
    }

}
