package com.timepath.compiler.backend.q1vm.impl

import com.timepath.Logger
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.compiler.types.defaults.function_t
import com.timepath.q1vm.ProgramData
import com.timepath.q1vm.StringManager
import com.timepath.with
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeneratorImpl(val state: Q1VM.State) : Generator {

    companion object {
        val logger = Logger()
    }

    override fun generate(roots: List<Expression>): Generator.ASM {
        for (root in roots) {
            root.transform { it.reduce() }
        }
        state.allocator.push("<forward declarations>")
        val allocate = object : ASTVisitor<Unit> {
            override fun visit(e: FunctionExpression) {
                if (e.id in state.allocator) {
                    logger.warning { "redeclaring ${e.id}" }
                }
                state.allocator.allocateFunction(e.id, type = e.type(state) as function_t)
            }

            override fun visit(e: DeclarationExpression) {
                if (e.id in state.allocator) {
                    logger.warning { "redeclaring ${e.id}" }
                }
                state.allocator.allocateReference(e.id, e.type(state), e.value?.evaluate(state))
            }
        }
        roots.forEach { it.accept(allocate) }
        return ASMImpl(BlockExpression(roots).accept(state.generatorVisitor)) with {
            state.allocator.pop()
        }
    }

    inner class ASMImpl(override val ir: List<IR>) : Generator.ASM {

        /**
         * Ought to be enough, instructions can't address beyond this range anyway
         */
        val globalData = ByteBuffer.allocate(4 * 0xFFFF).order(ByteOrder.LITTLE_ENDIAN)
        val intData = globalData.asIntBuffer()
        val floatData = globalData.asFloatBuffer()

        /**
         * FIXME: metadata
         */
        override fun generateProgs(): ProgramData {
            val fieldDefs = arrayListOf<ProgramData.Definition>() with {
                for ((s, idx) in state.fields.map) {
                    val e = state.allocator.allocateString(s)
                    add(ProgramData.Definition(0, idx.toShort(), e.ref))
                }
            }
            val statements = arrayListOf<ProgramData.Statement>()
            val functions = arrayListOf<ProgramData.Function>()
            ir.forEach {
                if (it is IR.Function) {
                    if (it.function.firstStatement < 0) {
                        functions.add(it.function)
                    } else {
                        functions.add(it.function.copy(
                                firstStatement = statements.size(),
                                firstLocal = state.opts.userStorageStart)
                        )
                    }
                }
                if (it.real) {
                    val args = it.args
                    val a = if (args.size() > 0) args[0] else 0
                    val b = if (args.size() > 1) args[1] else 0
                    val c = if (args.size() > 2) args[2] else 0
                    statements.add(ProgramData.Statement(it.instr!!, a, b, c))
                }
            }
            val globalDefs = arrayListOf<ProgramData.Definition>() with {
                val f = fun(it: Allocator.AllocationMap.Entry) {
                    val k = it.ref
                    val v = it.value?.any
                    val e = state.allocator.allocateString(it.name)
                    add(ProgramData.Definition(0, k.toShort(), e.ref))
                    when (v) {
                        is Pointer -> intData.put(k, v.int)
                        is Int -> floatData.put(k, v.toFloat())
                        is Float -> floatData.put(k, v)
                    }
                }
                state.allocator.references.all.forEach(f)
                state.allocator.constants.all.forEach(f)
            }

            val globalData = run {
                val size = 4 * (state.opts.userStorageStart + (state.allocator.references.size() + state.allocator.constants.size()))
                assert(globalData.position() < size)
                globalData.limit(size)
                globalData.position(0)
                globalData.slice().order(ByteOrder.LITTLE_ENDIAN)
            }

            val stringManager = StringManager(state.allocator.strings.all.map { it.name })

            val version = 6
            val crc = -1 // TODO: CRC16
            val entityFields = fieldDefs.size()

            val statementsOffset = 60 // Size of header
            val globalDefsOffset = statementsOffset + statements.size() * 8
            val fieldDefsOffset = globalDefsOffset + globalDefs.size() * 8
            val functionsOffset = fieldDefsOffset + fieldDefs.size() * 8
            val globalDataOffset = functionsOffset + functions.size() * 36
            // Last for simplicity; strings are not fixed size
            val stringsOffset = globalDataOffset + globalData.capacity()

            return ProgramData(
                    header = ProgramData.Header(
                            version = version,
                            crc = crc,
                            entityFields = entityFields,
                            statements = ProgramData.Header.Section(statementsOffset, statements.size()),
                            globalDefs = ProgramData.Header.Section(globalDefsOffset, globalDefs.size()),
                            fieldDefs = ProgramData.Header.Section(fieldDefsOffset, fieldDefs.size()),
                            functions = ProgramData.Header.Section(functionsOffset, functions.size()),
                            globalData = ProgramData.Header.Section(globalDataOffset, globalData.capacity() / 4),
                            stringData = ProgramData.Header.Section(stringsOffset, stringManager.constant.length())
                    ),
                    statements = statements,
                    globalDefs = globalDefs,
                    fieldDefs = fieldDefs,
                    functions = functions,
                    globalData = globalData,
                    strings = stringManager
            ) with {
                logger.severe {
                    StringBuilder {
                        appendln("Program's system-checksum = ${header.crc}")
                        appendln("Entity field space: ${header.entityFields}")
                        appendln("Globals: ${header.globalData.count}")
                        appendln("Counts:")
                        appendln("      code: ${header.statements.count}")
                        appendln("      defs: ${header.globalDefs.count}")
                        appendln("    fields: ${header.fieldDefs.count}")
                        appendln(" functions: ${header.functions.count}")
                        appendln("   strings: ${header.stringData.count}")
                    }.toString()
                }
            }
        }
    }
}
