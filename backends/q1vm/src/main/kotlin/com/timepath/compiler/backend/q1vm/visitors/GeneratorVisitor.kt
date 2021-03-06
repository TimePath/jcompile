package com.timepath.compiler.backend.q1vm.visitors

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.*
import com.timepath.compiler.backend.q1vm.types.class_t
import com.timepath.compiler.backend.q1vm.types.float_t
import com.timepath.compiler.backend.q1vm.types.vector_t
import com.timepath.compiler.debug
import com.timepath.compiler.getTextWS
import com.timepath.compiler.ir.IR
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Types
import com.timepath.compiler.types.defaults.sizeOf
import com.timepath.compiler.types.defaults.struct_t
import com.timepath.q1vm.ProgramData
import java.util.concurrent.atomic.AtomicInteger

class GeneratorVisitor(val state: Q1VM.State) : ASTVisitor<List<IR>> {

    companion object {
        val logger = Logger()
    }

    @Suppress("NOTHING_TO_INLINE") inline
    fun <T> T.list() = listOf(this)

    @Suppress("NOTHING_TO_INLINE") inline
    fun Expression.generate(): List<IR> = accept(this@GeneratorVisitor)

    inline fun Expression.wrap(body: (Expression) -> List<IR>) = try {
        body(this)
    } catch(e: Exception) {
        when (e) {
            is UnsupportedOperationException -> {
                ctx?.let { ctx ->
                    val reason = e.message!!
                    logger.severe { "${ctx.debug()}: error: $reason\n${ctx.getTextWS()}\n" }
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
    override fun visit(e: BreakStatement) = IR.Basic(
            Instruction.GOTO.Break, name = e.toString()
    ).list()

    private val conditions = AtomicInteger()

    override fun visit(e: ConditionalExpression): List<IR> {
        val conditionId = "__cond_${conditions.andIncrement}"
        val falseLabel = "${conditionId}_false"
        val endLabel = "${conditionId}_end"

        val ret = linkedListOf<IR>()
        val genPred = e.test.wrap { it.generate() }
        ret.addAll(genPred)
        val genTrue = e.pass.wrap { it.generate() }
        val genFalse = e.fail?.wrap { it.generate() }
        if (genFalse == null) {
            IR.Basic(Instruction.GOTO.If(endLabel, genPred.last().ret, false), name = e.test.toString()).let { ret.add(it) }
            // if
            ret.addAll(genTrue)
            ret.add(IR.Basic(Instruction.LABEL(endLabel), name = "end"))
        } else {
            val temp = state.allocator.allocateReference(type = e.type(state), scope = Instruction.Ref.Scope.Local)
            IR.Basic(Instruction.GOTO.If(falseLabel, genPred.last().ret, false), name = e.test.toString()).let { ret.add(it) }
            // if
            ret.addAll(genTrue)
            if (genTrue.isNotEmpty())
                ret.add(IR.Basic(Instruction.STORE[float_t::class.java](genTrue.last().ret, temp.ref), name = "store"))
            IR.Basic(Instruction.GOTO.Label(endLabel), name = "goto end").let { ret.add(it) }
            // else
            ret.add(IR.Basic(Instruction.LABEL(falseLabel), name = "false"))
            ret.addAll(genFalse)
            if (genFalse.isNotEmpty())
                ret.add(IR.Basic(Instruction.STORE[float_t::class.java](genFalse.last().ret, temp.ref), name = "store"))
            // return
            ret.add(IR.Basic(Instruction.LABEL(endLabel), name = "end"))
            ret.add(IR.Return(temp.ref))
        }
        return ret
    }

    override fun visit(e: ConstantExpression) = state.allocator.allocateConstant(e.value, e.type(state), e.name).let {
        IR.Return(it.ref).list()
    }

    /** Filled in by visit(LoopExpression) */
    override fun visit(e: ContinueStatement) = IR.Basic(
            Instruction.GOTO.Continue, name = e.toString()
    ).list()

    override fun visit(e: DeclarationExpression): List<IR> {
        val type = e.type
        if (type !is vector_t && type is struct_t && type !is class_t) {
            return type.fields.flatMap {
                val memberType = it.value
                memberType.declare("${e.id}_${it.key}", null).generate()
            }.apply {
                state.allocator.let {
                    it.scope.peek().lookup[e.id] = it.references[first().ret]!!.copy(name = e.id, type = type)
                }
            }
        } else {
            val decl = when (e) {
                is AliasExpression -> e.alias
                else -> e
            }
            if (state.allocator.insideFunc) {
                val init = decl.value
                fun peek(id: String) = state.allocator.scope.peek().lookup[id]
                val global = peek(decl.id) ?: state.allocator.allocateReference(decl.id, type, null, scope = Instruction.Ref.Scope.Local)
                return IR.Declare(global).list() + (init?.let { e.ref().set(it).generate() } ?: emptyList())
            } else {
                val const = decl.value?.evaluate(state)
                val global = state.allocator[decl.id] ?: state.allocator.allocateReference(decl.id, type, const, scope = Instruction.Ref.Scope.Global)
                return IR.Declare(global).list()
            }
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
                    -e.builtin!!,
                firstLocal = 0,
                numLocals = 0,
                profiling = 0,
                nameOffset = state.allocator.allocateString(e.id).ref.i,
                fileNameOffset = 0,
                numParams = e.params.orEmpty().size,
                sizeof = ByteArray(8).apply {
                    e.params.orEmpty()
                            .asSequence()
                            .take(size)
                            .map { it.type.sizeOf() }
                            .forEachIndexed { i, size ->
                                this[i] = size.toByte()
                            }
                }
        )
        state.allocator.push(e)
        val params = linkedListOf<Expression>().apply {
            e.params?.let { addAll(it) }
            e.vararg?.let { add(it) }
        }
        val genParams = params.flatMap { it.generate() }
        val children = e.children.flatMap { it.wrap { it.generate() } }
        state.allocator.pop()

        return listOf(IR.Function(global, f, genParams + children + IR.Basic(
                // TODO: don't assume __return offset
                Instruction.RETURN(Instruction.Ref(e.params.orEmpty().sumBy { it.type.sizeOf() } + 1, Instruction.Ref.Scope.Local)), name = "done")
        ))
    }

    /** Filled in by new labels */
    override fun visit(e: GotoExpression) = IR.Basic(Instruction.GOTO.Label(e.id), name = e.toString()).list()

    override fun visit(e: LabelExpression) = IR.Label(e.id).list()

    private val loops = AtomicInteger()

    override fun visit(e: LoopExpression) = linkedListOf<IR>().apply {
        val loopId = "__loop_${loops.andIncrement}"
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
                add(IR.Basic(it, name = e.predicate.toString()))
            }
        }
        run {
            Instruction.LABEL(beginLabel).let { add(IR.Basic(it, name = "begin")) }
            genBody.let { addAll(it) }
            Instruction.LABEL(continueLabel).let { add(IR.Basic(it, name = "continue")) }
            genUpdate?.let { addAll(it) }
            genPred.let { addAll(it) }
            Instruction.GOTO.If(beginLabel, genPred.last().ret).let {
                add(IR.Basic(it, name = e.predicate.toString()))
            }
        }
        Instruction.LABEL(breakLabel).let { add(IR.Basic(it, name = "break")) }
    }

    override fun visit(e: IndexExpression): List<IR> {
        val typeL = e.left.type(state)
        if (typeL !is class_t) return visit(e as BinaryExpression)
        return linkedListOf<IR>().apply {
            val genL = e.left.generate()
            addAll(genL)
            val genR = e.right.generate()
            addAll(genR)
            val type = e.type(state)
            val out = state.allocator.allocateReference(type = type, scope = Instruction.Ref.Scope.Local)
            val instr = e.instr as? Instruction.Factory ?: Instruction.LOAD[float_t::class.java]
            add(IR.Basic(instr(genL.last().ret, genR.last().ret, out.ref), out.ref, e.toString()))
        }
    }

    override fun visit(e: MemberExpression): List<IR> = linkedListOf<IR>().apply {
        val left = e.left
        if (e.field.owner is class_t) {
            val genL = left.generate()
                    .apply { addAll(this) }
            // check(e.field.owner is entity_t, "Field belongs to different type")
            val genR = state.fields[e.field.owner, e.field.id].generate()
                    .apply { addAll(this) }
            val out = state.allocator.allocateReference(type = e.type(state), scope = Instruction.Ref.Scope.Local)
            val instr = e.instr as? Instruction.Factory ?: Instruction.LOAD[float_t::class.java]
            IR.Basic(instr(genL.last().ret, genR.last().ret, out.ref), out.ref, e.toString()).apply { add(this) }
        } else {
            if (left is MemberExpression) {
                // FIXME: generalise
                val obj = left.left
                val innerField = left.field
                val outerField = e.field
                val index = innerField.owner.offsetOf(innerField.id) + outerField.owner.offsetOf(outerField.id)

                val genL = obj.generate()
                        .apply { addAll(this) }
                val genR = Pointer(index).expr("${innerField.id}_${outerField.id}", outerField.type(state)).generate()
                        .apply { addAll(this) }
                val out = state.allocator.allocateReference(type = e.type(state), scope = Instruction.Ref.Scope.Local)
                val instr = e.instr as? Instruction.Factory ?: Instruction.LOAD[float_t::class.java]
                IR.Basic(instr(genL.last().ret, genR.last().ret, out.ref), out.ref, e.toString()).apply { add(this) }
            } else {
                val obj = left
                val field = e.field
                val genL = obj.generate().apply { addAll(this) }
                add(IR.Return(genL.last().ret + field.owner.offsetOf(field.id)))
            }
        }
    }

    override fun visit(e: MemberReferenceExpression) = state.fields[e.owner, e.id].generate()

    override fun visit(e: MemoryReference) = IR.Return(e.ref).list()

    override fun visit(e: MethodCallExpression) = linkedListOf<IR>().apply {
        // TODO: increase this
        if (e.args.size > 8) {
            logger.warning { "${e.function} takes ${e.args.size} parameters" }
        }
        val genF = e.function.generate().apply { addAll(this) }
        val args = e.args.asSequence().take(8).map { it.generate() }.toList()
        args.flatMapTo(this) { it }
        val returnType = e.type(state)
        val ret = state.allocator.allocateReference(type = returnType, scope = Instruction.Ref.Scope.Local)
        val params = args.zip(e.args) { it, other -> it.last().ret to other.type(state).javaClass }
        IR.Basic(Instruction.CALL[params](genF.last().ret), Instruction.OFS_PARAM(-1), "$e")
                .apply { add(this) }
        IR.Basic(Instruction.STORE[returnType.javaClass](Instruction.OFS_PARAM(-1), ret.ref), ret.ref, "Save response")
                .apply { add(this) }
    }

    override fun visit(e: Nop) = emptyList<IR>()

    override fun visit(e: ParameterExpression) = linkedListOf<IR>().apply {
        visit(e as DeclarationExpression)
                .apply { addAll(this) }
        e.ref().set(MemoryReference(Instruction.OFS_PARAM(e.index), e.type))
                .apply { addAll(generate()) }
    }

    override fun visit(e: ReferenceExpression): List<IR> {
        val id = e.refers.id
        val global = state.allocator[id]
        if (global == null) {
            logger.severe { "unknown reference $id" }
            throw NullPointerException("unknown reference $id")
        }
        return IR.Return(global.ref).list()
    }

    override fun visit(e: ReturnStatement): List<IR> {
        val ret = linkedListOf<IR>()
        val args = e.returnValue?.generate()?.let {
            ret.addAll(it)
            Instruction.Args(it.last().ret)
        } ?: Instruction.Args(Instruction.Ref(0, Instruction.Ref.Scope.Global))
        ret.add(IR.Basic(Instruction.RETURN(args), name = e.toString()))
        return ret
    }

    override fun visit(e: SwitchExpression): List<IR> {
        val reduced = e.reduce(state)
        return reduced.flatMap { it.generate() }
    }

    override fun visit(e: UnaryExpression) = Types.handle<Q1VM.State, List<IR>>(
            Operation(e.op, e.operand.type(state)))(state, e.operand, null)

    override fun visit(e: UnaryExpression.Cast) = e.operand.generate()

    fun post(e: UnaryExpression, f: (Expression) -> Expression) = BlockExpression(linkedListOf<Expression>().apply {
        val it = e.operand
        val tmp = it.type(state).declare("tmp", null).let {
            add(it)
            it.ref()
        }
        add(tmp set it)
        add(f(it))
        add(tmp)
    }).generate()

    override fun visit(e: UnaryExpression.PostIncrement) = post(e) { UnaryExpression.PreIncrement(it, null) }
    override fun visit(e: UnaryExpression.PostDecrement) = post(e) { UnaryExpression.PreDecrement(it, null) }

}
