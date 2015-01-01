package com.timepath.quakec.vm

import com.timepath.quakec.vm.defs.Header
import com.timepath.quakec.vm.defs.ProgramData
import groovy.transform.CompileStatic

@CompileStatic
class ProgramDataWriter {

    RandomAccessFile f

    ProgramDataWriter(File file) {
        f = new RandomAccessFile(file)
    }

    def write(ProgramData ret) {
        def writeSection = { Header.Section it ->
            f.writeInt(it.offset)
            f.writeInt(it.count)
        }
        def h = ret.header
        f.writeInt h.version
        f.writeInt h.crc
        writeSection h.statements
        writeSection h.globalDefs
        writeSection h.fieldDefs
        writeSection h.functions
        writeSection h.stringData
        writeSection h.globalData
        f.writeInt h.entityCount

        f.offset = ret.header.statements.offset
        for (it in ret.statements) {
            f.writeShort it.op.ordinal() as short
            f.writeShort it.a
            f.writeShort it.b
            f.writeShort it.c
        }
        f.offset = ret.header.globalDefs.offset
        for (it in ret.globalDefs) {
            f.writeShort it.type
            f.writeShort it.offset
            f.writeInt it.nameOffset
        }
        f.offset = ret.header.fieldDefs.offset
        for (it in ret.fieldDefs) {
            f.writeShort it.type
            f.writeShort it.offset
            f.writeInt it.nameOffset
        }
        f.offset = ret.header.functions.offset
        for (it in ret.functions) {
            f.writeInt it.firstStatement
            f.writeInt it.firstLocal
            f.writeInt it.numLocals
            f.writeInt it.profiling
            f.writeInt it.nameOffset
            f.writeInt it.fileNameOffset
            f.writeInt it.numParams
            f.write it.sizeof
        }
        for (it in ret.strings.constant.entrySet()) {
            f.offset = ret.header.stringData.offset + it.key
            f.writeString it.value
        }
        f.offset = ret.header.globalData.offset
        f.write ret.globalData.array()
    }

}
