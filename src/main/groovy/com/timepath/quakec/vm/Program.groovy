package com.timepath.quakec.vm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
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

    @TupleConstructor
    static class Frame {
        int sp, stmt
        Function fn
    }

    void exec(Function start) {
        Stack<Frame> stack = []
        int sp = -1
        int stmt = -1
        Function fn
        def push = { Function it ->
            stack << new Frame(sp, stmt, fn)

            def k = it.firstLocal;
            for (int i = 0; i < it.numParams; i++) {
                for (int j = 0; j < it.sizeof[i]; j++) {
                    data.globalIntData.put(k++, data.globalIntData.get(Instruction.OFS_PARM0 + (3 * i) + j));
                }
            }

            sp = 0
            stmt = it.firstStatement
            fn = it
        }
        def pop = { ->
            Frame it = stack.pop()

            // TODO: copy locals back to prev

            sp = it.sp
            stmt = it.stmt
            fn = it.fn
        }

        push start

        while (stack) {
            def name = fn.name;
            Statement s = data.statements[stmt]
            println s
            def ret = s(data)
            if (ret == 0) { // DONE, RETURN
                pop()
                continue
            }
            stmt += ret
            switch (s.op) {
                case Instruction.CALL0:
                case Instruction.CALL1:
                case Instruction.CALL2:
                case Instruction.CALL3:
                case Instruction.CALL4:
                case Instruction.CALL5:
                case Instruction.CALL6:
                case Instruction.CALL7:
                case Instruction.CALL8:
                    def function = data.functions[data.globalIntData.get(s.a)]
                    def i = function.firstStatement
                    if (i < 0) {
                        builtin(-i, s.op.ordinal() - Instruction.CALL0.ordinal())
                    } else {
                        push function
                    }
                    break
            }
        }
    }

    class Builtin {
        private String name
        private List<Class> parameterTypes
        private Class varargsType
        private Closure callback

        Builtin(String name, List<Class> parameterTypes, Class varargsType, Closure callback) {
            this.name = name
            this.parameterTypes = parameterTypes
            this.varargsType = varargsType
            this.callback = callback
        }

        @CompileDynamic
        Object call(int parameterCount) {
            def offset = Instruction.OFS_PARM0
            def getFloat = { int i -> data.globalFloatData.get(i) }
            def getString = { int i -> data.strings[data.globalIntData.get(i)] }
            def read = { Class it ->
                switch (it) {
                    case Float:
                    case float:
                        return getFloat(-3 + (offset += 3))
                    case String:
                        return getString(-3 + (offset += 3))
                    default:
                        return null
                }
            }
            Object[] args = parameterTypes.collect { Class it -> read(it) }
            Object[] varargs = ((parameterTypes.size()..<parameterCount).collect { read(varargsType) })
            def objects = args + varargs
            println """$name(${objects.collect({
                it.class == String ? "\"${Utils.escapeWhitespace(it, false)}\"" : it
            }).join(', ')})"""
            callback(*objects)
        }

    }

    Map<Integer, Builtin> builtins = [
            (1): new Builtin('print', [], String, { String[] args ->
                args.join('')
            }),
            (2): new Builtin('ftos', [Float], null, { float arg ->
                arg as String
            })
    ]

    def builtin(int id, int parameterCount) {
        def builtin = builtins.get(id)
        def ret = builtin.call(parameterCount)
        if (!ret) return
        switch (ret.class) {
            case Float:
            case float:
                data.globalFloatData.put(Instruction.OFS_RETURN, ret as float)
                break
            case String:
                // TODO: make temp string
                break
            default:
                break
        }
    }

    public static void main(String[] args) {
        def data = "${System.properties["user.home"]}/IdeaProjects/xonotic/gmqcc"
        new Program(new Loader("${data}/progs.dat" as File)).exec()
    }

}