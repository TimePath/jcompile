package com.timepath.quakec.ast

import java.util.LinkedHashMap


class GenerationContext {

    val registry: Registry = Registry()

    inner class Registry {

        var counter: Int = 100
        val values: MutableMap<Int, Any> = LinkedHashMap()
        val reverse: MutableMap<Int, String> = LinkedHashMap()
        val lookup: MutableMap<String, Int> = LinkedHashMap()

        fun contains(name: String): Boolean = lookup.containsKey(name)

        fun get(name: String): Int = lookup[name] ?: 0

        fun register(name: String?, value: Any? = null): Int {
            val n = name ?: "var$counter"
            val existing = lookup[n]
            if (existing != null) return existing
            val i = counter++
            if (value != null)
                values[i] = value
            reverse[i] = n
            lookup[n] = i
            return i
        }

        override fun toString() = reverse.map { "${it.key}\t${it.value}\t${values[it.key]}" }.join("\n")

    }
}