package com.timepath.quakec.compiler.gen

import java.util.HashMap
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Stack
import java.util.regex.Pattern
import com.timepath.quakec.Logging

class Allocator {

    class object {
        val logger = Logging.new()
    }

    val scope = Stack<Scope>()
    var counter: Int = 100
    private val reverse: MutableMap<Int, String> = LinkedHashMap()
    val constants: MutableMap<Int, Any> = HashMap()
    val strings = LinkedHashSet<String>()
    private var stringOffset = 0

    {
        push()
        allocateReference("_")
    }

    data class Scope(val lookup: MutableMap<String, Int> = HashMap())

    fun push() {
        scope.push(Scope())
    }

    fun pop() {
        scope.pop()
    }

    private fun vecName(name: String): String? {
        val vec = Pattern.compile("(.+)_[xyz]$")
        val matcher = vec.matcher(name)
        if (matcher.matches()) {
            return matcher.group(1)
        }
        return null
    }

    private inline fun all(operation: (Scope) -> Unit) = scope.reverse().forEach(operation)

    fun contains(name: String): Boolean {
        all {
            if (it.lookup.containsKey(name)) {
                return true
            }
            if (it.lookup.containsKey(vecName(name))) {
                return true
            }
        }
        return false
    }

    fun get(name: String): Int? {
        all {
            val i = it.lookup[name]
            if (i != null) {
                return i
            }
            val j = it.lookup[vecName(name)]
            if (j != null) {
                return j
            }
        }
        return null
    }

    fun allocateReference(id: String? = null): Int {
        val name = id ?: "ref$counter"
        val existing = this[name]
        if (existing != null) return existing
        val i = counter++
        reverse[i] = name
        constants[i] = 0
        scope.peek().lookup[name] = i
        return i
    }

    fun allocateConstant(value: Any): Int {
        val name = "const$counter"
        val existing = this[name]
        if (existing != null) return existing
        val i = counter++
        reverse[i] = name
        constants[i] = value
        return i
    }

    fun allocateString(s: String): Int {
        val pointer = stringOffset
        strings.add(s)
        stringOffset += s.length() + 1
        return pointer
    }

    override fun toString() = reverse.map { "${it.key}\t${it.value}\t${constants[it.key]}" }.join("\n")

}
