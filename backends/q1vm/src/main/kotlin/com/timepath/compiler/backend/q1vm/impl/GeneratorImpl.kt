package com.timepath.compiler.backend.q1vm.impl

import com.timepath.Logger
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.backend.q1vm.Instruction
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
                    val instr = when (it.instr) {
                        Instruction.DONE -> QInstruction.DONE
                        Instruction.MUL_FLOAT -> QInstruction.MUL_FLOAT
                        Instruction.MUL_VEC -> QInstruction.MUL_VEC
                        Instruction.MUL_FLOAT_VEC -> QInstruction.MUL_FLOAT_VEC
                        Instruction.MUL_VEC_FLOAT -> QInstruction.MUL_VEC_FLOAT
                        Instruction.DIV_FLOAT -> QInstruction.DIV_FLOAT
                        Instruction.ADD_FLOAT -> QInstruction.ADD_FLOAT
                        Instruction.ADD_VEC -> QInstruction.ADD_VEC
                        Instruction.SUB_FLOAT -> QInstruction.SUB_FLOAT
                        Instruction.SUB_VEC -> QInstruction.SUB_VEC
                        Instruction.EQ_FLOAT -> QInstruction.EQ_FLOAT
                        Instruction.EQ_VEC -> QInstruction.EQ_VEC
                        Instruction.EQ_STR -> QInstruction.EQ_STR
                        Instruction.EQ_ENT -> QInstruction.EQ_ENT
                        Instruction.EQ_FUNC -> QInstruction.EQ_FUNC
                        Instruction.NE_FLOAT -> QInstruction.NE_FLOAT
                        Instruction.NE_VEC -> QInstruction.NE_VEC
                        Instruction.NE_STR -> QInstruction.NE_STR
                        Instruction.NE_ENT -> QInstruction.NE_ENT
                        Instruction.NE_FUNC -> QInstruction.NE_FUNC
                        Instruction.LE -> QInstruction.LE
                        Instruction.GE -> QInstruction.GE
                        Instruction.LT -> QInstruction.LT
                        Instruction.GT -> QInstruction.GT
                        Instruction.LOAD_FLOAT -> QInstruction.LOAD_FLOAT
                        Instruction.LOAD_VEC -> QInstruction.LOAD_VEC
                        Instruction.LOAD_STR -> QInstruction.LOAD_STR
                        Instruction.LOAD_ENT -> QInstruction.LOAD_ENT
                        Instruction.LOAD_FIELD -> QInstruction.LOAD_FIELD
                        Instruction.LOAD_FUNC -> QInstruction.LOAD_FUNC
                        Instruction.ADDRESS -> QInstruction.ADDRESS
                        Instruction.STORE_FLOAT -> QInstruction.STORE_FLOAT
                        Instruction.STORE_VEC -> QInstruction.STORE_VEC
                        Instruction.STORE_STR -> QInstruction.STORE_STR
                        Instruction.STORE_ENT -> QInstruction.STORE_ENT
                        Instruction.STORE_FIELD -> QInstruction.STORE_FIELD
                        Instruction.STORE_FUNC -> QInstruction.STORE_FUNC
                        Instruction.STOREP_FLOAT -> QInstruction.STOREP_FLOAT
                        Instruction.STOREP_VEC -> QInstruction.STOREP_VEC
                        Instruction.STOREP_STR -> QInstruction.STOREP_STR
                        Instruction.STOREP_ENT -> QInstruction.STOREP_ENT
                        Instruction.STOREP_FIELD -> QInstruction.STOREP_FIELD
                        Instruction.STOREP_FUNC -> QInstruction.STOREP_FUNC
                        Instruction.RETURN -> QInstruction.RETURN
                        Instruction.NOT_FLOAT -> QInstruction.NOT_FLOAT
                        Instruction.NOT_VEC -> QInstruction.NOT_VEC
                        Instruction.NOT_STR -> QInstruction.NOT_STR
                        Instruction.NOT_ENT -> QInstruction.NOT_ENT
                        Instruction.NOT_FUNC -> QInstruction.NOT_FUNC
                        Instruction.IF -> QInstruction.IF
                        Instruction.IFNOT -> QInstruction.IFNOT
                        Instruction.CALL0 -> QInstruction.CALL0
                        Instruction.CALL1 -> QInstruction.CALL1
                        Instruction.CALL2 -> QInstruction.CALL2
                        Instruction.CALL3 -> QInstruction.CALL3
                        Instruction.CALL4 -> QInstruction.CALL4
                        Instruction.CALL5 -> QInstruction.CALL5
                        Instruction.CALL6 -> QInstruction.CALL6
                        Instruction.CALL7 -> QInstruction.CALL7
                        Instruction.CALL8 -> QInstruction.CALL8
                        Instruction.STATE -> QInstruction.STATE
                        Instruction.GOTO -> QInstruction.GOTO
                        Instruction.AND -> QInstruction.AND
                        Instruction.OR -> QInstruction.OR
                        Instruction.BITAND -> QInstruction.BITAND
                        Instruction.BITOR -> QInstruction.BITOR
                        else -> throw NoWhenBranchMatchedException()
                    }
                    statements.add(ProgramData.Statement(instr, a, b, c))
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
