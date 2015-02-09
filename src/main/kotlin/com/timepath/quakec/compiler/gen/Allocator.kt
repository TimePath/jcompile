package com.timepath.quakec.compiler.gen

import java.util.HashMap
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.Stack
import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.CompilerOptions
import com.timepath.quakec.compiler.Value
import com.timepath.quakec.compiler.gen.Allocator.AllocationMap.Entry
import com.timepath.quakec.compiler.Type

class Allocator(val opts: CompilerOptions) {

    class object {
        val logger = Logging.new()
    }

    /**
     * Maps names to pointers
     */
    inner class AllocationMap {

        inner data class Entry(
                /** Privately set */
                var name: String,
                val ref: Int,
                val value: Value,
                val type: Type) {

            fun tag(name: String) {
                if (!this.name.split('|').contains(name))
                    this.name += "|$name"
            }
        }

        private val free = LinkedList<Entry>()
        private val pool = LinkedList<Entry>()
        val all: List<Entry> = pool
        private val refs = LinkedHashMap<Int, Entry>()
        private val values = LinkedHashMap<Value, Entry>()
        private val names = LinkedHashMap<String, Entry>()

        /**
         * Considered inside a function at this depth
         */
        val insideFunc: Boolean
            get() = scope.size() >= 3

        fun allocate(id: String, ref: Int, value: Value?, type: Type): Entry {
            val valueOrDefault = value ?: Value(null)
            // only consider uninitialized local references for now
            if (opts.scopeFolding && insideFunc && !free.isEmpty() && valueOrDefault.value == null) {
                val e = free.pop()
                // add the entry to the current scope so it can be used again later on exit
                scope.peek().add(e)
                e.tag(id)
                return e
            }
            val e = Entry(id, ref, valueOrDefault, type)
            pool.add(e)
            if (!scope.empty() && valueOrDefault.value == null) {
                scope.peek().add(e)
            }
            refs[e.ref] = e
            values[e.value] = e
            names[e.name] = e
            return e
        }

        fun contains(ref: Int) = ref in refs
        fun get(ref: Int): Entry? = refs[ref]

        fun contains(value: Value) = value in values
        fun get(value: Value): Entry? = values[value]

        fun contains(name: String) = name in names
        fun get(name: String): Entry? = names[name]
        fun set(name: String, value: Entry) {
            names[name] = value
        }

        fun size() = pool.size()

        private val scope = Stack<LinkedList<Entry>>()

        fun push() {
            scope.push(LinkedList<Entry>())
        }

        /**
         * Push all previously used entries to the head of the queue
         */
        fun pop() {
            val wasInside = insideFunc
            free.addAll(0, scope.pop())
            // forget old free vars
            if (wasInside != insideFunc)
                free.clear()
        }

    }

    val functions = AllocationMap()
    val references = AllocationMap()
    val constants = AllocationMap()
    val strings = AllocationMap()

    data class Scope(val id: Any, val lookup: MutableMap<String, Entry> = HashMap())

    val scope = Stack<Scope>()

    fun push(id: Any) {
        scope.push(Scope(id))
        references.push()
    }

    fun pop() {
        if (!scope.empty())
            scope.pop()
        references.pop()
    }

    {
        push("<builtin>")
        allocateReference("_", Type.Function(Type.String, listOf(Type.String))) // TODO: not really a function
    }

    private inline fun all(operation: (Scope) -> Unit) = scope.reverse().forEach(operation)

    fun contains(name: String): Boolean {
        all {
            if (name in it.lookup) {
                return true
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
        }
        return null
    }

    private var funCounter: Int = 0

    /**
     * Return the index to a constant referring to this function
     */
    fun allocateFunction(id: String? = null, type: Type.Function): Entry {
        val name = id ?: "fun${funCounter++}"
        val i = functions.size()
        // index the function will have
        val function = functions.allocate(name, i, null, type)
        val const = allocateConstant(Value(function.ref), type, "fun($name)")
        scope.peek().lookup[name] = const
        return const
    }

    private var refCounter: Int = 0

    /**
     * Reserve space for this variable and add its name to the current scope
     */
    fun allocateReference(id: String? = null, type: Type, value: Value? = null): Entry {
        val name = id ?: "ref${refCounter++}"
        val i = opts.userStorageStart + (references.size() + constants.size())
        val entry = references.allocate(name, i, value, type)
        scope.peek().lookup[name] = entry
        return entry
    }

    /**
     * Reserve space for this constant
     */
    fun allocateConstant(value: Value, type: Type, id: String? = null): Entry {
        if (value.value is String) {
            val string = allocateString(value.value)
            return allocateConstant(Value(string.ref), Type.String, "str(${string.name})")
        }
        val name: String = when {
            id != null -> id
            else -> when (value.value) {
                is Int -> "${value.value}i"
                is Float -> "${value.value}f"
                else -> "$value"
            }
        }
        if (opts.mergeConstants) {
            constants[value]?.let { it ->
                constants[name] = it
                it.tag(name)
                return it
            }
        }
        val i = opts.userStorageStart + (references.size() + constants.size())
        return constants.allocate(name, i, value, type)
    }

    private var stringCounter = 0

    fun allocateString(s: String): Entry {
        val name = s
        // merge strings
        val existing = strings[name]
        if (existing != null) {
            return existing
        }
        val i = stringCounter
        stringCounter += name.length() + 1
        return strings.allocate(name, i, null, Type.String)
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
