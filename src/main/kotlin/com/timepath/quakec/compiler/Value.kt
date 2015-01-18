package com.timepath.quakec.compiler

class Value(val value: Any? = null) {

    val type: Type? = null

    fun toBoolean(): Boolean = false

    override fun toString(): String = "${value?.javaClass?.getSimpleName()}(${value?.toString()?.quote() ?: ""})"

    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is Value -> false
            else -> value == other.value
        }
    }

    override fun hashCode(): Int {
        return when {
            value != null -> value.hashCode()
            else -> 0
        }
    }
}