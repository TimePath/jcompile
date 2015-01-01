package com.timepath.quakec.vm.defs

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class Header {

    int version, crc, entityCount

    Section statements, globalDefs, fieldDefs, functions, stringData, globalData

    @ToString(includeNames = true)
    static class Section {

        int offset, count

    }

}