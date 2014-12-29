package com.timepath.quakec

import groovy.transform.CompileStatic
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Utils
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.antlr.v4.runtime.tree.TerminalNode
import org.antlr.v4.runtime.tree.Trees

@CompileStatic
class TreePrinterListener implements ParseTreeListener {
    private final List<String> ruleNames
    private final StringBuilder builder = new StringBuilder()
    private int level

    public TreePrinterListener(Parser parser) {
        this.ruleNames = Arrays.asList(parser.ruleNames)
    }

    public TreePrinterListener(List<String> ruleNames) {
        this.ruleNames = ruleNames
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        if (builder.length() > 0) {
            builder.append(' ')
        }

        builder.append('"' + Utils.escapeWhitespace(Trees.getNodeText(node, ruleNames), false).replace('"', '\\"') + '"')
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        visitTerminal(node)
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        if (builder.length() > 0) {
            builder.append(' ')
        }

        if (ctx.childCount > 0) {
            if (level++)
                builder.append('\n' + ('  ' * level))
            builder.append('(')
        }

        int ruleIndex = ctx.ruleIndex
        String ruleName
        if (ruleIndex >= 0 && ruleIndex < ruleNames.size()) {
            ruleName = ruleNames.get(ruleIndex)
        } else {
            ruleName = Integer.toString(ruleIndex)
        }

        builder.append(ruleName)
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        if (ctx.childCount > 0) {
            builder.append(')')
            builder.append('\n' + ('  ' * --level))
        }
    }

    @Override
    public String toString() {
        return builder.toString()
    }

}