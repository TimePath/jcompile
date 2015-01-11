package com.timepath.quakec.vm

import java.util.ArrayList
import java.util.LinkedHashMap

class StringManager(list: List<String>, expectedSize: Int? = null) {

    /**
     * Map of addresses to strings
     */
    val constant: Map<Int, String>

    /**
     * The largest constant key + the length of its value
     */
    private val constantSize: Int

    {
        var stroff = 0
        val strings = LinkedHashMap<Int, String>(list.size())
        for (s in list) {
            strings[stroff] = s
            stroff += s.length() + 1
        }
        constant = strings
        if (expectedSize != null) {
            val b = expectedSize == stroff
            assert(b, "String constants size mismatch")
        }
        constantSize = stroff
    }

    private val temp: MutableList<String> = ArrayList(512)
    private val zone: MutableList<String?> = ArrayList(512)

    fun get(index: Int): String? {
        if (index >= 0) {
            if (index < constantSize)
                return constant[index]!!
            val zoneIndex = index - constantSize
            if (zoneIndex < zone.size())
                return zone[zoneIndex]!!
        } else {
            val tempIndex = index.inv()
            if (tempIndex < temp.size()) {
                return temp[tempIndex]
            }
        }
        return null
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
