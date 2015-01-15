package com.timepath.quakec.compiler.ast

class Loop(val predicate: Expression,
           body: Statement,
           val checkBefore: Boolean = true,
           val initializer: List<Statement>? = null,
           val update:  List<Statement>? = null) : Statement() {
    {
        add(body)
    }

}
