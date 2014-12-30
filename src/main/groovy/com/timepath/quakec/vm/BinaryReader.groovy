package com.timepath.quakec.vm

import groovy.transform.CompileStatic

@CompileStatic
class BinaryReader {

    BinaryReader(RandomAccessFile raf) {
        this.raf = raf
    }

    private RandomAccessFile raf

    private long _read(int n) {
        int ret = 0
        for (int i = 0; i < n; i++) {
            ret |= ((raf.read() & 0xFF) << (8 * i))
        }
        return ret
    }

    int read(byte[] b) {
        raf.read(b)
    }

    long getOffset() { raf.filePointer }

    void setOffset(long offset) { raf.seek(offset) }

    boolean readBoolean() throws IOException { _read 1 }

    byte readByte() throws IOException { _read 1 }

    int readUnsignedByte() throws IOException { _read(1) & 0xFF }

    short readShort() throws IOException { _read 2 }

    int readUnsignedShort() throws IOException { _read(2) & 0xFFFF }

    char readChar() throws IOException { _read 2 }

    int readInt() throws IOException { _read 4 }

    long readUnsignedInt() throws IOException { _read(4) & 0xFFFFFFFFL }

    long readLong() throws IOException { _read 8 }

    float readFloat() throws IOException { Float.intBitsToFloat readInt() }

    double readDouble() throws IOException { Double.longBitsToDouble readLong() }
}
