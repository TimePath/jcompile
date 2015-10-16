package com.timepath.compiler.frontend.quakec

import com.timepath.compiler.unquote
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Pair

class CustomLexer(input: ANTLRInputStream) : NewQCLexer(input) {

    private var file = getSourceName()

    init {
        setTokenFactory(object : CommonTokenFactory() {
            private fun TokenSource.setSourceName(name: String): TokenSource = object : TokenSource by this {
                override fun getSourceName() = name
            }

            override fun create(source: Pair<TokenSource, CharStream>, type: Int, text: String?,
                                channel: Int, start: Int, stop: Int, line: Int, charPositionInLine: Int) =
                    super.create(Pair(source.a.setSourceName(file), source.b),
                            type, text, channel, start, stop, line, charPositionInLine)
        })
    }

    override fun emit(): Token {
        val token = super.emit()
        if (token.getType() == NewQCLexer.LineDirective) {
            val s = token.getText()
            val split = s.splitBy(" ")
            val line = split[1]
            setLine(line.toInt() - 1)
            file = split[2].trim().unquote()
            // TODO
            // val flags = split.drop(3)
            // `1' indicates the start of a new file.
            // `2' indicates returning to a file (after having included another file).
            // `3' indicates that the following text comes from a system header file; warnings should be suppressed.
            // `4' indicates that the following text should be treated as being wrapped in an implicit extern "C" block.
        }
        return token
    }
}
