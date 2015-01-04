package com.timepath.quakec.vm.defs

import com.timepath.quakec.vm.StringManager

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.properties.Delegates

class ProgramData(val header: Header? = null,
                  val statements: List<Statement>? = null,
                  val globalDefs: List<Definition>? = null,
                  val fieldDefs: List<Definition>? = null,
                  val functions: List<Function>? = null,
                  val strings: StringManager? = null,
                  val globalData: ByteBuffer? = null) {

    val globalIntData: IntBuffer by Delegates.lazy {
        globalData!!.asIntBuffer()
    }
    val globalFloatData: FloatBuffer by Delegates.lazy {
        globalData!!.asFloatBuffer()
    }
}
