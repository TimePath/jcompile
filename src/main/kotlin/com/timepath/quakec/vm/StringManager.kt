package com.timepath.quakec.vm

import java.util.ArrayList

class StringManager(
        /**
         * Map of addresses to strings
         */
        val constant: Map<Int, String>,
        /**
         * The largest constant key + its length
         */
        val constantSize: Int) {

    val temp: List<String> = ArrayList(512)
    val zone: List<String> = ArrayList(512)

    fun get(index: Int): String {
        if (index >= 0) {
            if (index < constantSize)
                return constant[index]!!
            val zoneIndex = index - constantSize
            if (zoneIndex < zone.size())
                return zone[zoneIndex]
        } else {
            val tempIndex = index.inv()
            if (tempIndex < temp.size()) {
                return temp.get(tempIndex)
            }
        }
        return ""
    }

}
