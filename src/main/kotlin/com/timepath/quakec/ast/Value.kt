package com.timepath.quakec.ast

import org.antlr.v4.runtime.misc.Utils

class Value(val value: Any? = null) {

    val type: Type? = null

    fun toBoolean(): Boolean = false

    override fun toString(): String = Utils.escapeWhitespace(value.toString(), false)
}
