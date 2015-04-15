package com.timepath.q1vm.util

import com.timepath.q1vm.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

fun ProgramDataReader(file: File) = ProgramDataReader(IOWrapper.File(file))
class ProgramDataReader(val raf: IOWrapper) {

    private fun <T> iterData(section: ProgramData.Header.Section, action: () -> T): MutableList<T> {
        val ret = ArrayList<T>(section.count)
        raf.offset = section.offset
        for (i in 0..section.count - 1) {
            ret add action()
        }
        return ret
    }

    private fun readSection() = ProgramData.Header.Section(
            offset = raf.readInt(),
            count = raf.readInt()
    )

    public fun read(): ProgramData {
        val header = ProgramData.Header(
                version = raf.readInt(),
                crc = raf.readInt(),
                statements = readSection(),
                globalDefs = readSection(),
                fieldDefs = readSection(),
                functions = readSection(),
                stringData = readSection(),
                globalData = readSection(),
                entityFields = raf.readInt()
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
                    raf.offset = header.stringData.offset

                    val list: MutableList<String> = ArrayList(512)
                    val sb = StringBuilder()
                    val c: Int
                    while (raf.offset - header.stringData.offset < header.stringData.count) {
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
                    list
                }(), header.stringData.count),
                globalData = ByteBuffer.wrap({
                    raf.offset = header.globalData.offset

                    val bytes = ByteArray(4 * header.globalData.count)
                    raf.read(bytes)
                    bytes
                }()).order(ByteOrder.LITTLE_ENDIAN)
        )
        return ret
    }

}
