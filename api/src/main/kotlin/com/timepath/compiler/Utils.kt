package com.timepath.compiler

import com.timepath.Logger
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval

public fun ParserRuleContext.getTextWS(): String {
    return Interval(start.getStartIndex(), stop.getStopIndex()).let {
        start.getInputStream().getText(it)
    }
}

public fun ParserRuleContext.debug(): String {
    val token = start
    val source = token.getTokenSource()

    val line = token.getLine()
    val col = token.getCharPositionInLine()
    val file = source.getSourceName()
    return "$file:$line:$col"
}

public fun String.unquote(): String = substring(1, length() - 1)

public inline fun time<T>(logger: Logger, name: String, action: () -> T): T {
    val start = System.currentTimeMillis()
    val ret = action()
    logger.info { "$name: ${(System.currentTimeMillis() - start).toDouble() / 1000} seconds" }
    return ret
}

public fun String.quote(): String = "\"${StringBuilder {
    for (c in this@quote) {
        when (c) {
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '"' -> append("\\\"")
            else -> append(c)
        }
    }
}}\""
