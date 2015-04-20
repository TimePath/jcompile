package com.timepath.compiler.backend.q1vm.impl

import com.timepath.compiler.ast.BlockExpression
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.q1vm.ProgramData
import com.timepath.q1vm.StringManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

class GeneratorImpl(val state: Q1VM.State) : Generator {

    override val gotoLabels = linkedMapOf<IR, String>()

    override fun generate(roots: List<Expression>): Generator.ASM {
        roots.forEach {
            it.transform { it.reduce() }
        }
        return ASMImpl(BlockExpression(roots).generate(state))
    }

    inner class ASMImpl(override val ir: List<IR>) : Generator.ASM {

        /**
         * Ought to be enough, instructions can't address beyond this range anyway
         */
        val globalData = ByteBuffer.allocate(4 * 0xFFFF).order(ByteOrder.LITTLE_ENDIAN)
        val intData = globalData.asIntBuffer()
        val floatData = globalData.asFloatBuffer()

        override fun generateProgs(): ProgramData {
            val globalDefs = ArrayList<ProgramData.Definition>()
            val fieldDefs = ArrayList<ProgramData.Definition>()
            fieldDefs.add(ProgramData.Definition(0, 0, 0)) // FIXME: temporary

            val statements = ArrayList<ProgramData.Statement>(ir.size())
            val functions = ArrayList<ProgramData.Function>()
            ir.forEach {
                if (it is FunctionIR) {
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
            val merge = fun (it: Allocator.AllocationMap.Entry) {
                val k = it.ref
                val v = it.value?.any
                when (v) {
                    is Pointer -> intData.put(k, v.int)
                    is Int -> floatData.put(k, v.toFloat())
                    is Float -> floatData.put(k, v)
                }
            }
            state.allocator.references.all.forEach(merge)
            state.allocator.constants.all.forEach(merge)

            val globalData = {
                val size = 4 * (state.opts.userStorageStart + (state.allocator.references.size() + state.allocator.constants.size()))
                assert(size >= globalData.position())
                globalData.limit(size)
                globalData.position(0)
                globalData.slice().order(ByteOrder.LITTLE_ENDIAN)
            }()

            val stringManager = StringManager(state.allocator.strings.all.map { it.name })

            val version = 6
            val crc = -1 // TODO: CRC16
            val entityFields = fieldDefs.size()

            val statementsOffset = 60
            val globalDefsOffset = statementsOffset + statements.size() * 8
            val fieldDefsOffset = globalDefsOffset + globalDefs.size() * 8
            val functionsOffset = fieldDefsOffset + fieldDefs.size() * 8
            val globalDataOffset = functionsOffset + functions.size() * 36
            // Last for simplicity; strings are not fixed size
            val stringsOffset = globalDataOffset + globalData.capacity() * 4

            return ProgramData(
                    header = ProgramData.Header(
                            version = version,
                            crc = crc,
                            entityFields = entityFields,
                            statements = ProgramData.Header.Section(statementsOffset, statements.size()),
                            globalDefs = ProgramData.Header.Section(globalDefsOffset, globalDefs.size()),
                            fieldDefs = ProgramData.Header.Section(fieldDefsOffset, fieldDefs.size()),
                            functions = ProgramData.Header.Section(functionsOffset, functions.size()),
                            globalData = ProgramData.Header.Section(stringsOffset, globalData.capacity()),
                            stringData = ProgramData.Header.Section(globalDataOffset, stringManager.constant.length())
                    ),
                    statements = statements,
                    globalDefs = globalDefs,
                    fieldDefs = fieldDefs,
                    functions = functions,
                    globalData = globalData,
                    strings = stringManager
            )
        }
    }
}
