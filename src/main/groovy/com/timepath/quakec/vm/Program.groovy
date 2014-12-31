package com.timepath.quakec.vm

import groovy.transform.CompileStatic
import org.antlr.v4.runtime.misc.Utils

@CompileStatic
class Program {

    private Loader data

    Program(Loader data) {
        this.data = data
    }

    void exec(String needle = 'main') {
        exec data.functions.find {
            it.name == needle
        }
    }

    void exec(Function start) {
        LinkedList<Function> stack = []
        LinkedList<Integer> fpstack = []
        int fp = -1
        def push = { Function fn ->
            stack << fn
            fpstack << fp
            fp = fn.firstStatement
        }

        push start

        while (stack) {
            Statement stmt = data.statements[fp]
            println stmt
            def ret = stmt(data)
            if (ret == 0) { // DONE, RETURN
                stack.pop()
                fp = fpstack.pop()
                continue
            }
            fp += ret
            switch (stmt.op) {
                case Instruction.CALL0:
                case Instruction.CALL1:
                case Instruction.CALL2:
                case Instruction.CALL3:
                case Instruction.CALL4:
                case Instruction.CALL5:
                case Instruction.CALL6:
                case Instruction.CALL7:
                case Instruction.CALL8:
                    def function = data.functions[data.globalIntData.get(stmt.a)]
                    def i = function.firstStatement
                    if (i < 0) {
                        builtin(-i)
                    } else {
                        push function
                    }
                    break
            }
        }
    }

    Map<Integer, Closure<Float>> builtins = [(1): { String arg0 ->
        println "print(\"${Utils.escapeWhitespace(arg0, false)}\");"
        1
    }]

    def builtin(int id) {
        println "Bultin #${id}"
        def builtin = builtins.get(id)
        def offset = Instruction.OFS_PARM0
        def getFloat = { int i -> data.globalFloatData.get(i) }
        def getString = { int i -> data.strings[data.globalIntData.get(i)] }
        def args = builtin.parameterTypes.collect {
            def ret = it == float ? getFloat(-3 + (offset += 3))
                    : it == String ? getString(-3 + (offset += 3))
                    : null
            ret
        }
        builtin.call(args)
    }


    public static void main(String[] args) {
        def data = "${System.properties["user.home"]}/IdeaProjects/xonotic/gmqcc"
        new Program(new Loader("${data}/progs.dat" as File)).exec()
    }

}