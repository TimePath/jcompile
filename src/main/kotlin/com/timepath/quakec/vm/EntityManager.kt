package com.timepath.quakec.vm

class EntityManager(val data: ProgramData) {

    fun readFloat(self: Int, field: Int): Float {
        return 0f
    }

    fun readVector(self: Int, field: Int): Array<Float> {
        return array(0f, 0f, 0f)
    }

    fun readInt(self: Int, field: Int): Int {
        return 0
    }

    fun getAddress(self: Int, field: Int): Int {
        return 0
    }

    fun writeFloat(address: Int, value: Float) {

    }

    fun writeVector(address: Int, value: Array<Float>) {

    }

    fun writeInt(address: Int, value: Float) {

    }

}