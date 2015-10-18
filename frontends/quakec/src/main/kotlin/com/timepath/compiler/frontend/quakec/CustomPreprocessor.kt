package com.timepath.compiler.frontend.quakec

import org.anarres.cpp.*
import java.text.SimpleDateFormat
import java.util.*

class CustomPreprocessor : Preprocessor() {

    init {
        addWarnings(EnumSet.allOf(Warning::class.java))
        listener = DefaultPreprocessorListener()
        val now = Date()
        addMacro("__DATE__", SimpleDateFormat("\"MMM dd yyyy\"").format(now))
        addMacro("__TIME__", SimpleDateFormat("\"hh:mm:ss\"").format(now))
        addFeature(Feature.LINEMARKERS)
    }

    private val badRegex = "Bad token \\[#@\\d+,\\d+\\]:\"#\"".toRegex()

    override fun token(): Token {
        try {
            return super.token()
        } catch (e: Exception) {
            if (e.getMessage()?.matches(badRegex) ?: false) {
                return Token(Token.HASH, -1, -1, "#", null)
            }
            throw e
        }
    }

    override fun pragma(name: Token, value: MutableList<Token>) {
        when (name.text) {
            "noref" -> return
            else -> super.pragma(name, value)
        }
    }
}
