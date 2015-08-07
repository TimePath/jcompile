package com.timepath.compiler.backend.q1vm.visitors

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.types.class_t
import com.timepath.compiler.backend.q1vm.types.float_t
import com.timepath.compiler.debug
import com.timepath.compiler.getTextWS
import com.timepath.compiler.ir.IR
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Types
import com.timepath.compiler.types.defaults.struct_t
import com.timepath.q1vm.ProgramData
import com.timepath.with
import java.util.concurrent.atomic.AtomicInteger

class GeneratorVisitor(val state: Q1VM.State) : ASTVisitor<List<IR>> {

    companion object {
        val logger = Logger()
    }

    suppress("NOTHING_TO_INLINE") inline
    fun T.list<T>() = listOf(this)

    suppress("NOTHING_TO_INLINE") inline
    fun Expression.generate(): List<IR> = accept(this@GeneratorVisitor)

    inline fun Expression.wrap(body: (Expression) -> List<IR>) = try {
        body(this)
    } catch(e: Exception) {
        when (e) {
            is UnsupportedOperationException -> {
                ctx?.let { ctx ->
                    val reason = e.getMessage()!!
                    logger.severe { "${ctx.debug()}: error: ${reason}\n${ctx.getTextWS()}\n" }
                    state.errors.add(Compiler.Err(ctx, reason))
                }
            }
            else -> throw e
        }
        listOf<IR>()
    }

    override fun visit(e: BinaryExpression): List<IR> {
        val op = Operation(e.op, e.left.type(state), e.right.type(state))
        return Types.handle<Q1VM.State, List<IR>>(op)(state, e.left, e.right)
    }

    override fun visit(e: BlockExpression): List<IR> {
        state.allocator.push(e)
        val ret = e.children.flatMap {
            it.wrap { it.generate() }
        }
        state.allocator.pop()
        return ret
    }

    /** Filled in by visit(LoopExpression) */
    override fun visit(e: BreakStatement) = IR(
            Instruction.GOTO.Break, name = e.toString()
    ).list()

    private val conditions = AtomicInteger()

    override fun visit(e: ConditionalExpression): List<IR> {
        val conditionId = "__cond_${conditions.getAndIncrement()}"
        val falseLabel = "${conditionId}_false"
        val endLabel = "${conditionId}_end"

        val ret = linkedListOf<IR>()
        val genPred = e.test.wrap { it.generate() }
        ret.addAll(genPred)
        val genTrue = e.pass.wrap { it.generate() }
        val genFalse = e.fail?.wrap { it.generate() }
        if (genFalse == null) {
            IR(Instruction.GOTO.If(endLabel, genPred.last().ret, false), name = e.test.toString()).let { ret.add(it) }
            // if
            ret.addAll(genTrue)
            ret.add(IR(Instruction.LABEL(endLabel), name = "end"))
        } else {
            val temp = state.allocator.allocateReference(type = e.type(state), scope = Instruction.Ref.Scope.Local)
            IR(Instruction.GOTO.If(falseLabel, genPred.last().ret, false), name = e.test.toString()).let { ret.add(it) }
            // if
            ret.addAll(genTrue)
            if (genTrue.isNotEmpty())
                ret.add(IR(Instruction.STORE[javaClass<float_t>()](genTrue.last().ret, temp.ref), name = "store"))
            IR(Instruction.GOTO.Label(endLabel), name = "goto end").let { ret.add(it) }
            // else
            ret.add(IR(Instruction.LABEL(falseLabel), name = "false"))
            ret.addAll(genFalse)
            if (genFalse.isNotEmpty())
                ret.add(IR(Instruction.STORE[javaClass<float_t>()](genFalse.last().ret, temp.ref), name = "store"))
            // return
            ret.add(IR(Instruction.LABEL(endLabel), name = "end"))
            ret.add(IR.Return(temp.ref))
        }
        return ret
    }

    override fun visit(e: ConstantExpression) = state.allocator.allocateConstant(e.value, type = e.type(state)).let {
        IR.Return(it.ref).list()
    }

    /** Filled in by visit(LoopExpression) */
    override fun visit(e: ContinueStatement) = IR(
            Instruction.GOTO.Continue, name = e.toString()
    ).list()

