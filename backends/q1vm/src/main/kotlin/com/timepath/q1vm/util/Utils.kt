package com.timepath.q1vm.util

import java.nio.FloatBuffer
import java.nio.IntBuffer

operator fun FloatBuffer.set(index: Int, value: Float) = this.put(index, value)
operator fun IntBuffer.set(index: Int, value: Int) = this.put(index, value)
operator fun Float.not(): Boolean = this == 0f
operator fun Int.not(): Boolean = this == 0
fun Boolean.toFloat(): Float = if (this) 1f else 0f
