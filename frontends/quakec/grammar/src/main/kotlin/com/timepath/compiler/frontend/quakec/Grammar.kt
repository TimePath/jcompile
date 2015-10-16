package com.timepath.compiler.frontend.quakec

import kotlin.platform.platformStatic
import kotlin.properties.Delegates

object Might : Grammar() {

    val test by rules(
            "a" to { "a".tok() },
            "b" to { "b".tok() }
    )

    platformStatic fun main(args: Array<String>) {
        StringBuilder {
            Might.rules.forEach { appendln(it) }
        }.let {
            println(it)
        }
    }
}

abstract class Grammar {
    fun rule(fragment: Boolean = false, s: (String) -> String = { it }, f: Rule.() -> Rule) = named<Rule> {
        Rule.Top(fragment, { s(name) }, f)
    }

    interface TopLevel {
        val name: () -> String
    }

    /**
     * Won't show up unless instantiated
     */
    class Template(vararg flags: String, override val name: () -> String, val template: Rule.(Set<String>) -> Rule) : TopLevel {
        val flags: Collection<String> = flags.toSet()

        val need: MutableMap<Set<String>, Boolean> = hashMapOf()

        fun combinations(): Sequence<Set<String>> {
            var bits = 0
            return sequence(setOf()) {
                bits++
                val list = flags.mapIndexed { i, s ->
                    when {
                        bits and (1 shl i) == 0 -> null
                        else -> s
                    }
                }.filterNotNull()
                when {
                    list.isEmpty() -> null
                    else -> list.toSet()
                }
            }
        }

        override fun toString() = combinations()
                .filter { it in need.keySet() }
                .map { this(it).toString() }.join("\n")

        fun invoke(vararg active: String) = invoke(active.toSet())

        fun invoke(active: Set<String>): Rule.Top {
            assert(flags.containsAll(active))
            need[active] = true
            return Rule.Top(false, {
                name() + when {
                    active.isEmpty() -> ""
                    else -> "_" + active.joinToString("_")
                }
            }) { template(active) }
        }
    }

    fun template(vararg flags: String, f: Rule.(Set<String>) -> Rule) = named<Template> {
        Template(*flags, name = { name }, template = f)
    }

    fun rules(vararg alts: Pair<String, () -> Rule>) = named<Template> {
        val m = mapOf(*alts)
        val tmpl = Template(*m.keySet().toTypedArray(), name = { name }) { args ->
            var ret = any()
            if (args.isEmpty()) {
                m.keySet().forEach {
                    ret /= this@named.it(it)
                }
            } else {
                assert(args.size() == 1)
                ret /= m[args.single()]!!()
            }
            ret
        }
        tmpl() // instantiate it
        tmpl
    }

    companion object {
        val sRuleBegin = ":"
        val sRuleEnd = ";"
        val sFragment = "fragment\n"
        fun sCapture(s: String) = s + "="
    }

    val rules: List<TopLevel>
        get() {
            val rules = NamedObject.get<TopLevel>(this)
            rules.toString() // force evaluation
            val firstLexer = rules.indexOfFirst {
                it.name()[0].isUpperCase()
            }
            val tokens = tokens.map {
                Rule.Top(false, { it.key }, { it.value })
            }
            val head = if (firstLexer >= 0) rules.subList(0, firstLexer) else rules
            val tail = if (firstLexer >= 0) rules.subList(firstLexer, rules.size()) else emptyList()
            return head + tokens + tail
        }

    val EOF = object : Rule {
        override fun referStr(parent: Rule?) = "EOF"
    }

    val Any = object : Rule {
        override fun referStr(parent: Rule?) = "."
    }

    private val tokens: MutableMap<String, Rule> = hashMapOf()

