package com.timepath.quakec.vm

import com.timepath.quakec.vm.defs.*

import java.io.File

class ProgramDataWriter(file: File) {

    val f = RandomAccessFile(file)

    fun writeSection(it: Header.Section) {
        f.writeInt(it.offset)
        f.writeInt(it.count)
    }

    fun write(ret: ProgramData) {
        val h = ret.header!!
        f.writeInt(h.version)
        f.writeInt(h.crc)
        writeSection(h.statements)
        writeSection(h.globalDefs)
        writeSection(h.fieldDefs)
        writeSection(h.functions)
        writeSection(h.stringData)
        writeSection(h.globalData)
        f.writeInt(h.entityCount)

        f.offset = ret.header.statements.offset.toLong()
        for (it in ret.statements!!) {
            f.writeShort(it.op.ordinal())
            f.writeShort(it.a)
            f.writeShort(it.b)
            f.writeShort(it.c)
        }

        f.offset = ret.header.globalDefs.offset.toLong()
        for (it in ret.globalDefs!!) {
            f.writeShort(it.type.toInt())
            f.writeShort(it.offset.toInt())
            f.writeInt(it.nameOffset)
        }

        f.offset = ret.header.fieldDefs.offset.toLong()
        for (it in ret.fieldDefs!!) {
            f.writeShort(it.type.toInt())
            f.writeShort(it.offset.toInt())
            f.writeInt(it.nameOffset)
        }

        f.offset = ret.header.functions.offset.toLong()
        for (it in ret.functions!!) {
            f.writeInt(it.firstStatement)
            f.writeInt(it.firstLocal)
            f.writeInt(it.numLocals)
            f.writeInt(it.profiling)
            f.writeInt(it.nameOffset)
            f.writeInt(it.fileNameOffset)
            f.writeInt(it.numParams)
            f.write(it.sizeof)
        }

        for ((key, value) in ret.strings!!.constant.entrySet()) {
            f.offset = ret.header.stringData.offset.toLong() + key
            f.writeString(value)
        }

        f.offset = ret.header.globalData.offset.toLong()
        f.write(ret.globalData!!.array())
    }

}
