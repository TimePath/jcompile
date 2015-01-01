package com.timepath.quakec.vm

import com.timepath.quakec.vm.defs.*
import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.ByteOrder

@CompileStatic
class ProgramDataReader {

    RandomAccessFile f

    ProgramDataReader(File file) {
        f = new RandomAccessFile(file, 'r')
    }

    ProgramData read() {
        def ret = new ProgramData()
        ret.header = {
            new Header().with {
                def readSection = {
                    new Header.Section().with {
                        offset = f.readInt()
                        count = f.readInt()
                        it
                    }
                }
                version = f.readInt()
                crc = f.readInt()
                statements = readSection()
                globalDefs = readSection()
                fieldDefs = readSection()
                functions = readSection()
                stringData = readSection()
                globalData = readSection()
                entityCount = f.readInt()
                it
            }
        }()
        ret.statements = iterData(ret.header.statements) {
            new Statement(
                    f.readShort(),
                    f.readShort(),
                    f.readShort(),
                    f.readShort()
            ).with { data = ret; it }
        }
        ret.globalDefs = iterData(ret.header.globalDefs) {
            new Definition(
                    f.readShort(),
                    f.readShort(),
                    f.readInt()
            ).with { data = ret; it }
        }
        ret.fieldDefs = iterData(ret.header.fieldDefs) {
            new Definition(
                    f.readShort(),
                    f.readShort(),
                    f.readInt()
            ).with { data = ret; it }
        }
        ret.functions = iterData(ret.header.functions) {
            new Function(
                    f.readInt(),
                    f.readInt(),
                    f.readInt(),
                    f.readInt(),
                    f.readInt(),
                    f.readInt(),
                    f.readInt(),
                    [f.readByte(), f.readByte(), f.readByte(), f.readByte(),
                     f.readByte(), f.readByte(), f.readByte(), f.readByte()] as byte[]
            ).with { data = ret; it }
        }
        ret.strings = new StringManager({
            List<String> list = []
            def sb = new StringBuilder()
            f.offset = ret.header.stringData.offset
            for (int c; (c = f.readByte()) != -1;) {
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
        }(), ret.header.stringData.count)
        ret.globalData = {
            f.offset = ret.header.globalData.offset
            def bytes = new byte[ret.header.globalData.count * 4]
            f.read(bytes)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        }()
        return ret
    }

    private <T> List<T> iterData(Header.Section section, Closure<T> closure) {
        def ret = []
        f.offset = section.offset
        section.count.times {
            ret << closure()
        }
        ret
    }

}
