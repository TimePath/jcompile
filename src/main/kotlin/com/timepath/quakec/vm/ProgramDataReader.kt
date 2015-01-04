package com.timepath.quakec.vm

import com.timepath.quakec.vm.defs.*

import java.io.File
import java.util.ArrayList
import com.timepath.quakec.vm.defs.Header.Section
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedHashMap

class ProgramDataReader(file: File) {

    val f: RandomAccessFile = RandomAccessFile(file, "r")

    fun <T> iterData(section: Header.Section, action: () -> T): MutableList<T> {
        val ret = ArrayList<T>(section.count)
        f.offset = section.offset.toLong()
        for (i in 0..section.count - 1) {
            ret add action()
        }
        return ret
    }

    fun readSection(): Section = Section(offset = f.readInt(), count = f.readInt())

    fun read(): ProgramData {
        val header = Header(
                version = f.readInt(),
                crc = f.readInt(),
                statements = readSection(),
                globalDefs = readSection(),
                fieldDefs = readSection(),
                functions = readSection(),
                stringData = readSection(),
                globalData = readSection(),
                entityCount = f.readInt()
        )
        return ProgramData(
                header = header,
                statements = iterData(header.statements) {
                    Statement(
                            f.readShort(),
                            f.readShort(),
                            f.readShort(),
                            f.readShort()
                    )
                },
                globalDefs = iterData(header.globalDefs) {
                    Definition(
                            f.readShort(),
                            f.readShort(),
                            f.readInt()
                    )
                },
                fieldDefs = iterData(header.fieldDefs) {
                    Definition(
                            f.readShort(),
                            f.readShort(),
                            f.readInt()
                    )
                },
                functions = iterData(header.functions) {
                    Function(
                            f.readInt(),
                            f.readInt(),
                            f.readInt(),
                            f.readInt(),
                            f.readInt(),
                            f.readInt(),
                            f.readInt(),
                            byteArray(f.readByte(), f.readByte(), f.readByte(), f.readByte(),
                                    f.readByte(), f.readByte(), f.readByte(), f.readByte())
                    )
                },
                strings = StringManager({
                    val list: MutableList<String> = ArrayList(512)
                    val sb = StringBuilder()
                    f.offset = header.stringData.offset.toLong()

                    val c: Int
                    while (true) {
                        c = f.readByte().toInt()
                        when {
                            c < 0 -> break
                            c == 0 -> {
                                list add sb.toString()
                                sb.setLength(0)
                            }
                            else -> sb.append(c.toChar())
                        }
                    }

                    var stroff = 0
                    val strings = LinkedHashMap<Int, String>()
                    for (s in list) {
                        strings[stroff] = s
                        stroff += s.length() + 1
                    }
                    strings
                }(), header.stringData.count),
                globalData = {
                    f.offset = header.globalData.offset.toLong()
                    val bytes = ByteArray(header.globalData.count * 4)
                    f.read(bytes)
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                }()
        )
    }

}
