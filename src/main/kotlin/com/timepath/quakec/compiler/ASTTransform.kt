package com.timepath.quakec.compiler

import com.timepath.quakec.Logging
import com.timepath.quakec.QCBaseVisitor
import com.timepath.quakec.QCParser
import com.timepath.quakec.compiler.ast.*
import com.timepath.quakec.vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

class ASTTransform : QCBaseVisitor<List<Statement>>() {

    class object {
        val logger = Logging.new()
    }

    private fun debug(ctx: ParserRuleContext) {
        val token = ctx.start
        val source = token.getTokenSource()

        val line = token.getLine()
        val col = token.getCharPositionInLine()
        val file = source.getSourceName()
        logger.fine("{$token} $line,$col $file")
    }

    override fun defaultResult(): List<Statement> = emptyList()

    override fun aggregateResult(aggregate: List<Statement>, nextResult: List<Statement>): List<Statement> {
        return aggregate + nextResult
    }

    override fun visitCompilationUnit(ctx: QCParser.CompilationUnitContext): List<Statement> {
        return listOf(BlockStatement(visitChildren(ctx)))
    }

    override fun visitCompoundStatement(ctx: QCParser.CompoundStatementContext): List<Statement> {
        return listOf(BlockStatement(visitChildren(ctx)))
    }

    override fun visitFunctionDefinition(ctx: QCParser.FunctionDefinitionContext): List<Statement> {
        var declarator = ctx.declarator()
        while (declarator.declarator() != null) {
            declarator = declarator.declarator()
        }
        val id = declarator.getText()

        val old = ctx.declarator().parameterTypeList() == null
        val paramContext = if (old) {
            ctx.declarationSpecifiers().declarationSpecifier().map {
                it.typeSpecifier().directTypeSpecifier().parameterTypeList()
            }
        } else {
            listOf(ctx.declarator().parameterTypeList())
        }
        val paramDeclarations = paramContext.flatMap {
            it.parameterList().parameterDeclaration()
        }
        val params = paramDeclarations.mapIndexed {(i, it) ->
            val paramId = it.declarator()?.getText()
            if (paramId != null) {
                val declarationExpression = DeclarationExpression(paramId, null)
                val memoryReference = MemoryReference(Instruction.OFS_PARAM(i))
                listOf(declarationExpression, BinaryExpression.Assign(declarationExpression, memoryReference))
            } else {
                emptyList()
            }
        }.flatMap { it }

        val varargId = ctx.declarator()?.parameterTypeList()?.parameterVarargs()?.Identifier()
        val vararg: List<Statement> = if (varargId != null) {
            listOf(DeclarationExpression(varargId.getText(), null))
        } else {
            emptyList()
        }
        return listOf(FunctionLiteral(id, Type.Void, array(), params + vararg + visitChildren(ctx.compoundStatement())))
    }

    override fun visitDeclaration(ctx: QCParser.DeclarationContext): List<Statement> {
        val declarations = ctx.initDeclaratorList()?.initDeclarator()
        if (declarations == null) {
            val enum = ctx.enumSpecifier()
            return enum.enumeratorList().enumerator().map {
                val id = it.enumerationConstant().getText()
                DeclarationExpression(id)
            }
        }
        return declarations.map {(it: QCParser.InitDeclaratorContext): Expression ->
            var declarator = it.declarator()
            while (declarator.declarator() != null) {
                declarator = declarator.declarator()
            }
            val id = declarator.getText()
            val initializer = it.initializer()?.accept(this)?.single()
            val constantExpression = if (initializer != null) initializer as ConstantExpression else null
            if (constantExpression != null) {
                val value = constantExpression.evaluate()
                val s = value.value.toString()
                if (s.startsWith('#')) {
                    // FIXME: HACK
                    return@map FunctionLiteral(id, builtin = s.substring(1).toInt())
                }
            }
            return@map DeclarationExpression(id, constantExpression)
        }
    }

    override fun visitLabeledStatement(ctx: QCParser.LabeledStatementContext): List<Statement> {
        val id = ctx.Identifier()?.getText()
        // TODO: custom node
        return if (id != null) listOf(DeclarationExpression(id)) else emptyList()
    }

