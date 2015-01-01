package com.timepath.quakec

import groovy.transform.CompileStatic
import org.antlr.v4.runtime.misc.NotNull

@CompileStatic
class ScopeCollector extends QCBaseListener {

    class Scope {

        class Declaration {

            String name

            Declaration(String name) {
                this.name = name
                println "${tab}defining ${name}"
            }

            def refine() {
                println "${tab}refine ${name}"
            }
        }

        Scope parent, child

        private Map<String, Declaration> declarationMap = [:]

        Scope(Scope parent) {
            this.parent = parent
            if (parent) parent.child = this
        }

        boolean warned

        void declare(String name) {
            def s = this
            for (; s; s = s.parent) {
                def declaration = s.declarationMap[name]
                if (declaration) {
                    declaration.refine()
                    return
                }
            }
            if (parent && declarationMap.size() > 10 && !warned) {
                println 'Scope size exceeds 10'
                warned = true
            }
            declarationMap[name] = new Declaration(name)
        }

    }

    String getTab() { '\t' * (depth - 1) }
    int depth

    Scope scope = push()

    Scope push() {
        depth++
        scope = new Scope(scope)
    }

    Scope pop() {
        depth--
        scope = scope.parent
    }

    @Override
    void enterCompoundStatement(@NotNull QCParser.CompoundStatementContext ctx) {
        push()
        def size = ctx.blockItemList().blockItem().size()
        def n = 100
        if (size > n) {
            println "Statement count exceeds $n ($size) in ${functionStack.peek()}"
        }
    }

    @Override
    void exitCompoundStatement(@NotNull QCParser.CompoundStatementContext ctx) {
        pop()
    }

    private Stack<String> functionStack = new Stack<>()

    @Override
    void enterFunctionDefinition(@NotNull QCParser.FunctionDefinitionContext ctx) {
        def text = ctx.declarator().text
        functionStack.push(text)
        scope.declare(text)
    }

    @Override
    void exitFunctionDefinition(@NotNull QCParser.FunctionDefinitionContext ctx) {
        functionStack.pop()
    }

    @Override
    void enterDeclaration(@NotNull QCParser.DeclarationContext ctx) {
        ctx.initDeclaratorList().initDeclarator().each {
            scope.declare(it.text)
        }
    }
}
