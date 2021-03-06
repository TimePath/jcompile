package com.timepath.q1vm

import com.timepath.q1vm.util.set
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*

class EntityManager(val data: ProgramData) {

    private val entities = ArrayList<Entity?>().apply { add(Entity()) }
    private val entitySize = data.header.entityFields

    inner class Entity {
        val byte: ByteBuffer = ByteBuffer.allocateDirect(4 * entitySize)

        val int: IntBuffer by lazy(LazyThreadSafetyMode.NONE) {
            byte.asIntBuffer()
        }
        val float: FloatBuffer by lazy(LazyThreadSafetyMode.NONE) {
            byte.asFloatBuffer()
        }

        override fun equals(other: Any?): Boolean{
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Entity

            if (byte != other.byte) return false

            return true
        }

        override fun hashCode(): Int{
            return byte.hashCode()
        }
    }

    fun checkBounds(self: Int, field: Int): Unit = when {
        self == 0 ->
            throw IllegalStateException("Assignment to world")
        self !in (0..entities.size - 1) ->
            throw IndexOutOfBoundsException("Entity is out of bounds")
        field !in (0..entitySize - 1) ->
            throw IndexOutOfBoundsException("Field is out of bounds")
    }

    fun readFloat(self: Int, field: Int): Float {
        checkBounds(self, field)
        return entities[self]!!.float.get(field)
    }

    fun readVector(self: Int, field: Int): Array<Float> {
        checkBounds(self, field)
        val x = entities[self]!!.float.get(field + 0)
        val y = entities[self]!!.float.get(field + 1)
        val z = entities[self]!!.float.get(field + 2)
        return arrayOf(x, y, z)
    }

    fun readInt(self: Int, field: Int): Int {
        checkBounds(self, field)
        return entities[self]!!.int.get(field)
    }

    fun getAddress(self: Int, field: Int): Int {
        checkBounds(self, field)
        return (self * entitySize) + field
    }

    fun writeFloat(address: Int, value: Float) {
        val self = address / entitySize
        val field = address % entitySize
        checkBounds(self, field)
        entities[self]!!.float[field] = value
    }

    fun writeVector(address: Int, value: Array<Float>) {
        val self = address / entitySize
        val field = address % entitySize
        checkBounds(self, field)
        val float = entities[self]!!.float
        val (x, y, z) = value
        float[field + 0] = x
        float[field + 1] = y
        float[field + 2] = z
    }

    fun writeInt(address: Int, value: Float) {
        val self = address / entitySize
        val field = address % entitySize
        checkBounds(self, field)
        entities[self]!!.float[field] = value
    }

    fun spawn(): Int {
        val s = entities.size
        entities.add(Entity())
        return s
    }

    fun kill(self: Int) {
        entities[self] = null
    }

}
