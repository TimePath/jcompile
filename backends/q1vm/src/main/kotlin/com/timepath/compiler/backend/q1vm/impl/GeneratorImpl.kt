package com.timepath.compiler.backend.q1vm.impl

import com.timepath.Logger
import com.timepath.compiler.Vector
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.ir.Allocator
import com.timepath.compiler.ir.IR
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.types.defaults.sizeOf
import com.timepath.q1vm.ProgramData
import com.timepath.q1vm.QInstruction
import com.timepath.q1vm.QType
import com.timepath.q1vm.StringManager
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeneratorImpl(val state: Q1VM.State) : Generator {

    companion object {
        val logger = Logger()
    }

    override fun generate(roots: List<Expression>): Generator.ASM {
        val block = BlockExpression(roots)
        val reduced = block.reduce(state).single()
        state.allocator.push("<forward declarations>")
        val allocate = object : ASTVisitor<Unit> {
            override fun visit(e: BlockExpression) = e.children.forEach { it.accept(this) }

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
        reduced.accept(allocate)
        val ir = reduced.accept(state.generatorVisitor)
        return ASMImpl(ir)
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
            val qinstr = when (instr) {
                is Instruction.MUL_FLOAT -> QInstruction.MUL_FLOAT
                is Instruction.MUL_VEC -> QInstruction.MUL_VEC
                is Instruction.MUL_FLOAT_VEC -> QInstruction.MUL_FLOAT_VEC
                is Instruction.MUL_VEC_FLOAT -> QInstruction.MUL_VEC_FLOAT
                is Instruction.DIV_FLOAT -> QInstruction.DIV_FLOAT
                is Instruction.ADD_FLOAT -> QInstruction.ADD_FLOAT
                is Instruction.ADD_VEC -> QInstruction.ADD_VEC
                is Instruction.SUB_FLOAT -> QInstruction.SUB_FLOAT
                is Instruction.SUB_VEC -> QInstruction.SUB_VEC
                is Instruction.EQ -> when (instr.type) {
                    float_t::class.java -> QInstruction.EQ_FLOAT
                    vector_t::class.java -> QInstruction.EQ_VEC
                    string_t::class.java -> QInstruction.EQ_STR
                    entity_t::class.java -> QInstruction.EQ_ENT
                    field_t::class.java -> QInstruction.EQ_FUNC
                    function_t::class.java -> QInstruction.EQ_FUNC
                    else -> QInstruction.EQ_FLOAT
                }
                is Instruction.NE -> when (instr.type) {
                    float_t::class.java -> QInstruction.NE_FLOAT
                    vector_t::class.java -> QInstruction.NE_VEC
                    string_t::class.java -> QInstruction.NE_STR
                    entity_t::class.java -> QInstruction.NE_ENT
                    field_t::class.java -> QInstruction.NE_FUNC
                    function_t::class.java -> QInstruction.NE_FUNC
                    else -> QInstruction.NE_FLOAT
                }
                is Instruction.LE -> QInstruction.LE
                is Instruction.GE -> QInstruction.GE
                is Instruction.LT -> QInstruction.LT
                is Instruction.GT -> QInstruction.GT
                is Instruction.LOAD -> when (instr.type) {
                    float_t::class.java -> QInstruction.LOAD_FLOAT
                    vector_t::class.java -> QInstruction.LOAD_VEC
                    string_t::class.java -> QInstruction.LOAD_STR
                    entity_t::class.java -> QInstruction.LOAD_ENT
                    field_t::class.java -> QInstruction.LOAD_FIELD
                    function_t::class.java -> QInstruction.LOAD_FUNC
                    else -> QInstruction.LOAD_FLOAT
                }
                is Instruction.ADDRESS -> QInstruction.ADDRESS
                is Instruction.STORE -> when (instr.type) {
                    void_t::class.java -> return
                    float_t::class.java -> QInstruction.STORE_FLOAT
                    vector_t::class.java -> QInstruction.STORE_VEC
                    string_t::class.java -> QInstruction.STORE_STR
                    entity_t::class.java -> QInstruction.STORE_ENT
                    field_t::class.java -> QInstruction.STORE_FIELD
                    function_t::class.java -> QInstruction.STORE_FUNC
                    else -> QInstruction.STORE_FLOAT
                }
                is Instruction.STOREP -> when (instr.type) {
                    float_t::class.java -> QInstruction.STOREP_FLOAT
                    vector_t::class.java -> QInstruction.STOREP_VEC
                    string_t::class.java -> QInstruction.STOREP_STR
                    entity_t::class.java -> QInstruction.STOREP_ENT
                    field_t::class.java -> QInstruction.STOREP_FIELD
                    function_t::class.java -> QInstruction.STOREP_FUNC
                    else -> QInstruction.STOREP_FLOAT
                }
                is Instruction.RETURN -> QInstruction.RETURN
                is Instruction.NOT -> when (instr.type) {
                    float_t::class.java -> QInstruction.NOT_FLOAT
                    vector_t::class.java -> QInstruction.NOT_VEC
                    string_t::class.java -> QInstruction.NOT_STR
                    entity_t::class.java -> QInstruction.NOT_ENT
                    field_t::class.java -> QInstruction.NOT_FUNC
                    function_t::class.java -> QInstruction.NOT_FUNC
                    else -> QInstruction.NOT_FLOAT
                }
                is Instruction.CALL -> {
                    instr.params.mapIndexedTo(statements) { idx, it ->
                        val param = Instruction.OFS_PARAM(idx)
                        val move = when (it.second) {
                            float_t::class.java -> QInstruction.STORE_FLOAT
                            vector_t::class.java -> QInstruction.STORE_VEC
                            string_t::class.java -> QInstruction.STORE_STR
                            entity_t::class.java -> QInstruction.STORE_ENT
                            field_t::class.java -> QInstruction.STORE_FIELD
                            function_t::class.java -> QInstruction.STORE_FUNC
                            else -> QInstruction.STORE_FLOAT
                        }
                        ProgramData.Statement(move, it.first.toGlobal(localOfs), param.toGlobal(localOfs), -1)
                    }
                    QInstruction.from(QInstruction.CALL0.ordinal + instr.params.size.coerceIn(0, 8))
                }
                is Instruction.STATE -> QInstruction.STATE
                is Instruction.LABEL -> {
                    jm.label(instr.id)
                    return
                }
                is Instruction.GOTO -> {
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
                is Instruction.AND -> QInstruction.AND
                is Instruction.OR -> QInstruction.OR
                is Instruction.BITAND -> QInstruction.BITAND
                is Instruction.BITOR -> QInstruction.BITOR
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

        fun storeFloat(i: Int, f: Float) {
            // TODO: compare epsilon?
            floatData.put(i, f)
        }

        fun storeConstant(localOfs: Int, it: Allocator.AllocationMap.Entry): ProgramData.Definition {
            val k = it.ref
            val v = it.value?.any
            val e = state.allocator.allocateString(it.name)
            val i = k.toGlobal(localOfs)
            when (v) {
                is Pointer -> intData.put(i, v.int)
                is Int -> storeFloat(i, v.toFloat())
                is Float -> storeFloat(i, v)
                is Vector -> {
                    storeFloat(i + 0, v.x)
                    storeFloat(i + 1, v.y)
                    storeFloat(i + 2, v.z)
                }
            }
            return ProgramData.Definition(QType[it.type], i.toShort(), e.ref.i)
        }

        /**
         * FIXME: metadata
         */
        override fun generateProgs(): ProgramData {
            val fieldDefs = arrayListOf<ProgramData.Definition>().apply {
                for ((pair, idx) in state.fields.map) {
                    val (s, owner) = pair
                    val type = owner.fields[s]!!
                    val e = state.allocator.allocateString(s)
                    add(ProgramData.Definition(QType[type], idx.toShort(), e.ref.i))
                }
            }
            val localOfs = state.opts.userStorageStart + run {
                val find: (Allocator.AllocationMap.Entry) -> Boolean = { it.ref.scope == Instruction.Ref.Scope.Global }
                val tmp = state.allocator.constants.all.filter(find) + state.allocator.references.all.filter(find)
                val last = tmp.maxBy { it.ref.i }
                last!!.ref.i
            }
            val numLocals = run {
                val find: (Allocator.AllocationMap.Entry) -> Boolean = { it.ref.scope == Instruction.Ref.Scope.Local }
                val tmp = state.allocator.constants.all.filter(find) + state.allocator.references.all.filter(find)
                val last = tmp.maxBy { it.ref.i }
                last!!.ref.i
            }
            val statements = arrayListOf<ProgramData.Statement>()
            val functions = arrayListOf<ProgramData.Function>()
            functions.add(ProgramData.Function(0, 0, 0, 0, 0, 0, 0, byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)))
            for (it in ir) {
                if (it is IR.Function) {
                    val firstStatement = statements.size
                    val f = it.function as ProgramData.Function
                    if (f.firstStatement < 0) {
                        functions.add(f)
                    } else {
                        f.copy(
                                firstStatement = firstStatement,
                                firstLocal = localOfs,
                                numLocals = numLocals
                        ).apply { functions.add(this) }
                    }
                    val jm = JumpManager { statements.size }
                    for (stmt in it.children) {
                        if (stmt is IR.Return) continue
                        if (stmt is IR.Declare) continue
                        generateFunction(stmt, localOfs, jm, statements)
                    }
                    jm.fixup(statements)
                    statements.add(ProgramData.Statement(QInstruction.DONE, 0, -1, -1)) // FIXME: -1
                } else {
                    if (it is IR.Declare) continue
                    throw UnsupportedOperationException("" + it)
                }
            }
            val globalDefs = arrayListOf<ProgramData.Definition>().apply {
                for (it in state.allocator.references.all) {
                    add(storeConstant(localOfs, it))
                }
                for (it in state.allocator.constants.all) {
                    add(storeConstant(localOfs, it))
                }
            }

            val globalData = run {
                val size = 4 * (localOfs + numLocals + 1)
                assert(globalData.position() < size)
                globalData.limit(size)
                globalData.position(0)
                globalData.slice().order(ByteOrder.LITTLE_ENDIAN)
            }

            val stringManager = StringManager(state.allocator.strings.all.map { it.name })

            val version = 6
            val crc = -1 // TODO: CRC16
            val entityFields = entity_t.fields.values.sumBy { it.sizeOf() } // FIXME: entity classes

            val statementsOffset = 60 // Size of header
            val globalDefsOffset = statementsOffset + statements.size * 8
            val fieldDefsOffset = globalDefsOffset + globalDefs.size * 8
            val functionsOffset = fieldDefsOffset + fieldDefs.size * 8
            val globalDataOffset = functionsOffset + functions.size * 36
            // Last for simplicity; strings are not fixed size
            val stringsOffset = globalDataOffset + globalData.capacity()

            return ProgramData(
                    header = ProgramData.Header(
                            version = version,
                            crc = crc,
                            entityFields = entityFields,
                            statements = ProgramData.Header.Section(statementsOffset, statements.size),
                            globalDefs = ProgramData.Header.Section(globalDefsOffset, globalDefs.size),
                            fieldDefs = ProgramData.Header.Section(fieldDefsOffset, fieldDefs.size),
                            functions = ProgramData.Header.Section(functionsOffset, functions.size),
                            globalData = ProgramData.Header.Section(globalDataOffset, globalData.capacity() / 4),
                            stringData = ProgramData.Header.Section(stringsOffset, stringManager.constant.size)
                    ),
                    statements = statements,
                    globalDefs = globalDefs,
                    fieldDefs = fieldDefs,
                    functions = functions,
                    globalData = globalData,
                    strings = stringManager
            ).apply {
                logger.severe {
                    buildString {
                        appendln("Program's system-checksum = ${header.crc}")
                        appendln("Entity field space: ${header.entityFields}")
                        appendln("Globals: ${header.globalData.count}")
                        appendln("Counts:")
                        appendln("      code: ${header.statements.count}")
                        appendln("      defs: ${header.globalDefs.count}")
                        appendln("    fields: ${header.fieldDefs.count}")
                        appendln(" functions: ${header.functions.count}")
                        appendln("   strings: ${header.stringData.count}")
                    }
                }
            }
        }
    }
}
