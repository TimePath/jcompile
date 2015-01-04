package com.timepath.quakec.vm

import java.util.ArrayList
import java.util.LinkedHashMap

class StringManager(val constant: LinkedHashMap<Int, String>, val constantSize: Int) {

    val temp: ArrayList <String> = ArrayList(512)
    val zone: ArrayList <String> = ArrayList(512)

    fun get(index: Int): String {
        if (index >= 0) {
            if (index < constantSize)
                return constant[index]
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
