package com.timepath.quakec.compiler.gen

import java.util.HashMap
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.Stack
import java.util.regex.Pattern
import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.ast.Value
import com.timepath.quakec.compiler.gen.Allocator.AllocationMap.Entry

class Allocator {

    class object {
        val logger = Logging.new()
    }

    /**
     * Maps names to pointers
     */
    class AllocationMap {

        internal data class Entry(var ref: Int,
                                  var value: Value,
                                  var name: String)

        val all = LinkedList<Entry>()
        internal val refs = LinkedHashMap<Int, Entry>()
        internal val values = LinkedHashMap<Value, Entry>()
        internal val names = LinkedHashMap<String, Entry>()

        fun add(e: Entry) {
            all.add(e)
            refs[e.ref] = e
            values[e.value] = e
            names[e.name] = e
        }

        fun contains(i: Int) = i in refs
        fun get(i: Int): Entry? = refs[i]

        fun contains(v: Value) = v in values
        fun get(v: Value): Entry? = values[v]

        fun contains(s: String) = s in names
        fun get(s: String): Entry? = names[s]

        fun size() = names.size()

    }

    val functions = AllocationMap()
    val references = AllocationMap()
    val constants = AllocationMap()
    val strings = AllocationMap()

    data class Scope(val lookup: MutableMap<String, Entry> = HashMap())

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

    fun get(name: String): Entry? {
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

    private inline fun allocate(map: AllocationMap, id: String, index: Int, onCreate: () -> Value?): Entry {
        val existing = map[id]
        if (existing != null) return existing
        val i = index
        val entry = Entry(i, onCreate() ?: Value(null), id)
        map.add(entry)
        return entry
    }

    var counter: Int = 100

    /**
     * Return the index to a constant referring to this function
     */
    fun allocateFunction(id: String? = null): Entry {
        val name = id ?: "fun$counter"
        val i = functions.size()
        // index the function will have
        val function = allocate(functions, name, i, { null })
        val const = allocateConstant(Value(function.ref), "fun($name)")
        scope.peek().lookup[name] = const
        return const
    }

    /**
     * Reserve space for this variable and add its name to the current scope
     * Return the index in memory
     */
    fun allocateReference(id: String? = null): Entry {
        val name = id ?: "ref$counter"
        val i = counter
        val entry = allocate(references, name, i) {
            counter++
            null
        }
        scope.peek().lookup[name] = entry
        return entry
    }

    /**
     * Reserve space for this constant
     * Return the index in memory
     */
    fun allocateConstant(value: Value, id: String? = null): Entry {
        if (value.value is String) {
            val string = allocateString(value.value)
            return allocateConstant(Value(string.ref), "str(${string.name})")
        }
        val name: String = when {
            id != null -> id
            else -> when (value.value) {
                is Int -> "${value.value}i"
                is Float -> "${value.value}f"
                else -> "$value"
            }
        }
        // merge constants
        val existing = constants.values[value]
        if (existing != null) {
            constants.names[name] = existing
            if (!existing.name.split('|').contains(name))
                existing.name += "|$name"
            return existing
        }
        val i = counter
        return allocate(constants, name, i) {
            counter++
            value
        }
    }

    private var stringOffset = 0

    fun allocateString(s: String): Entry {
        val name = s
        val i = stringOffset
        return allocate(strings, name, i) {
            stringOffset += name.length() + 1
            null
        }
    }

    override fun toString(): String {
        val constants = constants.all.map { it.toString() }.join("\n")
        val functions = functions.all.map { it.toString() }.join("\n")
        val strings = strings.all.map { it.toString() }.join("\n")
        val references = references.all.map { it.toString() }.join("\n")
        return "constants:\n" + constants +
                "\n\n" +
                "functions:\n" + functions +
                "\n\n" +
                "strings:\n" + strings +
                "\n\n" +
                "references:\n" + references
    }

}
