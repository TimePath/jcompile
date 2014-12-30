package com.timepath.quakec.vm

import groovy.transform.CompileStatic

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
                    def i = data.globalData.getInt((int) stmt.a)
                    def fn = data.functions[i]
                    push fn
                    break
            }
        }
    }

    public static void main(String[] args) {
        def data = "${System.properties["user.home"]}/IdeaProjects/xonotic/gmqcc"
        new Program(new Loader("${data}/progs.dat" as File)).exec()
    }

}