    override fun visitJumpStatement(ctx: QCParser.JumpStatementContext): List<Statement> {
        // TODO: break, continue
        val expr = ctx.expression()
        val ret = if (expr != null) {
            val retVal = expr.accept(this).single()
            ReturnStatement(retVal as Expression)
        } else {
            ReturnStatement(null)
        }
        return listOf(ret)
    }

    override fun visitIterationStatement(ctx: QCParser.IterationStatementContext): List<Statement> {
        val predicate = ctx.predicate
        val predicateExpr = when (predicate) {
            null -> ConstantExpression(1)
            else -> predicate.accept(this).single()
        }

        val bodyStmt = ctx.statement().accept(this).single()
        val checkBefore = !ctx.getText().startsWith("do")
        val initializer = when {
            ctx.initD != null -> ctx.initD.accept(this).single()
            ctx.initE != null -> ctx.initE.accept(this).single()
            else -> null
        }
        val update = when (ctx.update) {
            null -> null
            else -> ctx.update.accept(this).single()
        }
        return listOf(Loop(predicateExpr as Expression, bodyStmt, checkBefore, initializer, update))
    }

    override fun visitIfStatement(ctx: QCParser.IfStatementContext): List<Statement> {
        val get = {(i: Int) -> ctx.statement()[i] }
        val test = ctx.expression()
        val yes = get(0)
        val no = when (ctx.statement().size()) {
            1 -> null
            else -> get(1)
        }
        val testExpr = test.accept(this).single()
        val yesExpr = yes.accept(this).single()
        val noExpr = no?.accept(this)?.single()
        return listOf(ConditionalExpression(testExpr as Expression, yesExpr, noExpr))
    }

    override fun visitExpressionStatement(ctx: QCParser.ExpressionStatementContext): List<Statement> {
        val expr = ctx.expression()
        if (expr != null) {
            return expr.accept(this)
        }
        // redundant semicolon
        return emptyList()
    }

    val QCParser.AssignmentExpressionContext.terminal: Boolean get() = assignmentExpression() != null

