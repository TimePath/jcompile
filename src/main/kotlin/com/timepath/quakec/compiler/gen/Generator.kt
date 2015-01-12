package com.timepath.quakec.compiler.gen

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Stack
import java.util.regex.Pattern
import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.ast.*
import com.timepath.quakec.vm
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.vm.Definition
import com.timepath.quakec.vm.Function
import com.timepath.quakec.vm.ProgramData
import com.timepath.quakec.vm.ProgramData.Header
import com.timepath.quakec.vm.ProgramData.Header.Section
import com.timepath.quakec.vm.StringManager

class Generator(val roots: List<Statement>) {

    class object {
        val logger = Logging.new()
    }

    val allocator: Allocator = Allocator()

    fun generate(): List<IR> = BlockStatement(roots).generate()

    /**
     * Ought to be enough, instructions can't address beyond this range anyway
     */
    val globalData = ByteBuffer.allocateDirect(4 * 0xFFFF).order(ByteOrder.LITTLE_ENDIAN)
    val intData = globalData.asIntBuffer()
    val floatData = globalData.asFloatBuffer()

    fun generateProgs(ir: List<IR> = generate()): ProgramData {
        logger.fine(allocator.toString())

        val globalDefs = ArrayList<Definition>()
        val fieldDefs = ArrayList<Definition>()

        val statements = ArrayList<vm.Statement>(ir.size())
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
                statements.add(vm.Statement(it.instr!!, a, b, c))
            }
        }
        for ((k, wrapped) in allocator.constants) {
            val v = wrapped.value
            when (v) {
                is Int -> intData.put(k, v)
            }
        }

        val globalData = {
            assert(4 * allocator.counter >= globalData.position())
            globalData.limit(4 * allocator.counter)
            globalData.position(0)
            globalData.slice().order(ByteOrder.LITTLE_ENDIAN)
        }()

        val stringManager = StringManager(allocator.strings.keySet())

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
                        stringData = Section(globalDataOffset, stringManager.constant.size())
                ),
                statements = statements,
                globalDefs = globalDefs,
                fieldDefs = fieldDefs,
                functions = functions,
                globalData = globalData,
                strings = stringManager
        )
    }

    private fun Statement.enter() {
        logger.fine("${"> > ".repeat(allocator.scope.size())} ${this.javaClass.getSimpleName()}")
        when (this) {
            is FunctionLiteral -> {
                if (id != null && id in allocator) {
                    logger.warning("redefining $id")
                }
            }
            is DeclarationExpression -> {
                // do nothing
            }
            is ReferenceExpression -> {
                if (id !in allocator) {
                    logger.severe("unknown reference $id")
                }
            }
        }
    }

    private fun Statement.exit() {
        when (this) {
            is BlockStatement -> {
                allocator.pop()
            }
            is FunctionLiteral -> {
                allocator.pop()
            }
        }
        logger.fine("${" < <".repeat(allocator.scope.size())} ${this.javaClass.getSimpleName()}")
    }

    private fun Statement.generate(): List<IR> {
        this.enter()
        val ret: List<IR> = when (this) {
            is BlockStatement -> {
                allocator.push()
                children.flatMap {
                    it.generate()
                }
            }
            is FunctionLiteral -> {
                val global = allocator.allocateFunction(id)
                val f = Function(
                        firstStatement = 0, // to be filled in later
                        firstLocal = 0,
                        numLocals = 0,
                        profiling = 0,
                        nameOffset = allocator.allocateString(id!!),
                        fileNameOffset = 0,
                        numParams = 0,
                        sizeof = byteArray(0, 0, 0, 0, 0, 0, 0, 0)
                )
                allocator.push()
                (listOf(
                        FunctionIR(f))
                        + children.flatMap { it.generate() }
                        + IR(instr = Instruction.DONE)
                        + ReferenceIR(global))
            }
            is ConstantExpression -> {
                val global = allocator.allocateConstant(value)
                listOf(
                        ReferenceIR(global))
            }
            is DeclarationExpression -> {
                val global = allocator.allocateReference(id)
                val ret = linkedListOf<IR>()
                if (this.value != null) {
                    val value = this.value.evaluate()
                    val s = value.toString()
                    if (s.startsWith('#')) { // FIXME: HACK
                        val builtin = Function(
                                firstStatement = -s.substring(1).toInt(),
                                firstLocal = 0,
                                numLocals = 0,
                                profiling = 0,
                                nameOffset = allocator.allocateString(id),
                                fileNameOffset = 0,
                                numParams = 0,
                                sizeof = byteArray(0, 0, 0, 0, 0, 0, 0, 0))
                        ret.add(FunctionIR(builtin))
                    }
                }
                ret.add(ReferenceIR(global))
                ret
            }
            is ReferenceExpression -> {
                val global = allocator[id]!!
                listOf(ReferenceIR(global))
            }
            is BinaryExpression.Assign -> {
                // ast:
                // left(a) = right(b)
                // vm:
                // b (=) a
                val genL = left.generate()
                val genR = right.generate()
                (genL + genR
                        + IR(instr, array(genR.last().ret, genL.last().ret), genL.last().ret, this.toString()))
            }
            is BinaryExpression<*, *> -> {
                // ast:
                // temp(c) = left(a) op right(b)
                // vm:
                // c (=) a (op) b
                val genL = left.generate()
                val genR = right.generate()
                val global = allocator.allocateReference()
                (genL + genR
                        + IR(instr, array(genL.last().ret, genR.last().ret, global), global, this.toString()))
            }
            is ConditionalExpression -> {
                // TODO
                test.generate()
            }
            is FunctionCall -> {
                val args = args.map { it.generate() }
                val instr = {(i: Int) ->
                    Instruction.from(Instruction.CALL0.ordinal() + i)
                }
                var i = 0
                val prepare: List<IR> = args.map {
                    val param = Instruction.OFS_PARAM(i++)
                    IR(Instruction.STORE_FLOAT, array(it.last().ret, param), param, "Prepare param $i")
                }
                val genF = function.generate()
                val funcId = genF.last().ret
                (args.flatMap { it }
                        + prepare
                        + listOf(IR(instr(i), array(funcId), Instruction.OFS_PARAM(-1)))
                        )
            }
            is ReturnStatement -> {
                listOf(
                        IR(Instruction.RETURN, array(0, 0, 0), 0))
            }
            else -> emptyList()
        }
        this.exit()
        return ret
    }

}