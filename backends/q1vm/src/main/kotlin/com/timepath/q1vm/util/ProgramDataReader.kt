package com.timepath.q1vm.util

import com.timepath.q1vm.ProgramData
import com.timepath.q1vm.StringManager
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ProgramDataReader(val raf: IOWrapper) {

    constructor(file: File) : this(IOWrapper.File(file))

    private inline fun iterData<T>(section: ProgramData.Header.Section, action: () -> T): List<T> {
        raf.offset = section.offset
        return (0..section.count - 1).map { action() }
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
                    ProgramData.Statement(
                            raf.readShort(),
                            raf.readShort(),
                            raf.readShort(),
                            raf.readShort()
                    )
                },
                globalDefs = iterData(header.globalDefs) {
                    ProgramData.Definition(
                            raf.readShort(),
                            raf.readShort(),
                            raf.readInt()
                    )
                },
                fieldDefs = iterData(header.fieldDefs) {
                    ProgramData.Definition(
                            raf.readShort(),
                            raf.readShort(),
                            raf.readInt()
                    )
                },
                functions = iterData(header.functions) {
                    ProgramData.Function(
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            raf.readInt(),
                            byteArrayOf(raf.readByte(), raf.readByte(), raf.readByte(), raf.readByte(),
                                    raf.readByte(), raf.readByte(), raf.readByte(), raf.readByte())
                    )
                },
                strings = StringManager(run {
                    raf.offset = header.stringData.offset

                    val list: MutableList<String> = arrayListOf()
                    val sb = StringBuilder()
                    val c: Int
                    loop@
                    while (raf.offset - header.stringData.offset < header.stringData.count) {
                        c = raf.readByte().toInt()
                        when {
                            c < 0 -> break@loop
                            c == 0 -> {
                                list add sb.toString()
                                sb.setLength(0)
                            }
                            else -> sb.append(c.toChar())
                        }
                    }
                    list
                }, header.stringData.count),
                globalData = ByteBuffer.wrap(run {
                    raf.offset = header.globalData.offset

                    val bytes = ByteArray(4 * header.globalData.count)
                    raf.read(bytes)
                    bytes
                }).order(ByteOrder.LITTLE_ENDIAN)
        )
        return ret
    }

}
