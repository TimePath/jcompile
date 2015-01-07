package com.timepath.quakec.ast

import java.util.ArrayList
import com.timepath.quakec.ast.impl.FunctionLiteral
import com.timepath.quakec.ast.impl.FunctionCall
import com.timepath.quakec.ast.impl.ReturnStatement
import com.timepath.quakec.ast.impl.ConstantExpression
import com.timepath.quakec.ast.impl.ReferenceExpression
import com.timepath.quakec.ast.impl.DeclarationExpression

abstract class Statement {

    open val attributes: Map<String, Any?>
        get() = mapOf()

    open val children: MutableList<Statement> = ArrayList()

    fun initChild<T : Statement>(elem: T, configure: (T.() -> Unit)? = null): T {
        if (configure != null)
            elem.configure()
        children.add(elem)
        return elem
    }

    private fun render(sb: StringBuilder, indent: String) {
        val name = this.javaClass.getSimpleName()
        if (children.isEmpty()) {
            sb.append("${indent}<${name}${renderAttributes()}/>\n")
        } else {
            sb.append("${indent}<$name${renderAttributes()}>\n")
            for (c in children) {
                c.render(sb, indent + "\t")
            }
            sb.append("${indent}</${name}>\n")
        }
    }

    private fun renderAttributes(): String? {
        val builder = StringBuilder()
        for ((k, v) in attributes) {
            builder.append(" ${k}=\"${v}\"")
        }
        return builder.toString()
    }

    fun toStringRecursive(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }

}

abstract class Expression : Statement() {
    /**
     * Used in constant folding
     *
     * @return A constant or null if it could change at runtime
     */
    open fun evaluate(): Value? = null

    /**
     * Used in constant folding
     *
     * @return true if constant folding is forbidden for this node (not descendants)
     */
    open fun hasSideEffects(): Boolean = false
}

class BlockStatement(vararg args: Statement) : Statement() {
    {
        children.addAll(args)
    }
}

fun ast(configure: (BlockStatement.() -> Unit)? = null): BlockStatement {
    val block = BlockStatement()
    if (configure != null)
        block.configure()
    return block
}

fun BlockStatement.block(configure: (BlockStatement.() -> Unit)? = null): BlockStatement {
    return initChild(BlockStatement(), configure)
}

fun BlockStatement.const(value: Any): ConstantExpression {
    return ConstantExpression(value)
}

fun BlockStatement.def(name: String): DeclarationExpression {
    return initChild(DeclarationExpression(name))
}

fun BlockStatement.ref(id: String): ReferenceExpression {
    return ReferenceExpression(id)
}

fun BlockStatement.func(returnType: Type, name: String, argTypes: Array<Type>,
                        configure: (BlockStatement.() -> Unit)? = null): FunctionLiteral {
    val functionLiteral = initChild(FunctionLiteral(name, returnType, argTypes))
    functionLiteral.initChild(BlockStatement(), configure)
    return functionLiteral
}

fun BlockStatement.ret(returnValue: Expression? = null): ReturnStatement {
    return initChild(ReturnStatement(returnValue))
}

fun BlockStatement.call(function: Expression, configure: (FunctionCall.() -> Unit)? = null): FunctionCall {
    return initChild(FunctionCall(function), configure)
}

fun FunctionCall.arg(expr: Expression): Expression {
    return initChild(expr)
}
