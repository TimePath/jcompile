package com.timepath.compiler.backend.q1vm.impl

import com.timepath.Logger
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.compiler.backend.q1vm.types.*
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
            for (it in ir) {
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
                if (!it.real) {
                    continue
                }
                val instr = it.instr
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
                    instr is Instruction.IF -> QInstruction.IF
                    instr is Instruction.IFNOT -> QInstruction.IFNOT
                    instr is Instruction.CALL ->
                        QInstruction.from(QInstruction.CALL0.ordinal() + Math.max(8, instr.argc))
                    instr is Instruction.STATE -> QInstruction.STATE
                    instr is Instruction.GOTO -> QInstruction.GOTO
                    instr is Instruction.AND -> QInstruction.AND
                    instr is Instruction.OR -> QInstruction.OR
                    instr is Instruction.BITAND -> QInstruction.BITAND
                    instr is Instruction.BITOR -> QInstruction.BITOR
                    else -> throw NoWhenBranchMatchedException()
                }
                val args = it.args
                val a = if (args.size() > 0) args[0] else 0
                val b = if (args.size() > 1) args[1] else 0
                val c = if (args.size() > 2) args[2] else 0
                statements.add(ProgramData.Statement(qinstr, a, b, c))
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
