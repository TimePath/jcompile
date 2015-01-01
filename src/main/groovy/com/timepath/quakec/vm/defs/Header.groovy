package com.timepath.quakec.vm.defs

import com.timepath.quakec.vm.BinaryReader
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class Header {

    int version, crc, entityCount

    Section statements, globalDefs, fieldDefs, functions, stringData, globalData

    def Header(BinaryReader r) {
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

    @ToString(includeNames = true)
    class Section {

        int offset, count

        def Section(BinaryReader r) {
            offset = r.readInt()
            count = r.readInt()
        }
    }

}