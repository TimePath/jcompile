package com.timepath.compiler.backend.q1vm.visitors

import com.timepath.Logger
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.types.class_t
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Types
import com.timepath.compiler.types.defaults.function_t
import com.timepath.debug
import com.timepath.getTextWS
import com.timepath.q1vm.Instruction
import com.timepath.q1vm.ProgramData

class GeneratorVisitor(val state: Q1VM.State) : ASTVisitor<List<IR>> {

    companion object {
        val logger = Logger()
    }

    suppress("NOTHING_TO_INLINE") inline fun Expression.generate(): List<IR> = accept(this@GeneratorVisitor)

    inline fun Expression.wrap(body: (Expression) -> List<IR>) = try {
        body(this)
    } catch(e: Exception) {
        when (e) {
            is UnsupportedOperationException -> {
                ctx?.let { ctx ->
                    val reason = e.getMessage()!!
                    logger.severe { "${ctx.debug()}: error: ${reason}\n${ctx.getTextWS()}\n" }
                    state.errors.add(Q1VM.Err(ctx, reason))
                }
            }
        }
        listOf<IR>()
    }

    override fun visit(e: BinaryExpression) = Types.handle<Q1VM.State, List<IR>>(Operation(e.op, e.left.type(state), e.right.type(state)))(state, e.left, e.right)

    override fun visit(e: BlockExpression): List<IR> {
        state.allocator.push(this)
        val list = e.children.flatMap {
            it.wrap { it.generate(state) }
        }
        state.allocator.pop()
        return list
    }

    override fun visit(e: BreakStatement): List<IR> {
        // filled in by Loop.doGenerate()
        return listOf(IR(Instruction.GOTO, array(0, 1, 0), name = e.toString()))
    }

    override fun visit(e: ConditionalExpression): List<IR> {
        val ret = linkedListOf<IR>()
        val genPred = e.test.wrap { it.generate(state) }
        ret.addAll(genPred)
        val genTrue = e.pass.wrap { it.generate(state) }
        val trueCount = genTrue.count { it.real }
        val genFalse = e.fail?.wrap { it.generate(state) }
        if (genFalse == null) {
            // No else, jump to the instruction after the body
            ret.add(IR(Instruction.IFNOT, array(genPred.last().ret, trueCount + 1, 0), name = e.test.toString()))
            ret.addAll(genTrue)
        } else {
            val falseCount = genFalse.count { it.real }
            val temp = state.allocator.allocateReference(type = e.type(state))
            // The if body has a goto, include it in the count
            val jumpTrue = IR(Instruction.IFNOT, array(genPred.last().ret, (trueCount + 2) + 1, 0), name = e.test.toString())
            ret.add(jumpTrue)
            // if
            ret.addAll(genTrue)
            if (genTrue.isNotEmpty())
                ret.add(IR(Instruction.STORE_FLOAT, array(genTrue.last().ret, temp.ref), name = "store"))
            else
                jumpTrue.args[1]--
            // end if, jump to the instruction after the else
            val jumpFalse = IR(Instruction.GOTO, array(falseCount + 2, 0, 0), name = "end if")
            ret.add(jumpFalse)
            // else
            ret.addAll(genFalse)
            if (genFalse.isNotEmpty())
                ret.add(IR(Instruction.STORE_FLOAT, array(genFalse.last().ret, temp.ref), name = "store"))
            else
                jumpFalse.args[1]--
            // return
            ret.add(IR.Return(temp.ref))
        }
        return ret
    }

    override fun visit(e: ConstantExpression): List<IR> {
        val constant = state.allocator.allocateConstant(e.value, type = e.type(state))
        return listOf(IR.Return(constant.ref))
    }

    override fun visit(e: ContinueStatement): List<IR> {
        // filled in by Loop.doGenerate()
        return listOf(IR(Instruction.GOTO, array(0, 0, 0), name = e.toString()))
    }

    override fun visit(e: DeclarationExpression): List<IR> {
        if (e.id in state.allocator.scope.peek().lookup) {
            logger.warning { "redeclaring ${e.id}" }
        }
        val global = state.allocator.allocateReference(e.id, e.type(state), e.value?.evaluate(state))
        return listOf(IR.Return(global.ref))
    }

