package com.timepath.quakec.vm

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString
class Header {

    int version, crc, entityCount

    Section statements, globalDefs, fieldDefs, functions, stringData, globalData

    def Header(Loader l) {
        version = l.readInt()
        crc = l.readInt()

        statements = new Section(l)
        globalDefs = new Section(l)
        fieldDefs = new Section(l)
        functions = new Section(l)
        stringData = new Section(l)
        globalData = new Section(l)

        entityCount = l.readInt()
    }

    @ToString
    class Section {

        int offset, count

        def Section(Loader l) {
            offset = l.readInt()
            count = l.readInt()
        }
    }

}