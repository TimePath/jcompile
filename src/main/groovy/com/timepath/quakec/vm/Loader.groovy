package com.timepath.quakec.vm

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

@CompileStatic
class Loader {

    Header h
    List<Statement> statements
    List<Definition> globalDefs
    List<Definition> fieldDefs
    List<Function> functions
    LinkedHashMap<Integer, String> stringData
    IntBuffer globalIntData
    FloatBuffer globalFloatData

    @Delegate
    private BinaryReader d

    Loader(File file) {
        this.d = new BinaryReader(new RandomAccessFile(file, "r"))
        h = new Header(this)
        statements = iterData(h.statements) {
            new Statement(
                    readShort(),
                    readShort(),
                    readShort(),
                    readShort()
            )
        }
        globalDefs = iterData(h.globalDefs) {
            new Definition(
                    readShort(),
                    readShort(),
                    readInt()
            ).with { loader = this; it }
        }
        fieldDefs = iterData(h.fieldDefs) {
            new Definition(
                    readShort(),
                    readShort(),
                    readInt()
            ).with { loader = this; it }
        }
        functions = iterData(h.functions) {
            new Function(
                    readInt(),
                    readInt(),
                    readInt(),
                    readInt(),
                    readInt(),
                    readInt(),
                    readInt(),
                    [readByte(), readByte(), readByte(), readByte(),
                     readByte(), readByte(), readByte(), readByte()] as byte[]
            ).with { loader = this; it }
        }
        stringData = {
            List<String> list = []
            def sb = new StringBuilder()
            offset = h.stringData.offset
            for (int c; (c = readByte()) != -1;) {
                if (!c) {
                    list << sb.toString()
                    sb.length = 0
                    continue
                }
                sb << ((char) c & 0xFF)
            }

            int stroff = 0
            LinkedHashMap<Integer, String> ret = [:]
            for (String s in list) {
                ret[stroff] = s
                stroff += s.length() + 1
            }
            ret
        }()
        def globalData = {
            offset = h.globalData.offset
            def bytes = new byte[h.globalData.count * 4]
            read(bytes)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        }()
        globalIntData = globalData.asIntBuffer()
        globalFloatData = globalData.asFloatBuffer()
    }

    private <T> List<T> iterData(Header.Section section, Closure<T> closure) {
        def ret = []
        offset = section.offset
        section.count.times {
            ret << closure()
        }
        ret
    }
}
