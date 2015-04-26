package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.api.Backend
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.compiler.backend.q1vm.data.Vector
import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.backend.q1vm.visitors.EvaluateVisitor
import com.timepath.compiler.backend.q1vm.visitors.GeneratorVisitor
import com.timepath.compiler.backend.q1vm.visitors.ReduceVisitor
import com.timepath.compiler.backend.q1vm.visitors.TypeVisitor
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.OperationHandler
import com.timepath.compiler.types.Types
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.types.defaults.struct_t
import com.timepath.getTextWS
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext
import java.util.LinkedHashMap

suppress("NOTHING_TO_INLINE") inline fun Expression.evaluate(state: Q1VM.State) = accept(state.evaluateVisitor)
suppress("NOTHING_TO_INLINE") inline fun Expression.generate(state: Q1VM.State) = accept(state.generatorVisitor)
suppress("NOTHING_TO_INLINE") inline fun Expression.reduce() = accept(ReduceVisitor)
suppress("NOTHING_TO_INLINE") inline fun Expression.type(state: Q1VM.State) = accept(state.typeVisitor)

public class Q1VM(opts: CompilerOptions = CompilerOptions()) : Backend<Q1VM.State, Generator.ASM> {

    override val state = State(opts)
    override fun generate(roots: Sequence<List<Expression>>) = state.gen.generate(roots.flatMap { it.sequence() })

    init {
        state.symbols.push("<builtin>")
        state.symbols.declare(DeclarationExpression("VA_ARGS", function_t(void_t, listOf(int_t))))
        state.symbols.declare(DeclarationExpression("false", bool_t, ConstantExpression(0)))
        state.symbols.declare(DeclarationExpression("true", bool_t, ConstantExpression(1)))
        // TODO: not really a function
        state.symbols.declare(DeclarationExpression("_", function_t(string_t, listOf(string_t))))
        state.symbols.push("<global>")
    }

    trait FieldCounter {
        fun get(type: struct_t, name: String): ConstantExpression
    }

    class Err(val ctx: ParserRuleContext, val reason: String) {
        private val token = ctx.start
        val file = token.getTokenSource().getSourceName()
        val line = token.getLine()
        val col = token.getCharPositionInLine()
        val code = ctx.getTextWS()
    }

    inner class State(val opts: CompilerOptions) : CompileState() {
        val errors = linkedListOf<Err>()
        val evaluateVisitor = EvaluateVisitor(this)
        val generatorVisitor = GeneratorVisitor(this)
        val typeVisitor = TypeVisitor(this)
        val allocator: Allocator = Allocator.new(opts)
        val gen: Generator = Generator.new(this)

        val fields: FieldCounter = object : FieldCounter {
            val map: MutableMap<String, Int> = LinkedHashMap()
            override fun get(type: struct_t, name: String) = ConstantExpression(Pointer(map.getOrPut(name) { map.size() }))
        }

        init {
            Types.types[javaClass<java.lang.Boolean>()] = bool_t
            Types.types[javaClass<java.lang.Character>()] = int_t
            Types.types[javaClass<java.lang.Integer>()] = int_t
            Types.types[javaClass<java.lang.Float>()] = float_t
            Types.types[javaClass<String>()] = string_t
            Types.types[javaClass<Vector>()] = vector_t
            Types.types[javaClass<Pointer>()] = int_t

            Types.handlers.add {
                if (it.op != ",") null else
                    OperationHandler(it.right!!) { gen: State, left, right ->
                        with(linkedListOf<IR>()) {
                            addAll(left.generate(gen))
                            addAll(right!!.generate(gen))
                            this
                        }
                    }
            }
            Types.handlers.add { it.left.handle(it) }
            Types.handlers.add { void_t.handle(it.copy(left = void_t, right = void_t)) }
            function_t.handlers.add {
                val ops = mapOf(
                        Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FUNC),
                        Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_FUNC),
                        Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_FUNC),
                        Operation("!", this) to DefaultUnaryHandler(bool_t, Instruction.NOT_FUNC),
                        Operation("&", this) to OperationHandler(float_t) { gen, self, _ ->
                            BinaryExpression.Divide(MemoryReference(self.generate(gen).last().ret, float_t), ConstantExpression(Pointer(1))).generate(gen)
                        }
                )
                ops[it]
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
