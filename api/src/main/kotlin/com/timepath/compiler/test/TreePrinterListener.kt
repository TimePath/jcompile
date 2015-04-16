package com.timepath.compiler.test

import com.timepath.quote
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.antlr.v4.runtime.tree.TerminalNode
import org.antlr.v4.runtime.tree.Trees

class TreePrinterListener(val ruleNames: List<String>) : ParseTreeListener {

    val sb = StringBuilder()
    var levelString = ""
    var level = 0
        set(value: Int) {
            val tab = "  "
            val diff = value// - $level
            if (diff > 0)
                levelString += tab.repeat(diff)
            else {
                val end = levelString.length() + tab.length() * diff
                levelString = levelString.substring(0, end)
            }
        }

    override fun visitTerminal(node: TerminalNode) {
        if (sb.length() > 0) {
            sb.append(" ")
        }

        sb.append(Trees.getNodeText(node, ruleNames).quote())
    }

    override fun visitErrorNode(node: ErrorNode) {
        visitTerminal(node)
    }

    override fun enterEveryRule(ctx: ParserRuleContext) {
        if (sb.length() > 0) {
            sb.append(" ")
        }

        if (ctx.getChildCount() > 0) {
            if (level++ > 0) sb.append("\n" + levelString)
            sb.append("(")
        }

        val ruleIndex = ctx.getRuleIndex()
        val ruleName = if (ruleIndex >= 0 && ruleIndex < ruleNames.size()) {
            ruleNames[ruleIndex]
        } else {
            Integer.toString(ruleIndex)
        }

        sb.append(ruleName)
    }

    override fun exitEveryRule(ctx: ParserRuleContext) {
        if (ctx.getChildCount() > 0) {
            sb.append(")")
            level--
            sb.append("\n" + levelString)
        }
    }

    override fun toString() = sb.toString()

}
