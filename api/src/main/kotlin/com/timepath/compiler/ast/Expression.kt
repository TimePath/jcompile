package com.timepath.compiler.ast

import com.timepath.compiler.Named
import org.antlr.v4.runtime.ParserRuleContext

abstract class Expression : Named {

    abstract val ctx: ParserRuleContext?

    abstract fun <T> accept(visitor: ASTVisitor<T>): T

    fun transform(transform: (Expression) -> Expression?): List<Expression> {
        // TODO: pure
        val ret = mutableChildren
        val it = ret.listIterator()
        while (it.hasNext()) {
            val before = it.next()
            val after = transform(before)
            if (after == null) {
                it.remove()
            } else {
                after.transform(transform)
                it.set(after)
            }
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

/**
 * Lonely semicolon
 */
class Nop(override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "Nop"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
}

