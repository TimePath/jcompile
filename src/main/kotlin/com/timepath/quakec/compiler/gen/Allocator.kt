package com.timepath.quakec.compiler.gen

import java.util.HashMap
import java.util.LinkedHashMap
import java.util.Stack
import java.util.regex.Pattern
import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.quote
import com.timepath.quakec.compiler.ast.Value

class Allocator {

    class object {
        val logger = Logging.new()
    }

    /**
     * Maps names to pointers
     */
    class AllocationMap {
        internal val references = LinkedHashMap<String, Int>()
        internal val reverse = LinkedHashMap<Int, String>()
        internal val values = LinkedHashMap<Int, Value>()

        fun contains(s: String) = s in references

        fun get(s: String): Int? = references[s]
        fun set(s: String, i: Int) {
            references[s] = i
            reverse[i] = s
        }

        fun get(i: Int): Value? = values[i]
        fun set(i: Int, value: Value) {
            values[i] = value
        }

        fun keySet(): MutableSet<String> = references.keySet()

        fun size() = references.size()

    }

    val functions = AllocationMap()
    val references = AllocationMap()
    val constants = AllocationMap()
    val strings = AllocationMap()

    data class Scope(val lookup: MutableMap<String, Int> = HashMap())

    val scope = Stack<Scope>()

    fun push() {
        scope.push(Scope())
    }

    fun pop() {
        scope.pop()
    }

    {
        push()
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
            if (name in it.lookup) {
                return true
            }
            val vecName = vecName(name)
            if (vecName != null) {
                if (vecName in it.lookup) {
                    return true
                }
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
            val vecName = vecName(name)
            if (vecName != null) {
                val j = it.lookup[vecName]
                if (j != null) {
                    return j
                }
            }
        }
        return null
    }

    private inline fun allocate(map: AllocationMap, id: String, index: Int, onCreate: () -> Unit): Int {
        val existing = map[id]
        if (existing != null) return existing
        val i = index
        map[id] = i
        onCreate()
        return i
    }

    var counter: Int = 100

    /**
     * Return the index to a constant referring to this function
     */
    fun allocateFunction(id: String? = null): Int {
        val name = id ?: "fun$counter"
        val i = functions.size()
        // index the function will have
        val idx = allocate(functions, name, i, {})
        val constPtr = allocateConstant(Value(idx), name)
        scope.peek().lookup[name] = constPtr
        return constPtr
    }

    /**
     * Reserve space for this variable and add its name to the current scope
     * Return the index in memory
     */
    fun allocateReference(id: String? = null): Int {
        val name = id ?: "ref$counter"
        val i = counter
        return allocate(references, name, i) {
            scope.peek().lookup[name] = i
            counter++
        }
    }

    /**
     * Reserve space for this constant
     * Return the index in memory
     */
    fun allocateConstant(value: Value, id: String? = null): Int {
        if (value.value is String) return allocateString(value.value)
        val name = id ?: "val$counter"
        val i = counter
        return allocate(constants, name, i) {
            constants[i] = value
            counter++
        }
    }

    private var stringOffset = 0

    fun allocateString(s: String): Int {
        val name = s
        val i = stringOffset
        return allocate(strings, name, i) {
            stringOffset += name.length() + 1
        }
    }

    override fun toString(): String {
        val functions = functions.reverse.map { "${it.key}\t${it.value}" }.join("\n")
        val references = references.reverse.map { "$${it.key}\t(${references[it.key]})\t${it.value}" }.join("\n")
        val constants = constants.reverse.map { "$${it.key}\t(${constants[it.key]})\t${it.value}" }.join("\n")
        val strings = strings.reverse.map { "$${it.key}\t${it.value.quote()}" }.join("\n")
        return "functions:\n" + functions +
                "\n\n" +
                "references:\n" + references +
                "\n\n" +
                "constants:\n" + constants +
                "\n\n" +
                "strings:\n" + strings
    }

}
