package com.timepath.q1vm.util

import java.nio.ByteBuffer

interface IOWrapper {
    var offset: Int

    private fun _read(n: Int): Long {
        var ret = 0
        repeat(n) {
            val b = read()
            ret = ret or ((b and 0xFF) shl (8 * it))
        }
        return ret.toLong()
    }

    fun read(): Int

    fun read(b: ByteArray)

    fun readBoolean(): Boolean = _read(1) != 0L

    fun readByte(): Byte = _read(1).toByte()

    fun readUnsignedByte(): Int = (_read(1) and 0xFF).toInt()

    fun readShort(): Short = _read(2).toShort()

    fun readUnsignedShort(): Int = (_read(2) and 0xFFFF).toInt()

    fun readChar(): Char = _read(2).toChar()

    fun readInt(): Int = _read(4).toInt()

    fun readUnsignedInt(): Long = _read(4) and 0xFFFFFFFFL

    fun readLong(): Long = _read(8)

    fun readFloat(): Float = java.lang.Float.intBitsToFloat(readInt())

    fun readDouble(): Double = java.lang.Double.longBitsToDouble(readLong())

    private fun _write(n: Int, v: Int) = repeat(n) { doWrite((v ushr (it * 8)).toByte()) }

    fun doWrite(b: Byte)

    fun write(b: ByteArray)

    fun writeByte(v: Int) = _write(1, v)

    fun writeShort(v: Int) = _write(2, v)

    fun writeInt(v: Int) = _write(4, v)

    class File(file: java.io.File, write: Boolean = false) : IOWrapper {
        val raf = java.io.RandomAccessFile(file, if (write) "rw" else "r")

        override fun read(): Int = raf.read()

        override fun read(b: ByteArray) {
            raf.read(b)
        }

        override fun doWrite(b: Byte) = raf.write(b.toInt())

        override fun write(b: ByteArray) = raf.write(b)

        override var offset: Int
            get() = raf.filePointer.toInt()
            set(value) = raf.seek(value.toLong())
    }

    class Buffer(val buf: ByteBuffer) : IOWrapper {
        override fun read(): Int = buf.get().toInt()

        override fun read(b: ByteArray) {
            buf.get(b)
        }

        override fun doWrite(b: Byte) {
            buf.put(b)
        }

        override fun write(b: ByteArray) {
            buf.put(b)
        }

        override var offset: Int
            get() = buf.position()
            set(offset) {
                buf.position(offset)
            }
    }
}
