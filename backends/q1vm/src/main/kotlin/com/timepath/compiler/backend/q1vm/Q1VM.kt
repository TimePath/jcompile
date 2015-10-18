package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.api.Backend
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.impl.AllocatorImpl
import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.backend.q1vm.visitors.EvaluateVisitor
import com.timepath.compiler.backend.q1vm.visitors.GeneratorVisitor
import com.timepath.compiler.backend.q1vm.visitors.ReduceVisitor
import com.timepath.compiler.backend.q1vm.visitors.TypeVisitor
import com.timepath.compiler.ir.IR
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Types
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.types.defaults.sizeOf
import com.timepath.compiler.types.defaults.struct_t

@Suppress("NOTHING_TO_INLINE") inline fun Expression.evaluate(state: Q1VM.State) = accept(state.evaluateVisitor)
@Suppress("NOTHING_TO_INLINE") inline fun Expression.reduce(state: Q1VM.State) = accept(state.reduceVisitor)
@Suppress("NOTHING_TO_INLINE") inline fun Expression.type(state: Q1VM.State) = accept(state.typeVisitor)

public class Q1VM(opts: CompilerOptions = CompilerOptions()) : Backend<Q1VM.State, Sequence<List<Expression>>, Generator.ASM> {

    override val state = State(opts)
    override fun compile(roots: Sequence<List<Expression>>) = Generator(state).generate(roots.flatMap { it.asSequence() }.toList())

    init {
        state.symbols.push("<builtin>")
        state.symbols.declare(DeclarationExpression("false", bool_t, 0.expr()))
        state.symbols.declare(DeclarationExpression("true", bool_t, 1.expr()))
        state.symbols.declare(DeclarationExpression("VA_ARGS", function_t(void_t, listOf(int_t))))
        state.symbols.push("<global>")
    }

    interface FieldCounter {
        val map: Map<Pair<String, struct_t>, Int>
        operator fun get(owner: struct_t, name: String): Expression
        fun size(): Int
    }

    class State(val opts: CompilerOptions) : CompileState() {

        @Suppress("NOTHING_TO_INLINE") inline
        fun Expression.generate() = accept(generatorVisitor)

        val evaluateVisitor = EvaluateVisitor(this)
        val generatorVisitor = GeneratorVisitor(this)
        val reduceVisitor = ReduceVisitor(this)
        val typeVisitor = TypeVisitor(this)
        val allocator = AllocatorImpl(opts)

        val fields = object : FieldCounter {
            override val map: MutableMap<Pair<String, struct_t>, Int> = linkedMapOf()
            var counter = 0
            override fun get(owner: struct_t, name: String): Expression {
                val type = owner.fields[name]
                check(type != null) {
                    "Can't find field $name in $owner"
                }
                type!!
                map.getOrPut(name to owner) {
                    val field = counter
                    counter += type.sizeOf()
                    field
                }
                return Pointer(owner.offsetOf(name)).expr(name, field_t(type))
            }

            override fun size() = map.size()
        }

        init {
            Types.types[@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") java.lang.Boolean::class.java] = bool_t
            Types.types[@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") java.lang.Character::class.java] = int_t
            Types.types[@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") java.lang.Integer::class.java] = int_t
            Types.types[@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") java.lang.Float::class.java] = float_t
            Types.types[String::class.java] = string_t
            Types.types[Vector::class.java] = vector_t
            Types.types[Pointer::class.java] = int_t

            Types.handlers.add {
                if (it.op != ",") null else
                    Operation.Handler.Binary<Q1VM.State, List<IR>>(it.right!!) { left, right ->
                        linkedListOf<IR>() apply {
                            addAll(left.generate())
                            addAll(right.generate())
                        }
                    }
            }
            Types.handlers.add { it.left.handle(it) }
            Types.handlers.add { void_t.handle(it.copy(left = void_t, right = void_t)) }
            function_t.handlers.add {
                when (it) {
                    Operation("=", this, this) ->
                        DefaultHandlers.Assign(this, Instruction.STORE[function_t::class.java])
                    Operation("==", this, this) ->
                        DefaultHandlers.Binary(bool_t, Instruction.EQ[function_t::class.java])
                    Operation("!=", this, this) ->
                        DefaultHandlers.Binary(bool_t, Instruction.NE[function_t::class.java])
                    Operation("!", this) ->
                        DefaultHandlers.Unary(bool_t, Instruction.NOT[function_t::class.java])
                    Operation("&", this) ->
                        Operation.Handler.Unary<Q1VM.State, List<IR>>(int_t) {
                            val gen = it.generate()
                            (MemoryReference(gen.last().ret, int_t) / Pointer(1).expr()).generate()
                        }
                    else -> null
                }
            }

            types["void"] = void_t
            types["float"] = float_t
            types["vector"] = vector_t
            types["string"] = string_t
            types["entity"] = entity_t
            types["int"] = int_t
            types["bool"] = bool_t
        }
    }

}
