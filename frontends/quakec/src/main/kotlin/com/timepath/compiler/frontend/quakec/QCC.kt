package com.timepath.compiler.frontend.quakec

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.tree.ParseTree

object QCC : Frontend {
    public val rules: Array<String> = QCParser.ruleNames

    fun tree(input: ANTLRInputStream): ParseTree {
        val lexer = QCLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = QCParser(tokens)
        parser.getInterpreter().setPredictionMode(PredictionMode.LL)
        return parser.compilationUnit()
    }

    override fun parse(stream: ANTLRInputStream, state: CompileState): Expression {
        return tree(stream).accept(ASTTransform(state)).single()
    }
}
