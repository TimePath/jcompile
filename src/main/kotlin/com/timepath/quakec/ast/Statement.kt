package com.timepath.quakec.ast

import java.util.LinkedList
import java.util.ArrayList
import com.timepath.quakec.ast.impl.BlockStatement
import com.timepath.quakec.ast.impl.FunctionLiteral
import com.timepath.quakec.ast.impl.FunctionCall
import com.timepath.quakec.ast.impl.ReturnStatement
import com.timepath.quakec.ast.impl.ConstantExpression
import com.timepath.quakec.ast.impl.ReferenceExpression

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

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }

}

fun ast(configure: BlockStatement.() -> Unit): BlockStatement {
    val block = BlockStatement()
    block.configure()
    return block
}

fun BlockStatement.block(configure: (BlockStatement.() -> Unit)? = null): BlockStatement {
    return initChild(BlockStatement(), configure)
}

fun BlockStatement.const(value: Any): ConstantExpression {
    return ConstantExpression(value)
}

fun BlockStatement.ref(id: String): ReferenceExpression {
    return ReferenceExpression(id)
}

fun BlockStatement.func(returnType: Type, name: String, argTypes: Array<Type>,
                        configure: (BlockStatement.() -> Unit)? = null): FunctionLiteral {
    val functionLiteral = FunctionLiteral(name, returnType, argTypes)
    initChild(functionLiteral)
    val block = BlockStatement()
    functionLiteral.initChild(block, configure)
    functionLiteral.block = block
    return functionLiteral
}

fun BlockStatement.call(function: Expression, configure: (FunctionCall.() -> Unit)? = null): FunctionCall {
    return initChild(FunctionCall(function), configure)
}

fun FunctionCall.arg(expr: Expression): Expression {
    return initChild(expr)
}

fun BlockStatement.ret(returnValue: Expression? = null): ReturnStatement {
    return initChild(ReturnStatement(returnValue))
}
