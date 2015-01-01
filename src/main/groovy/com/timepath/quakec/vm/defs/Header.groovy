package com.timepath.quakec.vm.defs

import com.timepath.quakec.vm.RandomAccessFile
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class Header {

    int version, crc, entityCount

    Section statements, globalDefs, fieldDefs, functions, stringData, globalData

    def Header(RandomAccessFile r) {
        version = r.readInt()
        crc = r.readInt()

        statements = new Section(r)
        globalDefs = new Section(r)
        fieldDefs = new Section(r)
        functions = new Section(r)
        stringData = new Section(r)
        globalData = new Section(r)

        entityCount = r.readInt()
    }

    def write(RandomAccessFile r) {
        r.writeInt(version)
        r.writeInt(crc)
        statements.write(r)
        globalDefs.write(r)
        fieldDefs.write(r)
        functions.write(r)
        stringData.write(r)
        globalData.write(r)
        r.writeInt(entityCount)
    }

    @ToString(includeNames = true)
    class Section {

        int offset, count

        def Section(RandomAccessFile r) {
            offset = r.readInt()
            count = r.readInt()
        }

        def write(RandomAccessFile r) {
            r.writeInt(offset)
            r.writeInt(count)
        }
    }

}