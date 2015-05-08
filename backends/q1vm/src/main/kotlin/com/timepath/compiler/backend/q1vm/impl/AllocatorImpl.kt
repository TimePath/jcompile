package com.timepath.compiler.backend.q1vm.impl

import com.timepath.compiler.Value
import com.timepath.compiler.backend.q1vm.Allocator
import com.timepath.compiler.backend.q1vm.CompilerOptions
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.compiler.backend.q1vm.types.bool_t
import com.timepath.compiler.backend.q1vm.types.string_t
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import com.timepath.quote
import com.timepath.with
import java.util.*

class AllocatorImpl(val opts: CompilerOptions) : Allocator {

    /**
     * Maps names to pointers
     */
    inner class AllocationMapImpl : Allocator.AllocationMap {

        inner data class EntryImpl(
                /** Privately set */
                override var name: String,
                override val ref: Int,
                override val value: Value?,
                override val type: Type) : Allocator.AllocationMap.Entry {

            val separator = '|'
            val tags = name.split(separator).toMutableSet()

            fun tag(tag: String) {
                if (tag !in tags) {
                    tags.add(tag)
                    name += separator + tag
                }
            }
        }

        private val free = LinkedList<EntryImpl>()
        private val pool = LinkedList<EntryImpl>()
        override val all: List<EntryImpl> = pool
        private val refs = LinkedHashMap<Int, EntryImpl>()
        private val values = LinkedHashMap<Value, EntryImpl>()
        private val names = LinkedHashMap<String, Allocator.AllocationMap.Entry>()

        /**
         * Considered inside a function at this depth
         */
        val insideFunc: Boolean
            get() = scope.size() >= 3

        fun allocate(id: String, ref: Int, value: Value?, type: Type): EntryImpl {
            // only consider uninitialized local references for now
            if (opts.scopeFolding && insideFunc && !free.isEmpty() && value == null) {
                val e = free.pop()
                // add the entry to the current scope so it can be used again later on exit
                scope.peek().add(e)
                e.tag(id)
                return e
            }
            val e = EntryImpl(id, ref, value, type)
            pool.add(e)
            if (!scope.empty() && value == null) {
                scope.peek().add(e)
            }
            refs[e.ref] = e
            values[e.value] = e
            names[e.name] = e
            return e
        }

        override fun contains(ref: Int) = ref in refs
        override fun get(ref: Int): EntryImpl? = refs[ref]

        override fun contains(value: Value) = value in values
        override fun get(value: Value): EntryImpl? = values[value]

        override fun contains(name: String) = name in names
        override fun get(name: String): Allocator.AllocationMap.Entry? = names[name]
        override fun set(name: String, value: Allocator.AllocationMap.Entry) {
            names[name] = value
        }

        override fun size() = pool.size()

        private val scope = Stack<LinkedList<EntryImpl>>()

        fun push() {
            scope.push(LinkedList<EntryImpl>())
        }

        /**
         * Push all previously used entries to the head of the queue
         */
        fun pop() {
            val wasInside = insideFunc
            free.addAll(0, scope.pop())
            if (!opts.overlapLocals && wasInside != insideFunc) {
                free.clear()
            }
        }
    }

    override val functions = AllocationMapImpl()
    override val references = AllocationMapImpl()
    override val constants = AllocationMapImpl()
    override val strings = AllocationMapImpl()

    data class Scope(override val id: Any, override val lookup: MutableMap<String, Allocator.AllocationMap.Entry> = HashMap()) : Allocator.Scope

    override val scope: Deque<Allocator.Scope> = LinkedList()

    override fun push(id: Any) {
        scope.push(Scope(id))
        references.push()
    }

    override fun pop() {
        scope.pop()
        references.pop()
    }

    init {
        push("<builtin>")
        allocateReference("false", bool_t, Value(0))
        allocateReference("true", bool_t, Value(1))
        allocateReference("_", function_t(string_t, listOf(string_t))) // TODO: not really a function
    }

    override fun contains(name: String) = scope.firstOrNull { name in it.lookup } != null
    override fun get(name: String): Allocator.AllocationMap.Entry? {
        scope.forEach {
            it.lookup[name]?.let {
                return it
            }
        }
        return null
    }

    /**
     * Return the index to a constant referring to this function
     */
    override fun allocateFunction(id: String, type: function_t): Allocator.AllocationMap.Entry {
        val function = functions.allocate(id, functions.size(), null, type)
        // Allocate a constant so the function can be called
        return allocateConstant(Value(Pointer(function.ref)), type, id).with {
            scope.peek().lookup[id] = this
        }
    }

    private var refCounter = 0

    /**
     * Reserve space for this variable and add its name to the current scope
     */
    override fun allocateReference(id: String?, type: Type, value: Value?): Allocator.AllocationMap.Entry {
        val name = id ?: "var${refCounter++}"
        val i = opts.userStorageStart + (references.size() + constants.size())
        val entry = references.allocate(name, i, value, type)
        scope.peek().lookup[name] = entry
        return entry
    }

    /**
     * Reserve space for this constant
     */
    override fun allocateConstant(value: Value, type: Type, id: String): Allocator.AllocationMap.Entry {
        if (value.any is String) {
            val str = allocateString(value.any)
            return allocateConstant(Value(Pointer(str.ref)), string_t, str.name.quote())
        }
        if (opts.mergeConstants) {
            constants[value]?.let {
                constants[id] = it
                it.tag(id)
                return it
            }
        }
        val i = opts.userStorageStart + (references.size() + constants.size())
        return constants.allocate(id, i, value, type)
    }

    private var stringCounter = 0

    override fun allocateString(s: String): Allocator.AllocationMap.Entry {
        // merge strings
        val existing = strings[s]
        if (existing != null) {
            return existing
        }
        val i = stringCounter
        stringCounter += s.length() + 1
        return strings.allocate(s, i, null, string_t)
    }

    override fun toString(): String {
        val constants = constants.all.joinToString("\n")
        val functions = functions.all.joinToString("\n")
        val strings = strings.all.joinToString("\n")
        val references = references.all.joinToString("\n")
        return "constants:\n" + constants +
                "\n\n" +
                "functions:\n" + functions +
                "\n\n" +
                "strings:\n" + strings +
                "\n\n" +
                "references:\n" + references
    }

}
