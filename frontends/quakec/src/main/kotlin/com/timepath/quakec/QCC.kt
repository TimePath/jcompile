package com.timepath.quakec

import com.timepath.quakec.compiler.api.Frontend
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import com.timepath.quakec.compiler.ast.Expression
import com.timepath.quakec.compiler.ASTTransform
import com.timepath.quakec.compiler.TypeRegistry

object QCC : Frontend {
    var rules: List<String>? = null

    fun tree(input: ANTLRInputStream): ParseTree {
        val lexer = QCLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = QCParser(tokens)
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL)
        val tree = try {
            parser.compilationUnit() // STAGE 1
        } catch (ignored: Exception) {
            // rewind input stream
            lexer.reset()
            tokens.reset()
            parser.reset()
            parser.getInterpreter().setPredictionMode(PredictionMode.LL)
            parser.compilationUnit() // STAGE 2
            // if we parse ok, it's LL not SLL
        }
        rules = parser.getRuleNames().toList()
        return tree
    }

    override fun parse(stream: ANTLRInputStream, types: TypeRegistry): Expression {
        return tree(stream).accept(ASTTransform(types)).single()
    }
}
