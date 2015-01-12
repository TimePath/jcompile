package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.quote
import org.antlr.v4.runtime.misc.Utils

class Value(val value: Any? = null) {

    val type: Type? = null

    fun toBoolean(): Boolean = false

    override fun toString(): String = "${value?.javaClass?.getSimpleName()}(${value.toString().quote()})"
}