package com.timepath.compiler

import com.timepath.compiler.types.Type

public data class Value(val any: Any) {

    /**
     * TODO
     */
    fun toBoolean(): Boolean = false

    // TODO: vectors

    operator fun plus(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs + rhs)
                is Int -> return Value(lhs + rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs + rhs)
                is Float -> return Value(lhs + rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    operator fun minus(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs - rhs)
                is Int -> return Value(lhs - rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs - rhs)
                is Float -> return Value(lhs - rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    fun shl(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs.toInt() shl rhs.toInt())
                is Int -> return Value(lhs.toInt() shl rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs shl rhs)
                is Float -> return Value(lhs shl rhs.toInt())
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    fun shr(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs.toInt() shr rhs.toInt())
                is Int -> return Value(lhs.toInt() shr rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs shr rhs)
                is Float -> return Value(lhs shr rhs.toInt())
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    operator fun times(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs * rhs)
                is Int -> return Value(lhs * rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs * rhs)
                is Float -> return Value(lhs * rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    operator fun div(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs / rhs)
                is Int -> return Value(lhs / rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs / rhs)
                is Float -> return Value(lhs / rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    operator fun mod(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs % rhs)
                is Int -> return Value(lhs % rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs % rhs)
                is Float -> return Value(lhs % rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    fun cast(type: Type): Value {
        // TODO
        //        val lhs = any
        //        when (lhs) {
        //            is Number -> when (type) {
        //                is float_t -> return Value(lhs.toFloat())
        //                is int_t -> return Value(lhs.toInt())
        //            }
        //        }
        throw UnsupportedOperationException("not supported")
    }

    operator fun minus() = Value(0) - this
}
