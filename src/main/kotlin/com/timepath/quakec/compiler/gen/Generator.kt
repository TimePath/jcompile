package com.timepath.quakec.compiler.gen

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList
import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.CompilerOptions
import com.timepath.quakec.compiler.ast.*
import com.timepath.quakec.compiler.gen.Allocator.AllocationMap.Entry
import com.timepath.quakec.vm
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.vm.Definition
import com.timepath.quakec.vm.Function
import com.timepath.quakec.vm.ProgramData
import com.timepath.quakec.vm.ProgramData.Header
import com.timepath.quakec.vm.ProgramData.Header.Section
import com.timepath.quakec.vm.StringManager

class Generator(val opts: CompilerOptions, val roots: List<Statement>) {

    class object {
        val logger = Logging.new()
    }

    val allocator: Allocator = Allocator(opts)

    fun generate(): List<IR> = BlockStatement(roots).generate()

    /**
     * Ought to be enough, instructions can't address beyond this range anyway
     */
    val globalData = ByteBuffer.allocateDirect(4 * 0xFFFF).order(ByteOrder.LITTLE_ENDIAN)
    val intData = globalData.asIntBuffer()
    val floatData = globalData.asFloatBuffer()

    fun generateProgs(ir: List<IR> = generate()): ProgramData {
        val globalDefs = ArrayList<Definition>()
        val fieldDefs = ArrayList<Definition>()
        fieldDefs.add(Definition(0, 0, 0)) // FIXME: temporary

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
        logger.finer("${"> > ".repeat(allocator.scope.size())} ${this.javaClass.getSimpleName()}")
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
        logger.finer("${" < <".repeat(allocator.scope.size())} ${this.javaClass.getSimpleName()}")
    }

