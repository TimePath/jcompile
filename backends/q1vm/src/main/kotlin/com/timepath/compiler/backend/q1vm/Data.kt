package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.Value
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.types.Type

data class Pointer(val int: Int)

fun Pointer.expr(name: String? = null, type: Type? = null) = ConstantExpression(Value(this), name, type, null)

data class Vector(val x: Float, val y: Float, val z: Float)

fun Vector.expr(name: String? = null) = ConstantExpression(Value(this), name, null, null)
