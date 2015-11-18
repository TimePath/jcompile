package com.timepath.compiler

import com.timepath.compiler.types.Type

// FIXME: violates modularity
data class Vector(val x: Float, val y: Float, val z: Float)

public data class Value(val any: Any) {

    fun toBoolean(): Boolean {
        (any as? Boolean)?.let { return it }
        (any as? Int)?.let { return it != 0 }
        (any as? Float)?.let { return it != 0f }
        (any as? Vector)?.let { return it.x != 0f && it.y != 0f && it.z != 0f }
        throw UnsupportedOperationException("not supported")
    }

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

    infix fun shl(other: Value): Value {
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

    infix fun shr(other: Value): Value {
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
                is Vector -> return Value(Vector(rhs.x * lhs, rhs.y * lhs, rhs.z * lhs))
            }
            is Int -> when (rhs) {
                is Float -> return Value(lhs * rhs)
                is Int -> return Value(lhs * rhs)
                is Vector -> return Value(Vector(rhs.x * lhs, rhs.y * lhs, rhs.z * lhs))
            }
            is Vector -> when (rhs) {
                is Float -> return Value(Vector(lhs.x * rhs, lhs.y * rhs, lhs.z * rhs))
                is Int -> return Value(Vector(lhs.x * rhs, lhs.y * rhs, lhs.z * rhs))
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

    operator fun unaryMinus() = Value(0) - this

    infix fun or(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Int -> when (rhs) {
                is Int -> return Value(lhs or rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }
}
