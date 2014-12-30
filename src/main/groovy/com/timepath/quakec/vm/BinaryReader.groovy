package com.timepath.quakec.vm

import groovy.transform.CompileStatic

@CompileStatic
class BinaryReader {

    BinaryReader(RandomAccessFile raf) {
        this.raf = raf
    }

    private RandomAccessFile raf

    private long read(int n) {
        int ret = 0
        for (int i = 0; i < n; i++) {
            ret |= ((raf.read() & 0xFF) << (8 * i))
        }
        return ret
    }

    long getOffset() { raf.filePointer }

    void setOffset(long offset) { raf.seek(offset) }

    boolean readBoolean() throws IOException { read 1 }

    byte readByte() throws IOException { read 1 }

    int readUnsignedByte() throws IOException { read(1) & 0xFF }

    short readShort() throws IOException { read 2 }

    int readUnsignedShort() throws IOException { read(2) & 0xFFFF }

    char readChar() throws IOException { read 2 }

    int readInt() throws IOException { read 4 }

    long readUnsignedInt() throws IOException { read(4) & 0xFFFFFFFFL }

    long readLong() throws IOException { read 8 }

    float readFloat() throws IOException { Float.intBitsToFloat readInt() }

    double readDouble() throws IOException { Double.longBitsToDouble readLong() }
}
