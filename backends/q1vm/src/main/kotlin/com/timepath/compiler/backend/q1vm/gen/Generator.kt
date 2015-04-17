package com.timepath.compiler.backend.q1vm.gen

import com.timepath.Logger
import com.timepath.compiler.ast.BlockExpression
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.gen.Allocator.AllocationMap.Entry
import com.timepath.compiler.data.Pointer
import com.timepath.q1vm.ProgramData
import com.timepath.q1vm.ProgramData.Header
import com.timepath.q1vm.ProgramData.Header.Section
import com.timepath.q1vm.StringManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

class Generator(val state: Q1VM.State) {

    val gotoLabels = linkedMapOf<IR, String>()

    companion object {
        val logger = Logger.new()
    }


    fun generate(roots: List<Expression>): ASM {
        roots.forEach {
            it.transform { it.reduce() }
        }
        return ASM(BlockExpression(roots).generate(state))
    }

    inner class ASM(val ir: List<IR>) {

        /**
         * Ought to be enough, instructions can't address beyond this range anyway
         */
        val globalData = ByteBuffer.allocate(4 * 0xFFFF).order(ByteOrder.LITTLE_ENDIAN)
        val intData = globalData.asIntBuffer()
        val floatData = globalData.asFloatBuffer()

        fun generateProgs(): ProgramData {
            val globalDefs = ArrayList<ProgramData.Definition>()
            val fieldDefs = ArrayList<ProgramData.Definition>()
            fieldDefs.add(ProgramData.Definition(0, 0, 0)) // FIXME: temporary

            val statements = ArrayList<ProgramData.Statement>(ir.size())
            val functions = ArrayList<ProgramData.Function>()
            ir.forEach {
                if (it is FunctionIR) {
                    functions.add(
                            if (it.function.firstStatement >= 0)
                                it.function.copy(firstStatement = statements.size())
                            else
                                it.function)
                }
                if (it.real) {
                    val args = it.args
                    val a = if (args.size() > 0) args[0] else 0
                    val b = if (args.size() > 1) args[1] else 0
                    val c = if (args.size() > 2) args[2] else 0
                    statements.add(ProgramData.Statement(it.instr!!, a, b, c))
                }
            }
            val merge = fun (it: Entry) {
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
            val entityFields = fieldDefs.size() // TODO: good enough?

            val statementsOffset = 60
            val globalDefsOffset = statementsOffset + statements.size() * 8
            val fieldDefsOffset = globalDefsOffset + globalDefs.size() * 8
            val functionsOffset = fieldDefsOffset + fieldDefs.size() * 8
            val globalDataOffset = functionsOffset + functions.size() * 36
            // Last for simplicity; strings are not fixed size
            val stringsOffset = globalDataOffset + globalData.capacity() * 4

            return ProgramData(
                    header = Header(
                            version = version,
                            crc = crc,
                            entityFields = entityFields,
                            statements = Section(statementsOffset, statements.size()),
                            globalDefs = Section(globalDefsOffset, globalDefs.size()),
                            fieldDefs = Section(fieldDefsOffset, fieldDefs.size()),
                            functions = Section(functionsOffset, functions.size()),
                            globalData = Section(stringsOffset, globalData.capacity()),
                            stringData = Section(globalDataOffset, stringManager.constant.length())
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
