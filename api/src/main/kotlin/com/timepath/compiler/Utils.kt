package com.timepath.compiler

import com.timepath.Logger
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval

public fun ParserRuleContext.getTextWS(): String {
    return Interval(start.startIndex, stop.stopIndex).let {
        start.inputStream.getText(it)
    }
}

public fun ParserRuleContext.debug(): String {
    val token = start
    val source = token.tokenSource

    val line = token.line
    val col = token.charPositionInLine
    val file = source.sourceName
    return "$file:$line:$col"
}

public fun String.unquote(): String = substring(1, length - 1)
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")

public inline fun <T> time(logger: Logger, name: String, action: () -> T): T {
    val start = System.currentTimeMillis()
    val ret = action()
    logger.info { "$name: ${(System.currentTimeMillis() - start).toDouble() / 1000} seconds" }
    return ret
}

public fun String.quote(): String = "\"${buildString {
    for (c in this@quote) {
        when (c) {
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '"' -> append("\\\"")
            else -> append(c)
        }
    }
}}\""
