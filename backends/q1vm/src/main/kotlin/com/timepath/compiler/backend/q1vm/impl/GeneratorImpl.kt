package com.timepath.compiler.backend.q1vm.impl

import com.timepath.Logger
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.ir.Allocator
import com.timepath.compiler.ir.IR
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.defaults.function_t
import com.timepath.q1vm.ProgramData
import com.timepath.q1vm.QInstruction
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
                state.allocator.allocateReference(e.id, e.type(state), e.value?.evaluate(state), Instruction.Ref.Scope.Global)
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

        inner class JumpManager(val idx: () -> Int) {
            /** Map of label to indices */
            private val labels: MutableMap<String, Int> = hashMapOf()
            /** Goto indices needing fixup */
            private val deferred: MutableList<Pair<String, Int>> = linkedListOf()

            fun label(id: String) {
                labels[id] = idx()
            }

            fun goto(label: String) {
                deferred.add(label to idx())
            }

            fun fixup(list: MutableList<ProgramData.Statement>) {
                for ((label, idx) in deferred) {
                    val it = labels[label]!!
                    val rel = it - idx
                    check(rel != 0)
                    val replace = list[idx]
                    when (replace.op) {
                        QInstruction.GOTO ->
                            list[idx] = replace.copy(a = rel)
                        QInstruction.IF, QInstruction.IFNOT ->
                            list[idx] = replace.copy(b = rel)
                        else -> throw NoWhenBranchMatchedException()
                    }
                }
            }
        }

        fun generateFunction(it: IR, localOfs: Int, jm: JumpManager, statements: MutableList<ProgramData.Statement>) {
            val instr = it.instr
            var (a, b, c) = (instr as? Instruction.WithArgs)?.args ?: Instruction.Args()
            val qinstr = when {
                it is IR.EndFunction -> QInstruction.DONE
                instr is Instruction.MUL_FLOAT -> QInstruction.MUL_FLOAT
                instr is Instruction.MUL_VEC -> QInstruction.MUL_VEC
                instr is Instruction.MUL_FLOAT_VEC -> QInstruction.MUL_FLOAT_VEC
                instr is Instruction.MUL_VEC_FLOAT -> QInstruction.MUL_VEC_FLOAT
                instr is Instruction.DIV_FLOAT -> QInstruction.DIV_FLOAT
                instr is Instruction.ADD_FLOAT -> QInstruction.ADD_FLOAT
                instr is Instruction.ADD_VEC -> QInstruction.ADD_VEC
                instr is Instruction.SUB_FLOAT -> QInstruction.SUB_FLOAT
                instr is Instruction.SUB_VEC -> QInstruction.SUB_VEC
                instr is Instruction.EQ -> when (instr.type) {
                    javaClass<float_t>() -> QInstruction.EQ_FLOAT
                    javaClass<vector_t>() -> QInstruction.EQ_VEC
                    javaClass<string_t>() -> QInstruction.EQ_STR
                    javaClass<entity_t>() -> QInstruction.EQ_ENT
                    javaClass<field_t>() -> QInstruction.EQ_FUNC
                    javaClass<function_t>() -> QInstruction.EQ_FUNC
                    else -> throw NoWhenBranchMatchedException()
                }
                instr is Instruction.NE -> when (instr.type) {
                    javaClass<float_t>() -> QInstruction.NE_FLOAT
                    javaClass<vector_t>() -> QInstruction.NE_VEC
                    javaClass<string_t>() -> QInstruction.NE_STR
                    javaClass<entity_t>() -> QInstruction.NE_ENT
                    javaClass<field_t>() -> QInstruction.NE_FUNC
                    javaClass<function_t>() -> QInstruction.NE_FUNC
                    else -> throw NoWhenBranchMatchedException()
                }
                instr is Instruction.LE -> QInstruction.LE
                instr is Instruction.GE -> QInstruction.GE
                instr is Instruction.LT -> QInstruction.LT
                instr is Instruction.GT -> QInstruction.GT
                instr is Instruction.LOAD -> when (instr.type) {
                    javaClass<float_t>() -> QInstruction.LOAD_FLOAT
                    javaClass<vector_t>() -> QInstruction.LOAD_VEC
                    javaClass<string_t>() -> QInstruction.LOAD_STR
                    javaClass<entity_t>() -> QInstruction.LOAD_ENT
                    javaClass<field_t>() -> QInstruction.LOAD_FIELD
                    javaClass<function_t>() -> QInstruction.LOAD_FUNC
                    else -> throw NoWhenBranchMatchedException()
                }
                instr is Instruction.ADDRESS -> QInstruction.ADDRESS
                instr is Instruction.STORE -> when (instr.type) {
                    javaClass<float_t>() -> QInstruction.STORE_FLOAT
                    javaClass<vector_t>() -> QInstruction.STORE_VEC
                    javaClass<string_t>() -> QInstruction.STORE_STR
                    javaClass<entity_t>() -> QInstruction.STORE_ENT
                    javaClass<field_t>() -> QInstruction.STORE_FIELD
                    javaClass<function_t>() -> QInstruction.STORE_FUNC
                    else -> throw NoWhenBranchMatchedException()
                }
                instr is Instruction.STOREP -> when (instr.type) {
                    javaClass<float_t>() -> QInstruction.STOREP_FLOAT
                    javaClass<vector_t>() -> QInstruction.STOREP_VEC
                    javaClass<string_t>() -> QInstruction.STOREP_STR
                    javaClass<entity_t>() -> QInstruction.STOREP_ENT
                    javaClass<field_t>() -> QInstruction.STOREP_FIELD
                    javaClass<function_t>() -> QInstruction.STOREP_FUNC
                    else -> throw NoWhenBranchMatchedException()
                }
                instr is Instruction.RETURN -> QInstruction.RETURN
                instr is Instruction.NOT -> when (instr.type) {
                    javaClass<float_t>() -> QInstruction.NOT_FLOAT
                    javaClass<vector_t>() -> QInstruction.NOT_VEC
                    javaClass<string_t>() -> QInstruction.NOT_STR
                    javaClass<entity_t>() -> QInstruction.NOT_ENT
                    javaClass<field_t>() -> QInstruction.NOT_FUNC
                    javaClass<function_t>() -> QInstruction.NOT_FUNC
                    else -> throw NoWhenBranchMatchedException()
                }
                instr is Instruction.CALL -> {
                    instr.params.mapIndexedTo(statements) { idx, it ->
                        val param = Instruction.OFS_PARAM(idx)
                        ProgramData.Statement(QInstruction.STORE_FLOAT, it.toGlobal(localOfs), param.toGlobal(localOfs), 0)
                    }
                    QInstruction.from(QInstruction.CALL0.ordinal() + instr.params.size().coerceIn(0, 8))
                }
                instr is Instruction.STATE -> QInstruction.STATE
                instr is Instruction.LABEL -> {
                    jm.label(instr.id)
                    return
                }
                instr is Instruction.GOTO -> {
                    if (instr is Instruction.GOTO.If) {
                        a = instr.condition
                        jm.goto(instr.id)
                        when (instr.expect) {
                            true -> QInstruction.IF
                            else -> QInstruction.IFNOT
                        }
                    } else {
                        if (instr is Instruction.GOTO.Label) {
                            jm.goto(instr.id)
                        }
                        QInstruction.GOTO
                    }
                }
                instr is Instruction.AND -> QInstruction.AND
                instr is Instruction.OR -> QInstruction.OR
                instr is Instruction.BITAND -> QInstruction.BITAND
                instr is Instruction.BITOR -> QInstruction.BITOR
                else -> throw NoWhenBranchMatchedException()
            }
            statements.add(ProgramData.Statement(qinstr, a.toGlobal(localOfs), b.toGlobal(localOfs), c.toGlobal(localOfs)))
        }

        fun Instruction.Ref.toGlobal(localOfs: Int): Int {
            val ofs = when (scope) {
                Instruction.Ref.Scope.Local ->
                    localOfs
                else -> 0
            }
            return ofs + i
        }

        /**
         * FIXME: metadata
         */
        override fun generateProgs(): ProgramData {
            val fieldDefs = arrayListOf<ProgramData.Definition>() with {
                for ((s, idx) in state.fields.map) {
                    val e = state.allocator.allocateString(s)
                    add(ProgramData.Definition(0, idx.toShort(), e.ref.i))
                }
            }
            val localOfs = state.opts.userStorageStart +
                    state.allocator.constants.all.count() +
                    state.allocator.references.all.count() // FIXME: too many?
            val numLocals = state.allocator.references.all.count { it.ref.scope == Instruction.Ref.Scope.Local }
            val statements = arrayListOf<ProgramData.Statement>()
            val functions = arrayListOf<ProgramData.Function>()
            val iter = ir.iterator()
            for (it in iter) {
                if (it is IR.Function) {
                    val firstStatement = statements.size()
                    it.function as ProgramData.Function
                    if (it.function.firstStatement < 0) {
                        functions.add(it.function)
                    } else {
                        it.function.copy(
                                firstStatement = firstStatement,
                                firstLocal = localOfs,
                                numLocals = numLocals
                        ) with { functions.add(this) }
                    }
                    val jm = JumpManager { statements.size() }
                    for (stmt in iter) {
                        if (stmt is IR.Return) continue
                        if (stmt is IR.EndFunction) break
                        if (stmt is IR.Declare) continue
                        generateFunction(stmt, localOfs, jm, statements)
                    }
                    jm.fixup(statements)
                    statements.add(ProgramData.Statement(QInstruction.DONE, 0, 0, 0))
                } else {
                    if (it is IR.Declare) continue
                    throw UnsupportedOperationException("" + it)
                }
            }
            val globalDefs = arrayListOf<ProgramData.Definition>() with {
                val f = fun(it: Allocator.AllocationMap.Entry) {
                    val k = it.ref
                    val v = it.value?.any
                    val e = state.allocator.allocateString(it.name)
                    val i = k.toGlobal(localOfs)
                    add(ProgramData.Definition(0, i.toShort(), e.ref.i))
                    when (v) {
                        is Pointer -> intData.put(i, v.int)
                        is Int -> floatData.put(i, v.toFloat())
                        is Float -> floatData.put(i, v)
                    }
                }
                state.allocator.references.all.forEach(f)
                state.allocator.constants.all.forEach(f)
            }

            val globalData = run {
                val size = 4 * (state.opts.userStorageStart + (state.allocator.references.size() + state.allocator.constants.size() + numLocals))
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
