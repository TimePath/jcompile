package com.timepath.compiler.gen

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList
import com.timepath.Logger
import com.timepath.compiler.CompilerOptions
import com.timepath.compiler.ast.*
import com.timepath.compiler.gen.Allocator.AllocationMap.Entry
import com.timepath.q1vm.Definition
import com.timepath.q1vm.Function
import com.timepath.q1vm.ProgramData
import com.timepath.q1vm.ProgramData.Header
import com.timepath.q1vm.ProgramData.Header.Section
import com.timepath.q1vm.StringManager
import com.timepath.q1vm.Statement

class Generator(val opts: CompilerOptions) {

    val gotoLabels = linkedMapOf<IR, String>()

    class object {
        val logger = Logger.new()
    }

    val allocator: Allocator = Allocator(opts)

    fun generate(roots: List<Expression>): ASM {
        roots.forEach {
            it.transform { it.reduce() }
        }
        return ASM(BlockExpression(roots).generate(this))
    }

    inner class ASM(val ir: List<IR>) {

        /**
         * Ought to be enough, instructions can't address beyond this range anyway
         */
        val globalData = ByteBuffer.allocate(4 * 0xFFFF).order(ByteOrder.LITTLE_ENDIAN)
        val intData = globalData.asIntBuffer()
        val floatData = globalData.asFloatBuffer()

        fun generateProgs(): ProgramData {
            val globalDefs = ArrayList<Definition>()
            val fieldDefs = ArrayList<Definition>()
            fieldDefs.add(Definition(0, 0, 0)) // FIXME: temporary

            val statements = ArrayList<Statement>(ir.size())
            val functions = ArrayList<Function>()
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
                    statements.add(Statement(it.instr!!, a, b, c))
                }
            }
            val merge = {(it: Entry): Unit ->
                val wrapped = it.value
                val k = it.ref
                val v = wrapped.value
                when (v) {
                    is Int -> intData.put(k, v)
                    is Float -> floatData.put(k, v)
                }
            }
            allocator.references.all.forEach(merge)
            allocator.constants.all.forEach(merge)

            val globalData = {
                val size = 4 * (opts.userStorageStart + (allocator.references.size() + allocator.constants.size()))
                assert(size >= globalData.position())
                globalData.limit(size)
                globalData.position(0)
                globalData.slice().order(ByteOrder.LITTLE_ENDIAN)
            }()

            val stringManager = StringManager(allocator.strings.all.map { it.name })

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
