package com.timepath.compiler.frontend.quakec

import com.timepath.compiler.Compiler
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import org.anarres.cpp.CppReader
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.tree.ParseTree

class QCC() : Frontend {
    val preprocessor = CustomPreprocessor()
    override fun define(name: String, value: String) {
        preprocessor.addMacro(name, value)
    }

    public val rules: Array<String> = QCParser.ruleNames

    fun tree(input: ANTLRInputStream): ParseTree {
        val lexer = QCLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = QCParser(tokens)
        parser.getInterpreter().setPredictionMode(PredictionMode.LL)
        return parser.compilationUnit()
    }

    override fun parse(include: Compiler.Include, state: CompileState): Expression {
        preprocessor.addInput(include.source)
        val stream = ANTLRInputStream(CppReader(preprocessor))
        stream.name = include.path
        return tree(stream).accept(ASTTransform(state)).single()
    }
}
