package com.timepath.compiler.frontend.quakec

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.use
import org.anarres.cpp.CppReader
import org.anarres.cpp.Preprocessor
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

public class QCC : Frontend<Q1VM.State> {

    companion object {
        val logger = Logger()
    }

    val cpp: Preprocessor = CustomPreprocessor()
    override fun define(name: String, value: String) = cpp.addMacro(name, value)

    override fun parse(includes: List<Compiler.Include>, state: Q1VM.State): Sequence<List<Expression>> {
        Executors.newSingleThreadExecutor().use {
            includes.map {
                it to submit(Callable {
                    cpp.addInput(it.source)
                    CppReader(cpp).useLines { it.join("\n") }
                })
            }
        }.let {
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).use {
                it.map {
                    val (include, future) = it
                    include to submit(Callable {
                        val stream = ANTLRInputStream(future.get())
                        stream.name = include.path
                        val lexer = CustomLexer(stream)
                        val parser = QCParser(lexer.let { CommonTokenStream(it) })
                        parser.compilationUnit()
                    })
                }
            }
        }.let {
            it.sequence().map {
                val (include, future) = it
                logger.info { include.path }
                val ret = future.get().accept(ASTTransform(state))
                ret.single().children
            }
        }.let {
            return it
        }
    }
}
