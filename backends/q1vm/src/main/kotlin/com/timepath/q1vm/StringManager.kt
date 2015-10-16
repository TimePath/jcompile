package com.timepath.q1vm

import com.timepath.with
import java.util.Scanner

class StringManager(list: Collection<String>,
                    expectedSize: Int? = null) {

    /**
     * Giant string
     */
    val constant: String

    /**
     * The largest constant key + the length of its value
     */
    private val constantSize: Int

    init {
        constant = StringBuilder {
            for (s in list) {
                append(s + "\u0000")
            }
        }.toString()
        val stroff = constant.length()
        if (expectedSize != null) {
            val b = expectedSize == stroff
            assert(b, "String constants size mismatch")
        }
        constantSize = stroff
    }

    private val temp: MutableList<String> = arrayListOf()
    private val zone: MutableList<String?> = arrayListOf()

    operator fun get(index: Int): String {
        if (index >= 0) {
            if (index < constantSize)
                return Scanner(constant.substring(index)).with {
                    useDelimiter("\u0000")
                }.next()
            val zoneIndex = index - constantSize
            if (zoneIndex < zone.size())
                return zone[zoneIndex]!!
        } else {
            val tempIndex = index.inv()
            if (tempIndex < temp.size()) {
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
                return constantSize + i
            }
        }
        val size = zone.size()
        zone.add(string)
        return constantSize + size
    }

    fun unzone(index: Int): Boolean {
        if (index < constantSize) return false
        val zoneIndex = index - constantSize
        if (zoneIndex >= zone.size()) return false
        if (zone[zoneIndex] == null) return false
        zone[zoneIndex] = null
        return true
    }

    fun tempString(string: String): Int {
        val size = temp.size()
        temp.add(string)
        return size.inv()
    }

    fun clearTempStrings() = temp.clear()

}
