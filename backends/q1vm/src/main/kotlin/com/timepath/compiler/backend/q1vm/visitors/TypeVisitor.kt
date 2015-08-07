package com.timepath.compiler.backend.q1vm.visitors

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.types.array_t
import com.timepath.compiler.backend.q1vm.types.class_t
import com.timepath.compiler.backend.q1vm.types.field_t
import com.timepath.compiler.backend.q1vm.types.void_t
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.Types
import com.timepath.compiler.types.defaults.function_t

class TypeVisitor(val state: Q1VM.State) : ASTVisitor<Type> {

    suppress("NOTHING_TO_INLINE") inline fun Expression.type(): Type = accept(this@TypeVisitor)

    override fun visit(e: Nop) = void_t

    override fun visit(e: BinaryExpression) = Types.type(Operation(e.op, e.left.type(), e.right.type()))

    override fun visit(e: BlockExpression) = e.children.lastOrNull()?.type() ?: void_t

    override fun visit(e: ConditionalExpression): Type {
        val type = e.pass.type()
        return when (e.fail) {
            null -> void_t // NullableType(type)
            else -> type
        }
    }

    override fun visit(e: ConstantExpression) = e.type ?: Types.from(e.value.any)

    override fun visit(e: IndexExpression): Type {
        val typeL = e.left.type()
        return when (typeL) {
            is class_t ->
                (e.right.type() as field_t).type
            is array_t ->
                typeL.type
            else -> visit(e as BinaryExpression)
        }
    }

    override fun visit(e: MemberExpression) = (e.field.type() as field_t).type

    override fun visit(e: MemberReferenceExpression): field_t {
        val type = e.owner.fields[e.id]
        if (type == null) {
            throw NullPointerException("${e.owner}.${e.id} is null")
        }
        return field_t(type)
    }

    override fun visit(e: FunctionExpression) = e.type

    override fun visit(e: GotoExpression) = void_t

    override fun visit(e: ReturnStatement) = e.returnValue?.type() ?: void_t

    override fun visit(e: BreakStatement) = void_t

    override fun visit(e: ContinueStatement) = void_t

    override fun visit(e: LoopExpression) = void_t

    override fun visit(e: MethodCallExpression) = (e.function.type() as function_t).type

    // FIXME
    override fun visit(e: SwitchExpression) = e.test.type()

    override fun visit(e: SwitchExpression.Case) = e.expr?.type() ?: void_t

    override fun visit(e: ReferenceExpression) = e.refers.type

    override fun visit(e: DeclarationExpression) = e.type

    override fun visit(e: MemoryReference) = e.type

    override fun visit(e: UnaryExpression.Cast) = e.type

    override fun visit(e: UnaryExpression) = Types.type(Operation(e.op, e.operand.type()))
}