    override fun visit(e: FunctionExpression): List<IR> {
        if (e.id in state.allocator) {
            logger.warning { "redefining ${e.id}" }
        }

        val global = state.allocator.allocateFunction(e.id, type = e.type(state) as function_t)
        val f = ProgramData.Function(
                firstStatement = if (e.builtin == null)
                    0 // to be filled in later
                else
                    -e.builtin,
                firstLocal = 0,
                numLocals = 0,
                profiling = 0,
                nameOffset = state.allocator.allocateString(e.id).ref,
                fileNameOffset = 0,
                numParams = 0,
                sizeof = byteArray(0, 0, 0, 0, 0, 0, 0, 0)
        )
        state.allocator.push(e.id)
        val params = with(linkedListOf<Expression>()) {
            e.params?.let { addAll(it) }
            e.vararg?.let { add(it) }
            this
        }
        val genParams = params.flatMap { it.generate(state) }
        val children = e.children.flatMap { it.wrap { it.generate(state) } }
        run {
            // Calculate label jumps
            val labelIndices = linkedMapOf<String, Int>()
            val jumpIndices = linkedMapOf<String, Int>()
            children.fold(0, { i, it ->
                when {
                    it is IR.Label -> {
                        labelIndices[it.id] = i
                    }
                    it.instr == Instruction.GOTO && it.args[0] == 0 -> {
                        jumpIndices[state.gen.gotoLabels[it]] = i
                    }
                }
                if (it.real) i + 1 else i
            })
            val real = children.filter { it.real }
            for ((s, i) in jumpIndices) {
                real[i].args[0] = labelIndices[s] - i
            }
        }
        val list = (listOf(
                IR.Function("${e.id} -> ${global.ref}", f.copy(numLocals = state.allocator.references.size())))
                + genParams
                + children
                + IR(instr = Instruction.DONE, ret = global.ref, name = "endfunction"))
        state.allocator.pop()
        return list
    }

    override fun visit(e: GotoExpression): List<IR> {
        // filled in by new labels
        val instr = IR(Instruction.GOTO, array(0, 0, 0), name = e.toString())
        state.gen.gotoLabels[instr] = e.id
        return listOf(instr)
    }

    override fun visit(e: IndexExpression): List<IR> {
        return with(linkedListOf<IR>()) {
            val typeL = e.left.type(state)
            if (typeL is class_t) {
                val genL = e.left.generate()
                addAll(genL)
                val genR = e.right.generate()
                addAll(genR)
                val type = e.type(state)
                val out = state.allocator.allocateReference(type = type)
                add(IR(e.instr as? Instruction ?: Instruction.LOAD_FLOAT, array(genL.last().ret, genR.last().ret, out.ref), out.ref, e.toString()))
                this
            } else {
                visit(e : BinaryExpression)
            }
        }
    }

    override fun visit(e: LabelExpression): List<IR> {
        return listOf(IR.Label(e.id))
    }

    override fun visit(e: LoopExpression): List<IR> {
        val genInit = e.initializer?.flatMap { it.generate(state) }

        val genPred = e.predicate.generate(state)
        val predCount = genPred.count { it.real }

        val genBody = e.children.flatMap { it.generate(state) }
        val bodyCount = genBody.count { it.real }

        val genUpdate = e.update?.flatMap { it.generate(state) }
        val updateCount = genUpdate?.count { it.real } ?: 0

        val totalCount = bodyCount + updateCount + predCount

        val ret = linkedListOf<IR>()
        if (genInit != null) {
            ret.addAll(genInit)
        }
        ret.addAll(genPred)
        if (e.checkBefore) {
            ret.add(IR(Instruction.IFNOT, array(genPred.last().ret,
                    totalCount + /* the last if */ 1 + /* the next instruction */ 1, 0), name = "loop precondition"))
        }
        ret.addAll(genBody)
        if (genUpdate != null) {
            ret.addAll(genUpdate)
        }
        ret.addAll(genPred)
        ret.add(IR(Instruction.IF, array(genPred.last().ret, -totalCount, 0), name = e.predicate.toString()))

        // break/continue; jump to end
        genBody.filter { it.real }.forEachIndexed { i, IR ->
            if (IR.instr == Instruction.GOTO && IR.args[0] == 0) {
                val after = (bodyCount - 1) - i
                IR.args[0] = after + 1 + when (IR.args[1]) {
                // break
                    1 -> updateCount + predCount + /* if */ 1
                    else -> 0
                }
                IR.args[1] = 0
            }
        }
        return ret
    }

