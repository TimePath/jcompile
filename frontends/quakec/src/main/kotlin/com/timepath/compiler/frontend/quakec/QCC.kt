package com.timepath.compiler.frontend.quakec

import com.timepath.compiler.Compiler
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import org.anarres.cpp.CppReader
import org.anarres.cpp.Preprocessor
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree

public class QCC : Frontend {
    val preprocessor: Preprocessor = CustomPreprocessor()
    override fun define(name: String, value: String) = preprocessor.addMacro(name, value)

    private fun parse(input: ANTLRInputStream): ParseTree {
        val lexer = QCLexer(input)
        val parser = QCParser(lexer.let { CommonTokenStream(it) })
        return parser.compilationUnit()
    }

    override fun parse(include: Compiler.Include, state: CompileState): Expression {
        preprocessor.addInput(include.source)
        val stream = ANTLRInputStream(CppReader(preprocessor))
        stream.name = include.path
        return parse(stream).accept(ASTTransform(state)).single()
    }
}
