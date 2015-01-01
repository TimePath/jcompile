package com.timepath.quakec.vm

import com.timepath.quakec.vm.defs.Definition
import com.timepath.quakec.vm.defs.Function
import com.timepath.quakec.vm.defs.Header
import com.timepath.quakec.vm.defs.ProgramData
import com.timepath.quakec.vm.defs.Statement
import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.ByteOrder

@CompileStatic
class ProgramDataLoader {

    BinaryReader r

    ProgramDataLoader(File file) {
        r = new BinaryReader(new RandomAccessFile(file, "r"))
    }

    ProgramData load() {
        def ret = new ProgramData()

        ret.header = new Header(r)
        ret.statements = iterData(ret.header.statements) {
            new Statement(
                    r.readShort(),
                    r.readShort(),
                    r.readShort(),
                    r.readShort()
            ).with { loader = ret; it }
        }
        ret.globalDefs = iterData(ret.header.globalDefs) {
            new Definition(
                    r.readShort(),
                    r.readShort(),
                    r.readInt()
            ).with { data = ret; it }
        }
        ret.fieldDefs = iterData(ret.header.fieldDefs) {
            new Definition(
                    r.readShort(),
                    r.readShort(),
                    r.readInt()
            ).with { data = ret; it }
        }
        ret.functions = iterData(ret.header.functions) {
            new Function(
                    r.readInt(),
                    r.readInt(),
                    r.readInt(),
                    r.readInt(),
                    r.readInt(),
                    r.readInt(),
                    r.readInt(),
                    [r.readByte(), r.readByte(), r.readByte(), r.readByte(),
                     r.readByte(), r.readByte(), r.readByte(), r.readByte()] as byte[]
            ).with { data = ret; it }
        }
        def stringData = {
            List<String> list = []
            def sb = new StringBuilder()
            r.offset = ret.header.stringData.offset
            for (int c; (c = r.readByte()) != -1;) {
                if (!c) {
                    list << sb.toString()
                    sb.length = 0
                    continue
                }
                sb << ((char) c & 0xFF)
            }

            int stroff = 0
            LinkedHashMap<Integer, String> strings = [:]
            for (String s in list) {
                strings[stroff] = s
                stroff += s.length() + 1
            }
            strings
        }()
        def globalData = {
            r.offset = ret.header.globalData.offset
            def bytes = new byte[ret.header.globalData.count * 4]
            r.read(bytes)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        }()
        ret.globalData = globalData
        ret.strings = new StringManager(stringData, ret.header.stringData.count)
        return ret
    }

    private <T> List<T> iterData(Header.Section section, Closure<T> closure) {
        def ret = []
        r.offset = section.offset
        section.count.times {
            ret << closure()
        }
        ret
    }

}