    override fun visit(e: MemberExpression): List<IR> {
        return with(linkedListOf<IR>()) {
            val genL = e.left.generate()
            addAll(genL)
            // check(e.field.owner is entity_t, "Field belongs to different type")
            val genR = state.fields[e.field.owner, e.field.id].generate()
            addAll(genR)
            val type = e.type(state)
            val out = state.allocator.allocateReference(type = type)
            add(IR(e.instr as? Instruction ?: Instruction.LOAD_FLOAT, array(genL.last().ret, genR.last().ret, out.ref), out.ref, e.toString()))
            this
        }
    }

    override fun visit(e: MemberReferenceExpression) = state.fields[e.owner, e.id].generate()

    override fun visit(e: MemoryReference): List<IR> {
        return listOf(IR.Return(e.ref))
    }

    override fun visit(e: MethodCallExpression): List<IR> {
        // TODO: increase this
        if (e.args.size() > 8) {
            logger.warning { "${e.function} takes ${e.args.size()} parameters" }
        }
        val args = e.args.take(8).map { it.generate(state) }
        fun instr(i: Int) = Instruction.from(Instruction.CALL0.ordinal() + i)
        var i = 0
        val prepare: List<IR> = args.map {
            val param = Instruction.OFS_PARAM(i++)
            IR(Instruction.STORE_FLOAT, array(it.last().ret, param), param, "Prepare param $i")
        }
        val global = state.allocator.allocateReference(type = e.type(state))
        with(linkedListOf<IR>()) {
            val genF = e.function.generate(state)
            addAll(genF)
            addAll(args.flatMap { it })
            addAll(prepare)
            add(IR(instr(i), array(genF.last().ret), Instruction.OFS_PARAM(-1), e.toString()))
            add(IR(Instruction.STORE_FLOAT, array(Instruction.OFS_PARAM(-1), global.ref), global.ref, "Save response"))
            return this
        }
    }

    override fun visit(e: Nop): List<IR> {
        return emptyList()
    }

    override fun visit(e: ParameterExpression): List<IR> {
        val memoryReference = MemoryReference(Instruction.OFS_PARAM(e.index), e.type)
        return with(linkedListOf<IR>()) {
            addAll(visit(e : DeclarationExpression))
            addAll(BinaryExpression.Assign(ReferenceExpression(e : DeclarationExpression), memoryReference).generate(state))
            this
        }
    }

    override fun visit(e: ReferenceExpression): List<IR> {
        val id = e.refers.id
        if (id !in state.allocator) {
            logger.severe { "unknown reference ${id}" }
        }
        // FIXME: null references
        val global = state.allocator[id]
        return listOf(IR.Return(global?.ref ?: 0))
    }

    override fun visit(e: DynamicReferenceExpression): List<IR> {
        val id = e.id
        if (id !in state.allocator) {
            logger.severe { "unknown late bound reference ${id}" }
        }
        // FIXME: null references
        val global = state.allocator[id]
        return listOf(IR.Return(global?.ref ?: 0))
    }

    override fun visit(e: ReturnStatement): List<IR> {
        val genRet = e.returnValue?.generate(state)
        val ret = linkedListOf<IR>()
        val args = array(0, 0, 0)
        if (genRet != null) {
            ret.addAll(genRet)
            args[0] = genRet.last().ret
        }
        ret.add(IR(Instruction.RETURN, args, 0, name = e.toString()))
        return ret
    }

    override fun visit(e: StructDeclarationExpression): List<IR> {
        val fields: List<IR> = e.struct.fields.flatMap {
            it.value.declare("${e.id}_${it.key}", state = state).flatMap {
                it.generate(state)
            }
        }
        val allocator = state.allocator
        allocator.scope.peek().lookup[e.id] = allocator.references[fields.first().ret]!!.dup(name = e.id, type = e.struct)
        return fields
    }

    override fun visit(e: SwitchExpression) = e.reduce().generate()

    override fun visit(e: UnaryExpression) = Types.handle<Q1VM.State, List<IR>>(Operation(e.op, e.operand.type(state)))(state, e.operand, null)
    override fun visit(e: UnaryExpression.Cast) = e.operand.generate()

}
