package com.timepath.quakec

import java.util.LinkedHashMap
import java.util.Stack
import com.timepath.quakec.QCParser.CompoundStatementContext
import com.timepath.quakec.QCParser.FunctionDefinitionContext
import com.timepath.quakec.QCParser.DeclarationContext

class ScopeCollector : QCBaseListener() {

    inner class Scope(val parent: Scope?) {

        inner class Declaration(val name: String) {
            {
                println("${tab}defining $name")
            }

            fun refine() {
                println("${tab}refine $name")
            }
        }

        var child: Scope? = null
        {
            if (parent != null) parent.child = this
        }
        val declarationMap = LinkedHashMap<String, Declaration>()
        var warned: Boolean = false

        fun declare(name: String) {
            var s: Scope? = this
            while (s != null) {
                val declaration = s!!.declarationMap[name]
                if (declaration != null) {
                    declaration.refine()
                    return
                }
                s = s!!.parent
            }
            if (parent != null && declarationMap.size() > 10 && !warned) {
                println("Scope size exceeds 10")
                warned = true
            }
            declarationMap[name] = Declaration(name)
        }

    }

    val tab: String
        get() = "\t" * (depth - 1)
    var depth: Int = 0

    var scope: Scope? = push()

    fun push(): Scope {
        depth++
        val new = Scope(scope)
        scope = new
        return new
    }

    fun pop() {
        depth--
        scope = scope!!.parent
    }

    override fun enterCompoundStatement(ctx: CompoundStatementContext) {
        push()
        val size = ctx.blockItemList().blockItem().size()
        val n = 100
        if (size > n) {
            println("Statement count exceeds $n ($size) in ${functionStack.peek()}")
        }
    }

    override fun exitCompoundStatement(ctx: CompoundStatementContext) {
        pop()
    }

    val functionStack = Stack<String>()

    override fun enterFunctionDefinition(ctx: FunctionDefinitionContext) {
        val text = ctx.declarator().getText()
        functionStack.push(text)
        scope!!.declare(text)
    }

    override fun exitFunctionDefinition(ctx: FunctionDefinitionContext) {
        functionStack.pop()
    }

    override fun enterDeclaration(ctx: DeclarationContext) {
        ctx.initDeclaratorList().initDeclarator().forEach {
            scope!!.declare(it.getText())
        }
    }

}
