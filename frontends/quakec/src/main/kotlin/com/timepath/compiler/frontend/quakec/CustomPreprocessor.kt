package com.timepath.compiler.frontend.quakec

import org.anarres.cpp.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumSet

private class CustomPreprocessor : Preprocessor() {

    init {
        addWarnings(EnumSet.allOf(javaClass<Warning>()))
        setListener(DefaultPreprocessorListener())
        val now = Date()
        addMacro("__DATE__", SimpleDateFormat("\"MMM dd yyyy\"").format(now))
        addMacro("__TIME__", SimpleDateFormat("\"hh:mm:ss\"").format(now))
        addFeature(Feature.LINEMARKERS)
    }

    override fun token(): Token {
        try {
            return super.token()
        } catch (e: Exception) {
            if (e.getMessage()?.matches("Bad token \\[#@\\d+,\\d+\\]:\"#\"") ?: false) {
                return Token(Token.HASH, -1, -1, "#", null)
            }
            throw e
        }
    }

    override fun pragma(name: Token, value: MutableList<Token>) {
        when (name.getText()) {
            "noref" -> return
            else -> super.pragma(name, value)
        }
    }
}