    override fun visitAssignmentExpression(ctx: QCParser.AssignmentExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.unaryExpression().accept(this).single()
            val right = ctx.assignmentExpression().accept(this).single()
            val ref = left as ReferenceExpression
            val value = right as Expression
            val assign = {(value: Expression): Expression ->
                BinaryExpression.Assign(ref, value)
            }
            val op = when (ctx.op.getType()) {
                QCParser.Assign -> assign(value)
                QCParser.OrAssign -> assign(BinaryExpression.BitOr(ref, value))
                QCParser.XorAssign -> assign(BinaryExpression.BitXor(ref, value))
                QCParser.AndAssign -> assign(BinaryExpression.BitAnd(ref, value))
                QCParser.LeftShiftAssign -> assign(BinaryExpression.Lsh(ref, value))
                QCParser.RightShiftAssign -> assign(BinaryExpression.Rsh(ref, value))
                QCParser.PlusAssign -> assign(BinaryExpression.Add(ref, value))
                QCParser.MinusAssign -> assign(BinaryExpression.Sub(ref, value))
                QCParser.StarAssign -> assign(BinaryExpression.Mul(ref, value))
                QCParser.DivAssign -> assign(BinaryExpression.Div(ref, value))
                QCParser.ModAssign -> assign(BinaryExpression.Mod(ref, value))
                else -> null
            }
            if (op != null) return listOf(op)
            return emptyList()
        }
        return super.visitAssignmentExpression(ctx)
    }

    val QCParser.ConditionalExpressionContext.terminal: Boolean get() = expression().isNotEmpty()

    override fun visitConditionalExpression(ctx: QCParser.ConditionalExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val condition = ctx.logicalOrExpression().accept(this).single()
            val yes = ctx.expression(0).accept(this).single()
            val no = ctx.expression(1).accept(this).single()
            return listOf(ConditionalExpression(condition as Expression, yes, no))
        }
        return super.visitConditionalExpression(ctx)
    }

    val QCParser.LogicalOrExpressionContext.terminal: Boolean get() = logicalOrExpression() != null

    override fun visitLogicalOrExpression(ctx: QCParser.LogicalOrExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.logicalOrExpression().accept(this).single()
            val right = ctx.logicalAndExpression().accept(this).single()
            return listOf(BinaryExpression.Or(left as Expression, right as Expression))
        }
        return super.visitLogicalOrExpression(ctx)
    }

    val QCParser.LogicalAndExpressionContext.terminal: Boolean get() = logicalAndExpression() != null

    override fun visitLogicalAndExpression(ctx: QCParser.LogicalAndExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.logicalAndExpression().accept(this).single()
            val right = ctx.inclusiveOrExpression().accept(this).single()
            return listOf(BinaryExpression.And(left as Expression, right as Expression))
        }
        return super.visitLogicalAndExpression(ctx)
    }

    val QCParser.InclusiveOrExpressionContext.terminal: Boolean get() = inclusiveOrExpression() != null

    override fun visitInclusiveOrExpression(ctx: QCParser.InclusiveOrExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.inclusiveOrExpression().accept(this).single()
            val right = ctx.exclusiveOrExpression().accept(this).single()
            return listOf(BinaryExpression.BitOr(left as Expression, right as Expression))
        }
        return super.visitInclusiveOrExpression(ctx)
    }

    val QCParser.ExclusiveOrExpressionContext.terminal: Boolean get() = exclusiveOrExpression() != null

    override fun visitExclusiveOrExpression(ctx: QCParser.ExclusiveOrExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.exclusiveOrExpression().accept(this).single()
            val right = ctx.andExpression().accept(this).single()
            return listOf(BinaryExpression.BitXor(left as Expression, right as Expression))
        }
        return super.visitExclusiveOrExpression(ctx)
    }

    val QCParser.AndExpressionContext.terminal: Boolean get() = andExpression() != null

    override fun visitAndExpression(ctx: QCParser.AndExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.andExpression().accept(this).single()
            val right = ctx.equalityExpression().accept(this).single()
            return listOf(BinaryExpression.BitAnd(left as Expression, right as Expression))
        }
        return super.visitAndExpression(ctx)
    }

    val QCParser.EqualityExpressionContext.terminal: Boolean get() = equalityExpression() != null

    override fun visitEqualityExpression(ctx: QCParser.EqualityExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.equalityExpression().accept(this).single()
            val right = ctx.relationalExpression().accept(this).single()
            val op = when (ctx.op.getType()) {
                QCParser.Equal -> BinaryExpression.Eq(left as Expression, right as Expression)
                QCParser.NotEqual -> BinaryExpression.Ne(left as Expression, right as Expression)
                else -> null
            }
            if (op != null) {
                return listOf(op)
            } else {
                return emptyList()
            }
        }
        return super.visitEqualityExpression(ctx)
    }

    val QCParser.RelationalExpressionContext.terminal: Boolean get() = relationalExpression() != null

    override fun visitRelationalExpression(ctx: QCParser.RelationalExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.relationalExpression().accept(this).single()
            val right = ctx.shiftExpression().accept(this).single()
            val op = when (ctx.op.getType()) {
                QCParser.Less -> BinaryExpression.Lt(left as Expression, right as Expression)
                QCParser.LessEqual -> BinaryExpression.Le(left as Expression, right as Expression)
                QCParser.Greater -> BinaryExpression.Gt(left as Expression, right as Expression)
                QCParser.GreaterEqual -> BinaryExpression.Ge(left as Expression, right as Expression)
                else -> null
            }
            if (op != null) {
                return listOf(op)
            } else {
                return emptyList()
            }
        }
        return super.visitRelationalExpression(ctx)
    }

    val QCParser.ShiftExpressionContext.terminal: Boolean get() = shiftExpression() != null

    override fun visitShiftExpression(ctx: QCParser.ShiftExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.shiftExpression().accept(this).single()
            val right = ctx.additiveExpression().accept(this).single()
            // TODO
            return listOf(BinaryExpression.Mul(left as Expression, right as Expression))
        }
        return super.visitShiftExpression(ctx)
    }

    val QCParser.AdditiveExpressionContext.terminal: Boolean get() = additiveExpression() != null

    override fun visitAdditiveExpression(ctx: QCParser.AdditiveExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.additiveExpression().accept(this).single()
            val right = ctx.multiplicativeExpression().accept(this).single()
            val op = when (ctx.op.getType()) {
                QCParser.Plus -> BinaryExpression.Add(left as Expression, right as Expression)
                QCParser.Minus -> BinaryExpression.Sub(left as Expression, right as Expression)
                else -> null
            }
            return if (op != null) listOf(op) else emptyList()
        }
        return super.visitAdditiveExpression(ctx)
    }

    val QCParser.MultiplicativeExpressionContext.terminal: Boolean get() = multiplicativeExpression() != null

    override fun visitMultiplicativeExpression(ctx: QCParser.MultiplicativeExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.multiplicativeExpression().accept(this).single()
            val right = ctx.castExpression().accept(this).single()
            // TODO
            return listOf(BinaryExpression.Mul(left as Expression, right as Expression))
        }
        return super.visitMultiplicativeExpression(ctx)
    }

    val QCParser.CastExpressionContext.terminal: Boolean get() = castExpression() != null

    override fun visitCastExpression(ctx: QCParser.CastExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.castExpression().accept(this).single()
            // TODO
            //            val right = ctx.typeName().accept(this).single()
            //            return BinaryExpression.Cast(left as Expression, right as Expression)
            return listOf(left)
        }
        return super.visitCastExpression(ctx)
    }

    val QCParser.UnaryExpressionContext.terminal: Boolean get() = unaryExpression() != null

    override fun visitUnaryExpression(ctx: QCParser.UnaryExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val right = ctx.unaryExpression().accept(this).single()
            val expr = right as Expression
            val expand = when (ctx.op.getType()) {
                QCParser.PlusPlus -> {
                    val ref = expr as ReferenceExpression
                    BinaryExpression.Assign(ref, BinaryExpression.Add(ref, ConstantExpression(1f)))
                }
                QCParser.MinusMinus -> {
                    val ref = expr as ReferenceExpression
                    BinaryExpression.Assign(ref, BinaryExpression.Sub(ref, ConstantExpression(1f)))
                }
                QCParser.Minus -> BinaryExpression.Sub(ConstantExpression(0), expr)
                else -> right
            }
            return listOf(expand)
        }
        val typeName = ctx.typeName()
        if (typeName != null) {
            return listOf(ReferenceExpression("TODO: sizeof(${typeName.getText()})"))
        }
        return super.visitUnaryExpression(ctx)
    }

    override fun visitPostfixPrimary(ctx: QCParser.PostfixPrimaryContext): List<Statement> {
        val left = ctx.primaryExpression().accept(this).single()
        return listOf(left)
    }

    override fun visitPostfixCall(ctx: QCParser.PostfixCallContext): List<Statement> {
        val left = ctx.postfixExpression().accept(this).single()

        val right = ctx.argumentExpressionList()
                ?.assignmentExpression()
                ?.flatMap { it.accept(this) }
                ?.filterNotNull()
        return listOf(FunctionCall(left as Expression, right ?: emptyList()))
    }

    override fun visitPrimaryExpression(ctx: QCParser.PrimaryExpressionContext): List<Statement> {
        val text = ctx.getText()
        if (ctx.Identifier() != null) {
            return listOf(ReferenceExpression(text))
        }
        if (ctx.StringLiteral().isNotEmpty()) {
            val concat = ctx.StringLiteral()
                    .fold("", {(ret: String, string: TerminalNode) ->
                        val s = string.getText()
                        ret + s.substring(1, s.length() - 1)
                    })
            return listOf(ConstantExpression(concat))
        }
        if (text.startsWith('#')) {
            return listOf(ConstantExpression(text))
        }
        if (ctx.Constant() != null) {
            val constant = ctx.Constant()
            val f = constant.getText().toFloat()
            return listOf(ConstantExpression(f))
        }
        return listOf(ConstantExpression("FIXME_${text}"))
    }
}