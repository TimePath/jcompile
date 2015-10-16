package com.timepath.compiler.frontend.quakec

import com.timepath.compiler.frontend.quakec.Grammar.Rule
import com.timepath.compiler.frontend.quakec.Grammar.Template
import kotlin.platform.platformStatic

object QC : Grammar() {

    val experimental = false
    val compat = true

    platformStatic fun main(args: Array<String>) {
        StringBuilder {
            appendln("grammar NewQC;")
            QC.rules.forEach { appendln(it) }
        }.let {
            println(it)
        }
    }

    val SEMI = ";".tok()
    val COMMA = ",".tok()

    val PARENS = Pair("(".tok(), ")".tok())
    val BRACKETS = Pair("[".tok(), "]".tok())
    val BRACES = Pair("{".tok(), "}".tok())

    val compilationUnit by rule { scopeGlobal.manyoptional() + EOF }
    val id by rule { Identifier / "var".tok() / "inline".tok() / "break".tok() }
    val attribType by rule { "const".tok() }
    val attribFunc by rule {
        val groupAttrib = "attribute"
        val groupKW = "kw"
        var ret = any()
        ret /= expr.capture(groupAttrib).wrap(BRACKETS).wrap(BRACKETS)
        ret /= "var".tok().capture(groupKW)
        ret /= "inline".tok().capture(groupKW)
        ret
    }
    val attribVar by rule {
        val groupAttrib = "attribute"
        val groupKW = "kw"
        var ret = any()
        ret /= expr.capture(groupAttrib).wrap(BRACKETS).wrap(BRACKETS)
        ret /= "typedef".tok().capture(groupKW)
        ret /= "static".tok().capture(groupKW)
        ret /= "noref".tok().capture(groupKW)
        ret /= "var".tok().capture(groupKW)
        ret /= "local".tok().capture(groupKW)
        ret
    }
    val block: Rule by rule { scopeBlock.manyoptional().wrap(BRACES) }
    val scopeBlock by rule {
        var ret = any()
        ret /= SEMI
        ret /= declVar
        ret /= stmt()
        ret
    }
    val scopeGlobal by rule {
        var ret = any()
        ret /= SEMI
        ret /= declEntity
        ret /= declEnum
        ret /= declFunc
        ret /= declVar
        ret
    }
    val declFunc by rule {
        var ret = any()
        val func = attribFunc.manyoptional() + type + id + functypeParams + ("=".tok().optional() + block).optional()
        ret /= func
        val builtin = attribFunc.manyoptional() + type + id + functypeParams + "=".tok() + exprAssign
        ret /= builtin
        val legacy = attribFunc.manyoptional() + type + id + "=".tok().optional() + block
        ret /= legacy
        ret
    }
    val declVar by rule { declVar_ + SEMI }
    val declVar_ by rule { attribVar.manyoptional() + type + declVar__.join(COMMA) }
    val declVar__ by rule {
        val initVar = ("=".tok() + exprAssign)
        val initArr = seq("=".tok(), (exprAssign.join(COMMA) + COMMA.optional()).wrap(BRACES))
        id + (initVar.optional() / (exprAssign.wrap(BRACKETS) + initArr.optional()))
    }
    val declEntity by rule {
        val parent = (":".tok() + id).optional()
        val body = scopeEntity.manyoptional().wrap(BRACES)
        "entityclass".tok() + id + parent + body
    }
    val scopeEntity by rule { SEMI / declVar / declFunc }
    val declEnum by rule {
        val type = ":".tok() + id
        val body = declEnum_.join(COMMA).wrap(BRACES)
        "enum".tok() + type.optional() + body
    }
    val declEnum_ by rule {
        val init = "=".tok() + exprAssign
        id + init.optional()
    }
    val functypeParams by rule { functypeParam.join(COMMA).optional().wrap(PARENS) }
    val functypeParam by rule { (type + id.optional()) / "...".tok() }
    val type by rule { attribType.manyoptional() + type_ }
    val type_: Rule by rule { typeName / (type_ + functypeParams) / (typePtr + type_) }
    val typePtr by rule { ".".tok() / "...".tok() }
    val typeName by rule {
        var ret = any()
        ret /= Identifier
        ret /= "auto".tok() / "var".tok()
        ret /= "void".tok()
        ret /= "bool".tok()
        ret /= "char".tok()
        ret /= "short".tok()
        ret /= "int".tok()
        ret /= "float".tok()
        ret /= "vector".tok()
        ret /= "string".tok()
        ret /= "entity".tok()
        ret
    }
    val expr: Rule by rule { binop(exprAssign, ",") }
    val exprAssign by rule {
        // TODO: pointer assign
        var op = any()
        op /= "=".tok()
        op /= "|=".tok()
        op /= "^=".tok()
        op /= "&=".tok()
        if (compat) op /= "&~=".tok() // same as `&=~`
        op /= "<<=".tok()
        op /= ">>=".tok()
        op /= ">>>=".tok()
        op /= "+=".tok()
        op /= "-=".tok()
        op /= "*=".tok()
        op /= "/=".tok()
        op /= "%=".tok()
        op /= "><=".tok()
        op /= "**=".tok()
        exprConst / (lvalue + op + expr)
    }
    val lvalue: Rule by rule {
        var ret = any()
        ret /= id
        ret /= "return".tok()
        ret /= lvalue + ".".tok() + lvalue
        ret /= lvalue + ".".tok() + lvalue.wrap(PARENS)
        ret /= lvalue + (exprAssign.join(COMMA)).optional().wrap(PARENS) + ".".tok() + lvalue
        ret /= lvalue + expr.wrap(BRACKETS) + (".".tok() + lvalue).optional()
        ret /= lvalue.wrap(PARENS)
        ret /= expr.wrap(PARENS)
        ret
    }
    val exprConst by rule { exprCond }
    val exprCond: Rule by rule {
        var ret = any()
        ret /= exprLogicalOr
        ret /= (exprCond + "?".tok() + expr + ":".tok() + expr)
        ret /= "if".tok() + expr.wrap(PARENS) + expr + "else".tok() + expr
        ret /= "switch".tok() + expr.wrap(PARENS) + block
        ret
    }
    val exprLogicalOr by rule { binop(exprLogicalXor, "||") }
    val exprLogicalXor by rule { binop(exprLogicalAnd, "^^") }
    val exprLogicalAnd by rule { binop(exprInclusiveOr, "&&") }
    val exprInclusiveOr by rule { binop(exprExclusiveOr, "|") }
    val exprExclusiveOr by rule { binop(exprAnd, "^") }
    val exprAnd by rule { binop(exprEqual, "&") }
    val exprEqual by rule { binop(exprCompare, "==", "!=") }
    val exprCompare by rule { binop(exprShift, "<", "<=", "<=>", ">=", ">") }
    val exprShift by rule { binop(exprAdd, "<<", ">>", ">>>") }
    val exprAdd by rule { binop(exprMul, "+", "-") }
    val exprMul by rule { binop(exprExp, "*", "/", "%", "><") }
    val exprExp by rule { binop(exprUnary, "**", left = false) }
    val exprUnary: Rule by rule {
        var ret = any()
        ret /= ("++".tok() / "--".tok()) + exprUnary
        ret /= ("+".tok() / "-".tok()) + exprUnary
        ret /= exprPostfix
        ret /= ("!".tok() / "~".tok()) + exprUnary
        ret /= type.wrap(PARENS) + exprUnary
        if (experimental) {
            ret /= ("&".tok() / "*".tok()) + exprUnary
        }
        ret /= "sizeof".tok() + exprUnary
        ret
    }
    val exprPostfix: Rule by rule { exprPrimary / (exprPostfix + ("++".tok() / "--".tok())) }
    val exprPrimary: Rule by rule {
        var ret = any()
        ret /= literal
        val parenExpr = expr.wrap(PARENS)
        ret /= parenExpr
        if (experimental) {
            ret /= declVar_
        }
        val va_arg = "...".tok() + (exprAssign + COMMA + type).wrap(PARENS)
        ret /= va_arg
        val field = exprPrimary + ".".tok() + id
        ret /= field
        val array = exprPrimary + expr.wrap(BRACKETS)
        ret /= array
        val member = exprPrimary + ((".*".tok() + expr) / (".".tok() + parenExpr))
        ret /= member
        var call = exprPrimary + (exprAssign.join(COMMA).optional()).wrap(PARENS)
        // fancy initialisation
        if (experimental) call += (id + ":".tok() + exprAssign).join(COMMA).wrap(BRACES).optional()
        ret /= call
        ret
    }
    val literal by rule {
        var ret = any()
        ret /= id
        ret /= Number
        ret /= Character
        ret /= Vector
        ret /= String
        val vector = ("[".tok() + exprAssign.join(COMMA) + "]".tok())
        ret /= vector
        val blockExpr = block.wrap(PARENS)
        ret /= blockExpr
        if (experimental) {
            val lambda = "inline".tok() + type + functypeParams + block
            ret /= lambda
        }
        ret
    }
    val stmt: Template by template("noif") { args ->
        val noif = "noif" in args
        val it = if (noif) stmt("noif") else stmt()
        var ret = any()
        ret /= block
        ret /= SEMI
        ret /= exprAssign + SEMI
        ret /= "switch".tok() + expr.wrap(PARENS) + block
        ret /= "do".tok() + stmt() + "while".tok() + expr.wrap(PARENS) + SEMI
        ret /= "goto".tok() + id + SEMI
        ret /= "break".tok() + id.optional() + SEMI
        ret /= "continue".tok() + id.optional() + SEMI
        ret /= "return".tok() + expr.optional() + SEMI
        if (experimental) ret /= "using".tok() + expr.wrap(PARENS) + stmt()
        ret /= ((id + ":".tok()) / (":".tok() + id)) + it.optional()
        ret /= "case".tok() + expr + ":".tok() + it
        ret /= "default".tok() + ":".tok() + it
        if (!noif)
            ret /= "if".tok() + expr.wrap(PARENS) + it
        ret /= "if".tok() + expr.wrap(PARENS) + stmt("noif") + "else".tok() + it
        ret /= "while".tok() + expr.wrap(PARENS) + it
        ret /= "for".tok() + ((SEMI / declVar / (expr + SEMI)) + expr.optional() + SEMI + expr.optional()).wrap(PARENS) + it
        ret
    }
    val Identifier by rule { IdentifierFragment + (NumberFragment / IdentifierFragment).manyoptional() }
    val IdentifierFragment by rule(fragment = true) {
        var ret = any()
        ret /= "a".tok() to "z".tok()
        ret /= "A".tok() to "Z".tok()
        ret /= "_".tok()
        ret /= "::".tok()
        ret
    }
    val Number by rule {
        var ret = any()
        ret /= "#".tok().optional() + (NumberFragment.manyoptional() + ".".tok()).optional() + NumberFragment.many() + "f".tok().optional()
        ret /= "0".tok() + "x".tok() + HexDigit.many()
        ret
    }
    val NumberFragment by rule(fragment = true) { "0".tok() to "9".tok() }
    val HexDigit by rule(fragment = true) { NumberFragment / ("a".tok() to "f".tok()) / ("A".tok() to "F".tok()) }
    val String by rule { ((Whitespace / Newline).manyoptional() + "\"".tok() + StringFragment.manyoptional() + "\"".tok()).many() }
    val StringFragment by rule(fragment = true) { raw("~[\"\\\\\\r\\n]") / EscapeSequence }
    val Character by rule { "'".tok() + CharacterFragment + "'".tok() }
    val CharacterFragment by rule(fragment = true) { raw("~['\\\\\\r\\n]") / EscapeSequence }
    val Vector by rule {
        val ws = Whitespace.manyoptional()
        val n = SignedNumber
        "'".tok() + ws.join(n) + "'".tok()
    }
    val SignedNumber by rule(fragment = true) { ("+".tok() / "-".tok()).optional() + Number }
    val EscapeSequence by rule(fragment = true) {
        var ret = any()
        ret /= "\\\\".tok() + raw("['\"?abfnrtv\\\\]")
        ret /= "\\\\x".tok() + HexDigit
        ret /= "\\\\{".tok() + "x".tok().optional() + Number + "}".tok()
        ret
    }
    val Whitespace by rule { raw("[ \\t]").many() + raw("-> channel(1)") }
    val Newline by rule { ("\\n".tok() / ("\\r".tok() + "\\n".tok().optional())) + raw("-> channel(1)") }
    val LineDirective by rule {
        val ws = Whitespace
        ("#".tok() / "#line".tok()) + ws.manyoptional() + Number + ws.many() + String + ws.manyoptional() + raw("~[\\r\\n]").manyoptional() + raw("-> channel(2)")
    }
    val PragmaDirective by rule {
        val ws = Whitespace
        "#".tok() + ws.manyoptional() + "pragma".tok() + ws.manyoptional() + raw("~[\\r\\n]").manyoptional() + raw("-> channel(2)")
    }
    val CommentLine by rule { "//".tok() + raw("~[\\r\\n]").manyoptional() + raw("-> channel(3)") }
    val CommentDoc by rule { "/**".tok() + Any.manyoptional().optional() + "*/".tok() + raw("-> channel(3)") }
    val CommentBlock by rule { "/*".tok() + Any.manyoptional().optional() + "*/".tok() + raw("-> channel(3)") }
}
