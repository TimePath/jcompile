package com.timepath.compiler.frontend.quakec

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.Q1VM
import org.anarres.cpp.CppReader
import org.anarres.cpp.Preprocessor
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

public class QCC : Frontend {

    companion object {
        val logger = Logger.new()
    }

    val cpp: Preprocessor = CustomPreprocessor()
    override fun define(name: String, value: String) = cpp.addMacro(name, value)

    inline fun ExecutorService.use<T>(body: ExecutorService.() -> T): T {
        val ret = body()
        this.shutdown()
        return ret
    }

    override fun parse(includes: List<Compiler.Include>, state: CompileState): List<List<Expression>> {
        val pre = Executors.newSingleThreadExecutor().use {
            includes.map {
                it to submit(Callable {
                    cpp.addInput(it.source)
                    StringBuilder { CppReader(cpp).forEachLine { appendln(it) } }.toString()
                })
            }
        }
        val post = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).use {
            pre.map { pair ->
                val (it, future) = pair
                it to submit(Callable {
                    val stream = ANTLRInputStream(future.get())
                    stream.name = it.path
                    val lexer = QCLexer(stream)
                    val parser = QCParser(lexer.let { CommonTokenStream(it) })
                    parser.compilationUnit()
                })
            }
        }
        val qs = state as Q1VM.State
        return post.map { pair ->
            val (it, future) = pair
            logger.info(it.path)
            val ret = future.get().accept(ASTTransform(qs))
            ret.single().children
        }
    }
}
