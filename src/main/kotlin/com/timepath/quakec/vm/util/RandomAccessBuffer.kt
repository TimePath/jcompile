package com.timepath.quakec.vm.util

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

fun RandomAccessBuffer(file: File, write: Boolean = false): RandomAccessBuffer {
    val raf = RandomAccessFile(file, if (write) "rw" else "r")
    return RandomAccessBuffer(raf.getChannel().map(when {
        write -> FileChannel.MapMode.READ_WRITE
        else -> FileChannel.MapMode.READ_ONLY
    }, 0, file.length()))
}

class RandomAccessBuffer(val raf: ByteBuffer) {

    private private fun _read(n: Int): Long {
        var ret = 0
        (0..n - 1).forEach {
            val b = raf.get().toInt()
            ret = ret or ((b and 0xFF) shl (8 * it))
        }
        return ret.toLong()
    }

    fun read(b: ByteArray) = raf.get(b)

    var offset: Int
        get() = raf.position()
        set(offset) {
            raf.position(offset)
        }

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
        raf.put(s.toByteArray())
        writeByte(0)
    }

    private fun _write(n: Int, v: Int) = (0..n - 1).forEach { raf.put(((v.toLong() ushr (it * 8)) and 255).toByte()) }

    fun write(b: ByteArray) = raf.put(b)

    fun writeByte(v: Int) = _write(1, v)

    fun writeShort(v: Int) = _write(2, v)

    fun writeInt(v: Int) = _write(4, v)
}
