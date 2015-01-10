package com.timepath.quakec.vm.util

import java.io.File
import java.util.ArrayList
import java.util.LinkedHashMap
import java.nio.ByteOrder
import java.nio.ByteBuffer
import com.timepath.quakec.vm.*

class ProgramDataReader(file: File) {

    val raf = RandomAccessFile(file, "r")

    private fun <T> iterData(section: ProgramData.Header.Section, action: () -> T): MutableList<T> {
        val ret = ArrayList<T>(section.count)
        raf.offset = section.offset.toLong()
        for (i in 0..section.count - 1) {
            ret add action()
        }
        return ret
    }

    private fun readSection(): ProgramData.Header.Section = ProgramData.Header.Section(offset = raf.readInt(), count = raf.readInt())

    fun read(): ProgramData {
        val header = ProgramData.Header(
                version = raf.readInt(),
                crc = raf.readInt(),
                statements = readSection(),
                globalDefs = readSection(),
                fieldDefs = readSection(),
                functions = readSection(),
                stringData = readSection(),
                globalData = readSection(),
                entityCount = raf.readInt()
        )
        val ret = ProgramData(
                header = header,
                statements = iterData(header.statements) {
                    Statement(
                            raf.readShort(),
                            raf.readShort(),
                            raf.readShort(),
                            raf.readShort()
                    )
                },
                globalDefs = iterData(header.globalDefs) {
                    Definition(
                            raf.readShort(),
                            raf.readShort(),
                            raf.readInt()
                    )
                },
                fieldDefs = iterData(header.fieldDefs) {
                    Definition(
                            raf.readShort(),
                            raf.readShort(),
                            raf.readInt()
                    )
                },
                functions = iterData(header.functions) {
                    Function(
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            byteArray(raf.readByte(), raf.readByte(), raf.readByte(), raf.readByte(),
                                    raf.readByte(), raf.readByte(), raf.readByte(), raf.readByte())
                    )
                },
                strings = StringManager({
                    val list: MutableList<String> = ArrayList(512)
                    val sb = StringBuilder()
                    raf.offset = header.stringData.offset.toLong()

                    val c: Int
                    while (true) {
                        c = raf.readByte().toInt()
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
                    val strings = LinkedHashMap<Int, String>(list.size())
                    for (s in list) {
                        strings[stroff] = s
                        stroff += s.length() + 1
                    }
                    strings
                }(), header.stringData.count),
                globalData = {
                    raf.offset = header.globalData.offset.toLong()
                    val bytes = ByteArray(header.globalData.count * 4)
                    raf.read(bytes)
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                }()
        )
        ret.statements?.forEach { it.data = ret }
        ret.globalDefs?.forEach { it.data = ret }
        ret.fieldDefs?.forEach { it.data = ret }
        ret.functions?.forEach { it.data = ret }
        return ret
    }

}