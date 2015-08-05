package com.timepath.compiler.ast

import com.timepath.compiler.api.Named
import org.antlr.v4.runtime.ParserRuleContext as PRC

public abstract class Expression : Named {

    override fun toString() = simpleName

    abstract val ctx: PRC?

    abstract fun accept<T>(visitor: ASTVisitor<T>): T

    private val mutableChildren: MutableList<Expression> = linkedListOf()

    // FIXME: abstract
    open fun withChildren(children: List<Expression>): Expression = this // throw UnsupportedOperationException("${simpleName}: withChildren()")

    val children: List<Expression>
        get() = mutableChildren

    fun initChild<T : Expression>(elem: T, configure: (T.() -> Unit)? = null): T {
        if (configure != null)
            elem.configure()
        add(elem)
        return elem
    }

    fun add(s: Expression) {
        mutableChildren.add(s)
    }

    fun addAll(c: List<Expression>) {
        mutableChildren.addAll(c)
    }

}



