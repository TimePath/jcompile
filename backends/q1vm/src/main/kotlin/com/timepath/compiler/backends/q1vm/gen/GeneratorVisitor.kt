package com.timepath.compiler.backends.q1vm.gen

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backends.q1vm.Q1VM
import com.timepath.compiler.data.Pointer
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Types
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.types.entity_t
import com.timepath.q1vm.Instruction
import com.timepath.q1vm.ProgramData
import java.util.LinkedList

// TODO: push up
fun Expression.generate(state: CompileState): List<IR> = accept(GeneratorVisitor(state as Q1VM.State))

class GeneratorVisitor(val state: Q1VM.State) : ASTVisitor<List<IR>> {

    [suppress("NOTHING_TO_INLINE")]
    inline fun Expression.generate(): List<IR> = accept(this@GeneratorVisitor)

    override fun visit(e: BinaryExpression) = Types.handle<List<IR>>(Operation(e.op, e.left.type(state), e.right.type(state)))(state, e.left, e.right)
    override fun visit(e: BinaryExpression.Add) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.AddAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.And) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.AndAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Assign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.BitAnd) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.BitOr) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Comma) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Divide) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.DivideAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Eq) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.ExclusiveOr) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.ExclusiveOrAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Ge) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Gt) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Le) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Lsh) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.LshAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Lt) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Modulo) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.ModuloAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Multiply) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.MultiplyAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Ne) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Or) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.OrAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Rsh) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.RshAssign) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.Subtract) = visit(e : BinaryExpression)
    override fun visit(e: BinaryExpression.SubtractAssign) = visit(e : BinaryExpression)

    override fun visit(e: BlockExpression): List<IR> {
        state.allocator.push(this)
        val list = e.children.flatMap {
            it.generate(state)
        }
        state.allocator.pop()
        return list
    }

    override fun visit(e: BreakStatement): List<IR> {
        // filled in by Loop.doGenerate()
        return listOf(IR(Instruction.GOTO, array(0, 1, 0)))
    }

    override fun visit(e: ConditionalExpression): List<IR> {
        with(e) {
            val ret = linkedListOf<IR>()
            val genPred = test.generate(state)
            ret.addAll(genPred)
            val genTrue = pass.generate(state)
            val trueCount = genTrue.count { it.real }
            val genFalse = fail?.generate(state)
            if (genFalse == null) {
                // No else, jump to the instruction after the body
                ret.add(IR(Instruction.IFNOT, array(genPred.last().ret, trueCount + 1, 0)))
                ret.addAll(genTrue)
            } else {
                val falseCount = genFalse.count { it.real }
                val temp = state.allocator.allocateReference(type = type(state))
                // The if body has a goto, include it in the count
                val jumpTrue = IR(Instruction.IFNOT, array(genPred.last().ret, (trueCount + 2) + 1, 0))
                ret.add(jumpTrue)
                // if
                ret.addAll(genTrue)
                if (genTrue.isNotEmpty())
                    ret.add(IR(Instruction.STORE_FLOAT, array(genTrue.last().ret, temp.ref)))
                else
                    jumpTrue.args[1]--
                // end if, jump to the instruction after the else
                val jumpFalse = IR(Instruction.GOTO, array(falseCount + 2, 0, 0))
                ret.add(jumpFalse)
                // else
                ret.addAll(genFalse)
                if (genFalse.isNotEmpty())
                    ret.add(IR(Instruction.STORE_FLOAT, array(genFalse.last().ret, temp.ref)))
                else
                    jumpFalse.args[1]--
                // return
                ret.add(ReferenceIR(temp.ref))
            }
            return ret
        }
    }

    override fun visit(e: ConstantExpression): List<IR> {
        val constant = state.allocator.allocateConstant(e.value, type = e.type(state))
        return listOf(ReferenceIR(constant.ref))
    }

    override fun visit(e: ContinueStatement): List<IR> {
        // filled in by Loop.doGenerate()
        return listOf(IR(Instruction.GOTO, array(0, 0, 0)))
    }

    override fun visit(e: DeclarationExpression): List<IR> {
        if (e.id in state.allocator.scope.peek().lookup) {
            Generator.logger.warning("redeclaring ${e.id}")
        }
        val global = state.allocator.allocateReference(e.id, e.type(state), e.value?.evaluate())
        return listOf(ReferenceIR(global.ref))
    }

    override fun visit(e: FunctionExpression): List<IR> {
        with(e) {
            if (id in state.allocator) {
                Generator.logger.warning("redefining $id")
            }

            val global = state.allocator.allocateFunction(id, type = type(state) as function_t)
            val f = ProgramData.Function(
                    firstStatement = if (builtin == null)
                        0 // to be filled in later
                    else
                        -builtin,
                    firstLocal = 0,
                    numLocals = 0,
                    profiling = 0,
                    nameOffset = state.allocator.allocateString(id).ref,
                    fileNameOffset = 0,
                    numParams = 0,
                    sizeof = byteArray(0, 0, 0, 0, 0, 0, 0, 0)
            )
            state.allocator.push(id)
            val params = with(linkedListOf<Expression>()) {
                params?.let { addAll(it) }
                vararg?.let { add(it) }
                this
            }
            val genParams = params.flatMap { it.generate(state) }
            val children = children.flatMap { it.generate(state) }
            run {
                // Calculate label jumps
                val labelIndices = linkedMapOf<String, Int>()
                val jumpIndices = linkedMapOf<String, Int>()
                children.fold(0, { i, it ->
                    when {
                        it is LabelIR -> {
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
                    FunctionIR(f))
                    + genParams
                    + children
                    + IR(instr = Instruction.DONE)
                    + ReferenceIR(global.ref))
            state.allocator.pop()
            return list
        }
    }

    override fun visit(e: GotoExpression): List<IR> {
        // filled in by new labels
        val instr = IR(Instruction.GOTO, array(0, 0, 0))
        state.gen.gotoLabels[instr] = e.id
        return listOf(instr)
    }

    override fun visit(e: IndexExpression): List<IR> {
        with(e) {
            with(linkedListOf<IR>()) {
                val typeL = left.type(state)
                if (typeL is entity_t) {
                    val genL = left.generate(state)
                    addAll(genL)
                    val genR = right.generate(state)
                    addAll(genR)
                    val out = state.allocator.allocateReference(type = type(state))
                    add(IR(instr as? Instruction ?: Instruction.LOAD_FLOAT,
                            array(genL.last().ret, genR.last().ret, out.ref), out.ref, this.toString()))
                    return this
                } else {
                    return visit(e : BinaryExpression)
                }
            }
        }
    }

    override fun visit(e: LabelExpression): List<IR> {
        return listOf(LabelIR(e.id))
    }

    override fun visit(e: LoopExpression): List<IR> {
        with(e) {
            val genInit = initializer?.flatMap { it.generate(state) }

            val genPred = predicate.generate(state)
            val predCount = genPred.count { it.real }

            val genBody = children.flatMap { it.generate(state) }
            val bodyCount = genBody.count { it.real }

            val genUpdate = update?.flatMap { it.generate(state) }
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
    }

    override fun visit(e: MemberExpression): LinkedList<IR> {
        with(e) {
            return with(linkedListOf<IR>()) {
                val genL = left.generate(state)
                addAll(genL)
                val genR = ConstantExpression(Pointer(0)).generate(state) // TODO: field by name
                addAll(genR)
                val out = state.allocator.allocateReference(type = type(state))
                add(IR(instr as? Instruction ?: Instruction.LOAD_FLOAT, array(genL.last().ret, genR.last().ret, out.ref), out.ref, this.toString()))
                this
            }
        }
    }

    override fun visit(e: MemoryReference): List<IR> {
        return listOf(ReferenceIR(e.ref))
    }

    override fun visit(e: MethodCallExpression): List<IR> {
        with(e) {
            // TODO: increase this
            if (args.size() > 8) {
                Generator.logger.warning("${function} takes ${args.size()} parameters")
            }
            val args = args.take(8).map { it.generate(state) }
            fun instr(i: Int) = Instruction.from(Instruction.CALL0.ordinal() + i)
            var i = 0
            val prepare: List<IR> = args.map {
                val param = Instruction.OFS_PARAM(i++)
                IR(Instruction.STORE_FLOAT, array(it.last().ret, param), param, "Prepare param $i")
            }
            val global = state.allocator.allocateReference(type = type(state))
            with(linkedListOf<IR>()) {
                val genF = function.generate(state)
                addAll(genF)
                addAll(args.flatMap { it })
                addAll(prepare)
                add(IR(instr(i), array(genF.last().ret), Instruction.OFS_PARAM(-1)))
                add(IR(Instruction.STORE_FLOAT, array(Instruction.OFS_PARAM(-1), global.ref), global.ref, "Save response"))
                return this
            }
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
            Generator.logger.severe("unknown reference ${id}")
        }
        // FIXME: null references
        val global = state.allocator[id]
        return listOf(ReferenceIR(global?.ref ?: 0))
    }

    override fun visit(e: DynamicReferenceExpression): List<IR> {
        val id = e.id
        if (id !in state.allocator) {
            Generator.logger.severe("unknown reference $id")
        }
        // FIXME: null references
        val global = state.allocator[id]
        return listOf(ReferenceIR(global?.ref ?: 0))
    }

    override fun visit(e: ReturnStatement): List<IR> {
        val genRet = e.returnValue?.generate(state)
        val ret = linkedListOf<IR>()
        val args = array(0, 0, 0)
        if (genRet != null) {
            ret.addAll(genRet)
            args[0] = genRet.last().ret
        }
        ret.add(IR(Instruction.RETURN, args, 0))
        return ret
    }

    override fun visit(e: StructDeclarationExpression): List<IR> {
        with(e) {
            val fields: List<IR> = struct.fields.flatMap {
                it.value.declare("${id}_${it.key}", null).flatMap {
                    it.generate(state)
                }
            }
            val allocator = state.allocator
            allocator.scope.peek().lookup[id] = allocator.references[fields.first().ret]!!.copy(name = id, type = struct)
            return fields
        }
    }

    override fun visit(e: SwitchExpression): List<IR> {
        return e.reduce()!!.generate()
    }

    override fun visit(e: SwitchExpression.Case): List<IR> {
        return listOf(CaseIR(e.expr))
    }

    override fun visit(e: UnaryExpression) = Types.handle<List<IR>>(Operation(e.op, e.operand.type(state)))(state, e.operand, null)
    override fun visit(e: UnaryExpression.Address) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.BitNot) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.Cast) = e.operand.generate()
    override fun visit(e: UnaryExpression.Dereference) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.Minus) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.Not) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.Plus) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.Post) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.PostDecrement) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.PostIncrement) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.PreDecrement) = visit(e : UnaryExpression)
    override fun visit(e: UnaryExpression.PreIncrement) = visit(e : UnaryExpression)

}
