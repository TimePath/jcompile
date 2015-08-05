package com.timepath.compiler.ast

import com.timepath.compiler.api.Named
import org.antlr.v4.runtime.ParserRuleContext as PRC

public abstract class Expression : Named {

    override fun toString() = simpleName

    abstract val ctx: PRC?

    abstract fun accept<T>(visitor: ASTVisitor<T>): T

    fun transform(transform: (Expression) -> List<Expression>?): List<Expression> {
        // TODO: pure
        val ret = mutableChildren
        val iter = ret.listIterator()
        while (iter.hasNext()) {
            val before = iter.next()
            val after = transform(before)
            iter.remove()
            after?.flatMap { it.transform(transform) }?.forEach { iter.add(it) }
        }
        return ret
    }

    private val mutableChildren: MutableList<Expression> = linkedListOf()

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



