package com.timepath.q1vm

import java.util.*

class StringManager(list: Collection<String>,
                    expectedSize: Int? = null) {

    /**
     * String buffer
     */
    val constant: ByteArray

    init {
        constant = buildString {
            append(0)
            for (s in list) {
                append(s)
                append(0.toChar())
            }
        }.toByteArray(Charsets.US_ASCII)
        if (expectedSize != null) {
            val b = expectedSize == constant.size
            assert(b) { "String constants size mismatch" }
        }
    }

    private val temp: MutableList<String> = arrayListOf()
    private val zone: MutableList<String?> = arrayListOf()

    operator fun get(index: Int): String {
        if (index >= 0) {
            if (index < constant.size)
                return Scanner(String(constant).substring(index)).apply {
                    useDelimiter("\u0000")
                }.next()
            val zoneIndex = index - constant.size
            if (zoneIndex < zone.size)
                return zone[zoneIndex]!!
        } else {
            val tempIndex = index.inv()
            if (tempIndex < temp.size) {
                return temp[tempIndex]
            }
        }
        return "<invalid string ($index)>"
    }

    fun zone(string: String): Int {
        // avoid shuffling by reusing
        zone.forEachIndexed { i, it ->
            if (it == null) {
                zone[i] = string
                return constant.size + i
            }
        }
        val size = zone.size
        zone.add(string)
        return constant.size + size
    }

    fun unzone(index: Int): Boolean {
        if (index < constant.size) return false
        val zoneIndex = index - constant.size
        if (zoneIndex >= zone.size) return false
        if (zone[zoneIndex] == null) return false
        zone[zoneIndex] = null
        return true
    }

    fun tempString(string: String): Int {
        val size = temp.size
        temp.add(string)
        return size.inv()
    }

    fun clearTempStrings() = temp.clear()

}