    override fun visit(e: DeclarationExpression): List<IR> {
        val type = e.type
        if (type is struct_t && type !is class_t) {
            return type.fields.flatMap {
                it.value.declare("${e.id}_${it.key}", null).generate()
            } with {
                state.allocator.let {
                    it.scope.peek().lookup[e.id] = it.references[first().ret]!!.copy(name = e.id, type = type)
                }
            }
        } else {
            val decl = when (e) {
                is AliasExpression -> e.alias
                else -> e
            }
            val scope = when (state.allocator.scope.size() > 3) {
                true -> Instruction.Ref.Scope.Local
                else -> Instruction.Ref.Scope.Global
            }
            val global = state.allocator[decl.id] ?: state.allocator.allocateReference(decl.id, type, decl.value?.evaluate(state), scope = scope)
            return IR.Declare(global).list()
        }
    }

    override fun visit(e: FunctionExpression): List<IR> {
        val global = state.allocator[e.id]
        if (global == null) {
            logger.warning { "undefined ${e.id}" }
            throw NullPointerException("undefined ${e.id}")
        }
        val f = ProgramData.Function(
                firstStatement = if (e.builtin == null)
                    0 // to be filled in later
                else
                    -e.builtin,
                firstLocal = 0,
                numLocals = 0,
                profiling = 0,
                nameOffset = state.allocator.allocateString(e.id).ref.i,
                fileNameOffset = 0,
                numParams = 0,
                sizeof = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        )
        state.allocator.push(e)
        val params = linkedListOf<Expression>() with {
            e.params?.let { addAll(it) }
            e.vararg?.let { add(it) }
        }
        val genParams = params.flatMap { it.generate() }
        val children = e.children.flatMap { it.wrap { it.generate() } }
        val list = (listOf(
                IR.Function(global, f))
                + genParams
                + children
                + IR.EndFunction(global.ref))
        state.allocator.pop()
        return list
    }

    /** Filled in by new labels */
    override fun visit(e: GotoExpression) = IR(Instruction.GOTO.Label(e.id), name = e.toString()).list()

    override fun visit(e: LabelExpression) = IR.Label(e.id).list()

    private val loops = AtomicInteger()

    override fun visit(e: LoopExpression) = linkedListOf<IR>() with {
        val loopId = "__loop_${loops.getAndIncrement()}"
        val beginLabel = "${loopId}_begin"
        val continueLabel = "${loopId}_continue"
        val breakLabel = "${loopId}_break"

        val genInit = e.initializer?.flatMap { it.generate() }
        val genPred = e.predicate.generate()
        val genBody = e.children.asSequence().flatMap {
            it.generate().asSequence().map {
                when (it.instr) {
                    is Instruction.GOTO.Break -> it.copy(Instruction.GOTO.Label(breakLabel))
                    is Instruction.GOTO.Continue -> it.copy(Instruction.GOTO.Label(continueLabel))
                    else -> it
                }
            }
        }.toList()
        val genUpdate = e.update?.flatMap { it.generate() }

        genInit?.let { addAll(it) }
        genPred.let { addAll(it) }
        if (e.checkBefore) {
            Instruction.GOTO.If(breakLabel, genPred.last().ret, false).let {
                add(IR(it, name = e.predicate.toString()))
            }
        }
        run {
            Instruction.LABEL(beginLabel).let { add(IR(it, name = "begin")) }
            genBody.let { addAll(it) }
            Instruction.LABEL(continueLabel).let { add(IR(it, name = "continue")) }
            genUpdate?.let { addAll(it) }
            genPred.let { addAll(it) }
            Instruction.GOTO.If(beginLabel, genPred.last().ret).let {
                add(IR(it, name = e.predicate.toString()))
            }
        }
        Instruction.LABEL(breakLabel).let { add(IR(it, name = "break")) }
    }

    override fun visit(e: IndexExpression): List<IR> {
        val typeL = e.left.type(state)
        if (typeL !is class_t) return visit(e as BinaryExpression)
        return linkedListOf<IR>() with {
            val genL = e.left.generate()
            addAll(genL)
            val genR = e.right.generate()
            addAll(genR)
            val type = e.type(state)
            val out = state.allocator.allocateReference(type = type, scope = Instruction.Ref.Scope.Local)
            val instr = e.instr as? Instruction.Factory ?: Instruction.LOAD[javaClass<float_t>()]
            add(IR(instr(genL.last().ret, genR.last().ret, out.ref), out.ref, e.toString()))
        }
    }