    private fun Statement.generate(): List<IR> {
        this.enter()
        val ret: List<IR> = when (this) {
            is BlockStatement -> {
                allocator.push("<block>")
                val list = children.flatMap {
                    it.generate()
                }
                allocator.pop()
                list
            }
            is FunctionLiteral -> {
                val global = allocator.allocateFunction(id)
                val f = Function(
                        firstStatement = if (builtin == null)
                            0 // to be filled in later
                        else
                            -builtin,
                        firstLocal = 0,
                        numLocals = 0,
                        profiling = 0,
                        nameOffset = allocator.allocateString(id!!).ref,
                        fileNameOffset = 0,
                        numParams = 0,
                        sizeof = byteArray(0, 0, 0, 0, 0, 0, 0, 0)
                )
                allocator.push(id)
                val list = (listOf(
                        FunctionIR(f))
                        + children.flatMap { it.generate() }
                        + IR(instr = Instruction.DONE)
                        + ReferenceIR(global.ref))
                allocator.pop()
                list
            }
            is ConstantExpression -> {
                val global = allocator.allocateConstant(value)
                listOf(ReferenceIR(global.ref))
            }
            is DeclarationExpression -> {
                val global = allocator.allocateReference(id, this.value?.evaluate())
                listOf(ReferenceIR(global.ref))
            }
            is MemoryReference -> {
                listOf(ReferenceIR(ref))
            }
            is EntityFieldReference -> {
                listOf(ReferenceIR(0)) // TODO: field by name
            }
            is ReferenceExpression -> {
                // FIXME: null references
                val global = allocator[id]
                listOf(ReferenceIR(global?.ref ?: 0))
            }
            is BinaryExpression.Assign -> {
                // ast:
                // left(a) = right(b)
                // vm:
                // b (=) a
                val left = when (left) {
                    is BinaryExpression.Dot -> {
                        // make a copy to avoid changing the right half of the assignment
                        val special = BinaryExpression.Dot(left.left, left.right)
                        special.instr = Instruction.ADDRESS
                        special
                    }
                    else -> left
                }
                val instr = when {
                    left is BinaryExpression.Dot -> {
                        Instruction.STOREP_FLOAT
                    }
                    else -> instr
                }
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
                        + IR(instr, array(genL.last().ret, genR.last().ret, global.ref), global.ref, this.toString()))
            }
            is ConditionalExpression -> {
                val ret = linkedListOf<IR>()
                val genPred = test.generate()
                ret.addAll(genPred)
                val genTrue = pass.generate()
                val trueCount = genTrue.count { it.real }
                val genFalse = fail?.generate()
                if (genFalse == null) {
                    // No else, jump to the instruction after the body
                    ret.add(IR(Instruction.IFNOT, array(genPred.last().ret, 1 + trueCount, 0)))
                    ret.addAll(genTrue)
                } else {
                    // The if body has a goto, include it in the count
                    ret.add(IR(Instruction.IFNOT, array(genPred.last().ret, 2 + trueCount, 0)))
                    ret.addAll(genTrue)
                    val falseCount = genFalse.count { it.real }
                    // end if, jump to the instruction after the else
                    ret.add(IR(Instruction.GOTO, array(1 + falseCount, 0, 0)))
                    // else
                    ret.addAll(genFalse)
                }
                ret
            }
            is ContinueStatement -> {
                // filled in by Loop.generate()
                listOf(IR(Instruction.GOTO, array(0, 0, 0)))
            }
            is BreakStatement -> {
                // filled in by Loop.generate()
                listOf(IR(Instruction.GOTO, array(0, 1, 0)))
            }
            is Loop -> {
                val genInit = initializer?.flatMap { it.generate() }

                val genPred = predicate.generate()
                val predCount = genPred.count { it.real }

                val genBody = children.flatMap { it.generate() }
                val bodyCount = genBody.count { it.real }

                val genUpdate = update?.flatMap { it.generate() }
                val updateCount = genUpdate?.count { it.real } ?: 0

                val totalCount = bodyCount + updateCount + predCount

                val ret = linkedListOf<IR>()
                if (genInit != null) {
                    ret.addAll(genInit)
                }
                ret.addAll(genPred)
                if (checkBefore) {
                    ret.add(IR(Instruction.IFNOT, array(genPred.last().ret,
                            totalCount + /* the last if */ 1 + /* the next instruction */ 1, 0)))
                }
                ret.addAll(genBody)
                if (genUpdate != null) {
                    ret.addAll(genUpdate)
                }
                ret.addAll(genPred)
                ret.add(IR(Instruction.IF, array(genPred.last().ret, -totalCount, 0)))

                // break/continue; jump to end
                genBody.filter { it.real }.forEachIndexed {(i, IR) ->
                    if(IR.instr == Instruction.GOTO && IR.args[0] == 0) {
                        val after = (bodyCount - 1) - i
                        IR.args[0] = after + 1 + when(IR.args[1]) {
                            // break
                            1 -> updateCount + predCount + /* if */ 1
                            else -> 0
                        }
                        IR.args[1] = 0
                    }
                }
                ret
            }
            is FunctionCall -> {
                // TODO: increase this
                if (args.size() > 8) {
                    logger.warning("${function} takes ${args.size()} parameters")
                }
                val args = args.take(8).map { it.generate() }
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
                val global = allocator.allocateReference()
                val ret = linkedListOf<IR>()
                ret.addAll(args.flatMap { it })
                ret.addAll(prepare)
                ret.add(IR(instr(i), array(funcId), Instruction.OFS_PARAM(-1)))
                ret.add(IR(Instruction.STORE_FLOAT, array(Instruction.OFS_PARAM(-1), global.ref), global.ref, "Save response"))
                ret
            }
            is ReturnStatement -> {
                val genRet = returnValue?.generate()
                val ret = linkedListOf<IR>()
                val args = array(0, 0, 0)
                if (genRet != null) {
                    ret.addAll(genRet)
                    args[0] = genRet.last().ret
                }
                ret.add(IR(Instruction.RETURN, args, 0))
                ret
            }
            else -> emptyList()
        }
        this.exit()
        return ret
    }

}