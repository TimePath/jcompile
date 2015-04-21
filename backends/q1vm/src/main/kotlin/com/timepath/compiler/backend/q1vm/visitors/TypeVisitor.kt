package com.timepath.compiler.backend.q1vm.visitors

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.Types
import com.timepath.compiler.types.defaults.function_t

class TypeVisitor(val state: Q1VM.State) : ASTVisitor<Type> {

    fun Expression.type(): Type = accept(this@TypeVisitor)

    override fun visit(e: Nop) = void_t

    override fun visit(e: BinaryExpression) = Types.type(Operation(e.op, e.left.type(), e.right.type()))
    override fun visit(e: BinaryExpression.Add) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.AddAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.And) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.AndAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Assign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.BitAnd) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.BitOr) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Comma) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Divide) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.DivideAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Eq) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.ExclusiveOr) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.ExclusiveOrAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Ge) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Gt) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Le) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Lsh) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.LshAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Lt) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Modulo) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.ModuloAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Multiply) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.MultiplyAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Ne) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Or) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.OrAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Rsh) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.RshAssign) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.Subtract) = visit(e: BinaryExpression)
    override fun visit(e: BinaryExpression.SubtractAssign) = visit(e: BinaryExpression)

    override fun visit(e: BlockExpression) = e.children.lastOrNull()?.type() ?: void_t

    override fun visit(e: ConditionalExpression): Type {
        val type = e.pass.type()
        return when (e.fail) {
            null -> void_t // NullableType(type)
            else -> type
        }
    }

    override fun visit(e: ConstantExpression) = Types.from(e.value.any)

    override fun visit(e: IndexExpression): Type {
        val typeL = e.left.type()
        return when (typeL) {
            is entity_t ->
                (e.right.type() as field_t).type
            is array_t ->
                typeL.type
            else -> visit(e:BinaryExpression)
        }
    }

    override fun visit(e: MemberExpression) = e.field.type()

    override fun visit(e: MemberReferenceExpression) = e.owner.fields[e.id]!!

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

    // FIXME: hack
    override fun visit(e: DynamicReferenceExpression): Type {
        state.allocator[e.id]?.let { return it.type }
        // probably a vector component
        //  return float_t
        return field_t(float_t)
        //  throw NullPointerException("Reference ${e.id} not found")
    }

    override fun visit(e: DeclarationExpression) = e.type
    override fun visit(e: ParameterExpression) = visit(e: DeclarationExpression)
    override fun visit(e: StructDeclarationExpression) = visit(e: DeclarationExpression)

    override fun visit(e: MemoryReference) = e.type

    override fun visit(e: UnaryExpression.Cast) = e.type

    override fun visit(e: UnaryExpression) = Types.type(Operation(e.op, e.operand.type()))
    override fun visit(e: UnaryExpression.Address) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.BitNot) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.Dereference) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.Minus) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.Not) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.Plus) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.Post) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.PostDecrement) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.PostIncrement) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.PreDecrement) = visit(e: UnaryExpression)
    override fun visit(e: UnaryExpression.PreIncrement) = visit(e: UnaryExpression)
}
