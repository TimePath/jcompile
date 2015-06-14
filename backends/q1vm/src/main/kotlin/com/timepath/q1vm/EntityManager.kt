package com.timepath.q1vm

import com.timepath.q1vm.util.set
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.ArrayList
import kotlin.properties.Delegates

private class EntityManager(val data: ProgramData) {

    private val entities = ArrayList<Entity?>()
    private val entitySize = data.header.entityFields

    inner data class Entity {
        val byte: ByteBuffer = ByteBuffer.allocateDirect(4 * entitySize)

        val int: IntBuffer by Delegates.lazy {
            byte.asIntBuffer()
        }
        val float: FloatBuffer by Delegates.lazy {
            byte.asFloatBuffer()
        }
    }

    fun checkBounds(self: Int, field: Int) {
        when {
            self !in (0..entities.size() - 1) -> {
                throw IndexOutOfBoundsException("Entity is out of bounds")
            }
            field !in (0..entitySize - 1) -> {
                throw IndexOutOfBoundsException("Field is out of bounds")
            }
        }
    }

    fun readFloat(self: Int, field: Int): Float {
        checkBounds(self, field)
        return entities[self]!!.float[field]
    }

    fun readVector(self: Int, field: Int): Array<Float> {
        checkBounds(self, field)
        val x = entities[self]!!.float[field + 0]
        val y = entities[self]!!.float[field + 1]
        val z = entities[self]!!.float[field + 2]
        return arrayOf(x, y, z)
    }

    fun readInt(self: Int, field: Int): Int {
        checkBounds(self, field)
        return entities[self]!!.int[field]
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
        val s = entities.size()
        entities.add(Entity())
        return s
    }

    fun kill(self: Int) {
        entities[self] = null
    }

}
