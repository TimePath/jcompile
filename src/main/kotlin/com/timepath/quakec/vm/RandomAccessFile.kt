package com.timepath.quakec.vm

import java.io.File

class RandomAccessFile(file: File, mode: String = "rw") {

    val raf = java.io.RandomAccessFile(file, mode)

    private fun _read(n: Int): Long {
        var ret = 0
        for (i in 0..n - 1) {
            val b = raf.read()
            ret = ret or ((b and 0xFF) shl (8 * i))
        }
        return ret.toLong()
    }

    fun read(b: ByteArray): Int = raf.read(b)

    var offset: Long
        get() = raf.getFilePointer()
        set(offset) = raf.seek(offset.toLong())

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

    fun writeString(s: String) {
        raf.writeBytes(s)
        writeByte(0)
    }

    fun _write(n: Int, v: Int) {
        for (i in 0..n - 1) {
            raf.write(((v.toLong() ushr (i * 8)) and 255).toInt())
        }
    }

    fun write(b: ByteArray) = raf.write(b)

    fun writeByte(v: Int) = _write(1, v)

    fun writeShort(v: Int) = _write(2, v)

    fun writeInt(v: Int) = _write(4, v)
}
