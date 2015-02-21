package com.timepath.compiler

class Value(val any: Any) {

    fun toBoolean(): Boolean = false

    override fun toString() = "${any.javaClass.getSimpleName()}(${any.toString().quote()})"

    override fun equals(other: Any?) = (other is Value) && (any == other.any)

    override fun hashCode() = any.hashCode()

    fun plus(other: Value?): Value? {
        val lhs = any
        val rhs = other!!.any
        return when {
            lhs is Float && rhs is Float -> Value(lhs + rhs)
            else -> throw UnsupportedOperationException("not supported")
        }
    }

    fun minus(other: Value?): Value? {
        val lhs = any
        val rhs = other!!.any
        return when {
            lhs is Float && rhs is Float -> Value(lhs - rhs)
            else -> throw UnsupportedOperationException("not supported")
        }
    }

    fun times(other: Value?): Value? {
        val lhs = any
        val rhs = other!!.any
        return when {
            lhs is Float && rhs is Float -> Value(lhs * rhs)
            else -> throw UnsupportedOperationException("not supported")
        }
    }

    fun div(other: Value?): Value? {
        val lhs = any
        val rhs = other!!.any
        return when {
            lhs is Float && rhs is Float -> Value(lhs / rhs)
            else -> throw UnsupportedOperationException("not supported")
        }
    }

    fun mod(other: Value?): Value? {
        val lhs = any
        val rhs = other!!.any
        return when {
            lhs is Float && rhs is Float -> Value(lhs % rhs)
            else -> throw UnsupportedOperationException("not supported")
        }
    }
}