    fun raw(s: String) = Rule.Raw(s)
    fun range(begin: Rule, end: Rule) = begin + Rule.Raw("..") + end
    fun Rule.to(other: Rule) = range(this, other)
    fun str(s: String) = when {
        s.matches("[a-zA-Z]{2,}".toRegex()) -> {
            val rulename = "KW_" + s
            tokens.getOrPut(rulename) { Rule.Terminal(s) } let { raw(rulename) }
        }
        else -> Rule.Terminal(s)
    }

    fun String.tok() = str(this)
    fun seq(vararg rules: Rule) = Rule.And(*rules)
    fun any(vararg rules: Rule): Rule {
        rules.singleOrNull()?.let { return it }
        return Rule.Or(*rules)
    }

    fun Rule.many() = Rule.Many(this)
    fun Rule.optional() = Rule.Optional(this)
    fun Rule.manyoptional() = Rule.Many(this, true)

    fun Rule.capture(s: String) = Rule.Capture(s, this)

    fun Rule.join(on: Rule) = this + (on + this).manyoptional()
    fun Rule.wrap(left: Rule, right: Rule) = left + this + right
    fun Rule.wrap(pair: Pair<Rule, Rule>) = wrap(pair.first, pair.second)

    fun Rule.binop(next: Rule, vararg op: String, left: Boolean = true) = when {
        left -> next / (this + any(*op.map { it.tok() }.toTypedArray()) + next)
        else -> next / (next + any(*op.map { it.tok() }.toTypedArray()) + this)
    }

    interface Rule {

        fun String.wrapIf(cond: Boolean) = if (cond) "($this)" else this

        fun join(rules: List<Rule>, s: String): String {
            return rules.map { it.referStr(this) }.join(s)
        }

        fun Rule.isMulti(): Boolean {
            return this is Or || this is And
        }

        fun referStr(parent: Rule?): String

        class Top(val fragment: Boolean, override val name: () -> String, f: Rule.() -> Rule) : Rule, TopLevel {
            val delegate by Delegates.lazy { f() }
            override fun toString() = "${if (fragment) sFragment else ""}${this.referStr(null)}\n    ${sRuleBegin}    ${delegate.referStr(null)}\n    ${sRuleEnd}"
            override fun referStr(parent: Rule?) = name()
        }

        class Raw(val s: String) : Rule {
            override fun referStr(parent: Rule?) = s
        }

        class Terminal(val s: String) : Rule {
            override fun referStr(parent: Rule?) = "'${s.replace("'", "\\'")}'"
        }

        fun Rule.and(other: Rule) = Rule.And(this, other)
        fun plus(other: Rule) = Rule.And(this, other)
        class And(val rules: List<Rule>) : Rule {
            override fun referStr(parent: Rule?): String {
                return join(rules, " ").wrapIf(false)
            }

            constructor(vararg rules: Rule) : this(rules.flatMap {
                when (it) {
                    is And -> it.rules
                    else -> listOf(it)
                }
            })
        }

        fun Rule.or(other: Rule) = Rule.Or(this, other)
        fun div(other: Rule) = Rule.Or(this, other)
        class Or(val rules: List<Rule>) : Rule {
            override fun referStr(parent: Rule?): String {
                return join(rules, "\n    |    ").wrapIf(parent != null)
            }

            constructor(vararg rules: Rule) : this(rules.flatMap {
                when (it) {
                    is Or -> it.rules
                    else -> listOf(it)
                }
            })
        }

        class Many(val delegate: Rule, val optional: Boolean = false) : Rule {
            val op = if (optional) "*" else "+"
            override fun referStr(parent: Rule?): String {
                return delegate.referStr(this).wrapIf(delegate.isMulti() || delegate is Optional) + op
            }
        }

        class Optional(val delegate: Rule) : Rule {
            override fun referStr(parent: Rule?): String {
                return delegate.referStr(this).wrapIf(delegate.isMulti()) + "?"
            }
        }

        class Capture(val name: String, val delegate: Rule) : Rule {
            override fun referStr(parent: Rule?): String {
                return sCapture(name) + delegate.referStr(this).wrapIf(delegate.isMulti())
            }
        }
    }
}