    override fun visit(e: MemberExpression): List<IR> = linkedListOf<IR>() with {
        if (e.field.owner is class_t) {
            val genL = e.left.generate()
                    .with { addAll(this) }
            // check(e.field.owner is entity_t, "Field belongs to different type")
            val genR = state.fields[e.field.owner, e.field.id].generate()
                    .with { addAll(this) }
            val out = state.allocator.allocateReference(type = e.type(state), scope = Instruction.Ref.Scope.Local)
            val instr = e.instr as? Instruction.Factory ?: Instruction.LOAD[javaClass<float_t>()]
            IR(instr(genL.last().ret, genR.last().ret, out.ref), out.ref, e.toString()).with { add(this) }
        } else {
            if (e.left is MemberExpression) {
                // FIXME: generalise
                val obj = e.left.left
                val innerField = e.left.field
                val outerField = e.field
                val index = innerField.owner.offsetOf(innerField.id) + outerField.owner.offsetOf(outerField.id)

                val genL = obj.generate()
                        .with { addAll(this) }
                val genR = Pointer(index).expr().generate()
                        .with { addAll(this) }
                val out = state.allocator.allocateReference(type = e.type(state), scope = Instruction.Ref.Scope.Local)
                val instr = e.instr as? Instruction.Factory ?: Instruction.LOAD[javaClass<float_t>()]
                IR(instr(genL.last().ret, genR.last().ret, out.ref), out.ref, e.toString()).with { add(this) }
            } else {
                val obj = e.left
                val field = e.field
                val genL = obj.generate().with { addAll(this) }
                add(IR.Return(genL.last().ret + field.owner.offsetOf(field.id)))
            }
        }
    }

    override fun visit(e: MemberReferenceExpression) = state.fields[e.owner, e.id].generate()

    override fun visit(e: MemoryReference) = IR.Return(e.ref).list()

    override fun visit(e: MethodCallExpression) = linkedListOf<IR>() with {
        // TODO: increase this
        if (e.args.size() > 8) {
            logger.warning { "${e.function} takes ${e.args.size()} parameters" }
        }
        val args = e.args.asSequence().take(8).map { it.generate() }.toList()
        val ret = state.allocator.allocateReference(type = e.type(state), scope = Instruction.Ref.Scope.Local)
        val genF = e.function.generate()
                .with { addAll(this) }
        args.flatMapTo(this) { it }
        val types = e.args.iterator()
        val params = args.map { it.last().ret to types.next().type(state).javaClass }
        IR(Instruction.CALL[params](genF.last().ret), Instruction.OFS_PARAM(-1), "$e")
                .with { add(this) }
        IR(Instruction.STORE[javaClass<float_t>()](Instruction.OFS_PARAM(-1), ret.ref), ret.ref, "Save response")
                .with { add(this) }
    }

    override fun visit(e: Nop) = emptyList<IR>()

    override fun visit(e: ParameterExpression) = linkedListOf<IR>() with {
        visit(e as DeclarationExpression)
                .with { addAll(this) }
        e.ref().set(MemoryReference(Instruction.OFS_PARAM(e.index), e.type))
                .with { addAll(generate()) }
    }

    override fun visit(e: ReferenceExpression): List<IR> {
        val id = e.refers.id
        val global = state.allocator[id]
        if (global == null) {
            logger.severe { "unknown reference ${id}" }
            throw NullPointerException("unknown reference ${id}")
        }
        return IR.Return(global.ref).list()
    }

    override fun visit(e: DynamicReferenceExpression): List<IR> {
        val id = e.id
        if (id !in state.allocator) {
            logger.severe { "unknown late bound reference ${id}" }
        }
        // FIXME: null references
        val global = state.allocator[id]
        return IR.Return(global?.ref ?: Instruction.Ref.Null).list()
    }

    override fun visit(e: ReturnStatement): List<IR> {
        val ret = linkedListOf<IR>()
        val args = e.returnValue?.generate()?.let {
            ret.addAll(it)
            Instruction.Args(it.last().ret)
        } ?: Instruction.Args(Instruction.Ref(0, Instruction.Ref.Scope.Global))
        ret.add(IR(Instruction.RETURN(args), name = e.toString()))
        return ret
    }

    override fun visit(e: SwitchExpression): List<IR> {
        val reduced = e.reduce(state)
        return reduced.flatMap { it.generate() }
    }

    override fun visit(e: UnaryExpression) = Types.handle<Q1VM.State, List<IR>>(
            Operation(e.op, e.operand.type(state)))(state, e.operand, null)

    override fun visit(e: UnaryExpression.Cast) = e.operand.generate()

}
