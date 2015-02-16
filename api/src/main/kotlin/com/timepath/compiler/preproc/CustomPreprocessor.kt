package com.timepath.compiler.preproc

import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumSet
import org.anarres.cpp.DefaultPreprocessorListener
import org.anarres.cpp.Preprocessor
import org.anarres.cpp.Token
import org.anarres.cpp.Warning

class CustomPreprocessor : Preprocessor() {

    {
        getSystemIncludePath().add("/usr/include")
        val now = Date()
        addWarnings(EnumSet.allOf(javaClass<Warning>()))
        setListener(DefaultPreprocessorListener())
        addMacro("__DATE__", SimpleDateFormat("\"MMM dd yyyy\"").format(now))
        addMacro("__TIME__", SimpleDateFormat("\"hh:mm:ss\"").format(now))
    }

    override fun token(): Token? {
        try {
            return super.token()
        } catch (e: Exception) {
            if (e.getMessage()!!.matches("Bad token \\[#@\\d+,\\d+\\]:\"#\"")) {
                return Token(Token.HASH, -1, -1, "#", null)
            }
            throw e
        }
    }

    override fun pragma(name: Token, value: MutableList<Token>) {
        if ("noref" == name.getText()) return
        super.pragma(name, value)
    }
}
