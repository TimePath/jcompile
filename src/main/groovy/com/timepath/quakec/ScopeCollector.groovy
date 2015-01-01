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

        void declare(String name) {
            def s = this
            for (; s; s = s.parent) {
                def declaration = s.declarationMap[name]
                if (declaration) {
                    declaration.refine()
                    return
                }
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
    }

    @Override
    void exitCompoundStatement(@NotNull QCParser.CompoundStatementContext ctx) {
        pop()
    }

    @Override
    void enterFunctionDefinition(@NotNull QCParser.FunctionDefinitionContext ctx) {
        scope.declare(ctx.declarator().text)
    }

    @Override
    void enterDeclaration(@NotNull QCParser.DeclarationContext ctx) {
        ctx.initDeclaratorList().initDeclarator().each {
            scope.declare(it.text)
        }
    }
}
