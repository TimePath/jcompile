package com.timepath.compiler.frontend.quakec

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.types.field_t
import com.timepath.compiler.backend.q1vm.types.void_t
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import java.util.ArrayList
import java.util.Collections

class ASTBuilder(val state: Q1VM.State) : NewQCBaseVisitor<List<Expression>>() {
    companion object {
        fun invoke(compilationUnit: NewQCParser.CompilationUnitContext, state: Q1VM.State) = compilationUnit.accept(ASTBuilder(state))

        public inline fun <T : Any, R : Any> Iterable<T?>.collect(transform: (T) -> Iterable<R>?): List<R> {
            val destination = ArrayList<R>()
            forEach {
                it?.let {
                    transform(it)?.let {
                        destination.addAll(it)
                    }
                }
            }
            return destination
        }

        fun Expression.list() = Collections.singletonList(this)
    }

    fun error(msg: String): Nothing {
        throw UnsupportedOperationException(msg)
    }

    override fun visitCompilationUnit(ctx: NewQCParser.CompilationUnitContext): List<Expression> {
        return ctx.scopeGlobal().collect { it.accept(this) }
    }

    override fun visitScopeGlobal(ctx: NewQCParser.ScopeGlobalContext): List<Expression> {
        return ctx.children.collect { it.accept(this) }
    }

    fun NewQCParser.Type_Context.toType(): Type {
        val self = this
        self.typeName()?.let {
            val s = it.getText()
            state.types[s]?.let { return it }
            error("Undefined type: $s")
        }
        self.functypeParams()?.let {
            return function_t(self.type_().toType(), it.functypeParam().map { it.type().type_().toType() })
        }
        self.typePtr()?.let {
            val n = it.getText().length()
            return sequence(self.type_().toType()) { field_t(it) }.take(1 + n).last()
        }
        throw NoWhenBranchMatchedException()
    }

    override fun visitDeclFunc(ctx: NewQCParser.DeclFuncContext): List<Expression> {
        val id = ctx.id().getText()
        val ret = ctx.type().type_().toType()
        val type: function_t = ctx.functypeParams()?.let {
            val params = it.functypeParam().map { it.type().type_().toType() }
            function_t(ret, params)
        } ?: run { // legacy
            if (ret is field_t) {
                val func = ret.type as function_t
                function_t(field_t(func.type), func.argTypes)
            } else {
                ret as function_t
            }
        }
        val body = ctx.block()?.accept(this)
        return FunctionExpression(id, type, add = body).list()
    }

    override fun visitDeclVar(ctx: NewQCParser.DeclVarContext): List<Expression> {
        return DeclarationExpression("null", void_t).list()
    }

    override fun visitBlock(ctx: NewQCParser.BlockContext): List<Expression> {
        return ctx.scopeBlock().collect { it.accept(this) } let { BlockExpression(it).list() }
    }

    override fun visitStmt(ctx: NewQCParser.StmtContext): List<Expression> {
        return Nop().list()
    }
}
