package com.timepath.q1vm.util

import com.timepath.q1vm.ProgramData
import java.io.File
import kotlin.test.assertEquals

class ProgramDataWriter(val raf: IOWrapper) {

    constructor(file: File) : this(IOWrapper.File(file, write = true))

    private fun writeSection(it: ProgramData.Header.Section) {
        raf.writeInt(it.offset)
        raf.writeInt(it.count)
    }

    fun write(ret: ProgramData) {
        val h = ret.header
        raf.writeInt(h.version)
        raf.writeInt(h.crc)
        writeSection(h.statements)
        writeSection(h.globalDefs)
        writeSection(h.fieldDefs)
        writeSection(h.functions)
        writeSection(h.stringData)
        writeSection(h.globalData)
        raf.writeInt(h.entityFields)

        raf.offset = ret.header.statements.offset
        for (it in ret.statements) {
            raf.writeShort(it.op.ordinal)
            raf.writeShort(it.a)
            raf.writeShort(it.b)
            raf.writeShort(it.c)
        }

        raf.offset = ret.header.globalDefs.offset
        for (it in ret.globalDefs) {
            raf.writeShort(it.type.toInt())
            raf.writeShort(it.offset.toInt())
            raf.writeInt(it.nameOffset)
        }

        raf.offset = ret.header.fieldDefs.offset
        for (it in ret.fieldDefs) {
            raf.writeShort(it.type.toInt())
            raf.writeShort(it.offset.toInt())
            raf.writeInt(it.nameOffset)
        }

        raf.offset = ret.header.functions.offset
        for (it in ret.functions) {
            raf.writeInt(it.firstStatement)
            raf.writeInt(it.firstLocal)
            raf.writeInt(it.numLocals)
            raf.writeInt(it.profiling)
            raf.writeInt(it.nameOffset)
            raf.writeInt(it.fileNameOffset)
            raf.writeInt(it.numParams)
            raf.write(it.sizeof)
        }

        raf.offset = ret.header.stringData.offset
        raf.write(ret.strings.constant)
        assertEquals(ret.header.stringData.let { it.offset + it.count }, raf.offset)
        // Ensure termination
        raf.writeByte(0)
        raf.writeByte(0)

        raf.offset = ret.header.globalData.offset
        raf.write(ByteArray(ret.globalData.capacity()).apply { ret.globalData.get(this) })
    }

}
