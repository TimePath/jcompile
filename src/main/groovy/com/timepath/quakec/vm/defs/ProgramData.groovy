package com.timepath.quakec.vm.defs

import com.timepath.quakec.vm.StringManager
import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

@CompileStatic
class ProgramData {

    Header header
    List<Statement> statements
    List<Definition> globalDefs
    List<Definition> fieldDefs
    List<Function> functions
    StringManager strings
    ByteBuffer globalData
    @Lazy
    IntBuffer globalIntData = globalData.asIntBuffer()
    @Lazy
    FloatBuffer globalFloatData = globalData.asFloatBuffer()

}
