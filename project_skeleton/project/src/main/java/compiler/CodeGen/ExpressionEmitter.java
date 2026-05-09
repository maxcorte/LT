package compiler.CodeGen;

import compiler.Parser.*;
import org.objectweb.asm.Opcodes;

import java.util.List;

final class ExpressionEmitter {

    private final CodeGenerator cg;

    ExpressionEmitter(CodeGenerator cg) { this.cg = cg; }

    // Literals

    void emitIntConstant(int v) {
        if (v >= -1 && v <= 5) {
            cg.mv.visitInsn(Opcodes.ICONST_0 + v);
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            cg.mv.visitIntInsn(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            cg.mv.visitIntInsn(Opcodes.SIPUSH, v);
        } else {
            cg.mv.visitLdcInsn(Integer.valueOf(v));
        }
    }

    void loadLocal(String type, int slot) {
        if (type.endsWith("[]"))                                  cg.mv.visitVarInsn(Opcodes.ALOAD, slot);
        else if (TypeUtils.isInt(type) || TypeUtils.isBool(type)) cg.mv.visitVarInsn(Opcodes.ILOAD, slot);
        else if (TypeUtils.isFloat(type))                          cg.mv.visitVarInsn(Opcodes.FLOAD, slot);
        else                                                       cg.mv.visitVarInsn(Opcodes.ALOAD, slot);
    }

    void storeLocal(String type, int slot) {
        if (type.endsWith("[]"))                                  cg.mv.visitVarInsn(Opcodes.ASTORE, slot);
        else if (TypeUtils.isInt(type) || TypeUtils.isBool(type)) cg.mv.visitVarInsn(Opcodes.ISTORE, slot);
        else if (TypeUtils.isFloat(type))                          cg.mv.visitVarInsn(Opcodes.FSTORE, slot);
        else                                                       cg.mv.visitVarInsn(Opcodes.ASTORE, slot);
    }

    void emitDefaultValue(String type) {
        if (type.endsWith("[]"))                                  cg.mv.visitInsn(Opcodes.ACONST_NULL);
        else if (TypeUtils.isInt(type) || TypeUtils.isBool(type)) cg.mv.visitInsn(Opcodes.ICONST_0);
        else if (TypeUtils.isFloat(type))                          cg.mv.visitInsn(Opcodes.FCONST_0);
        else                                                       cg.mv.visitInsn(Opcodes.ACONST_NULL);
    }

    void emitVarRef(VarRefNode node) {
        Signatures.VarInfo info = cg.scope.lookup(node.name);
        if (info != null) {
            loadLocal(info.type, info.slot);
            return;
        }
        Signatures.GlobalInfo g = cg.globals.get(node.name);
        if (g != null) {
            cg.mv.visitFieldInsn(Opcodes.GETSTATIC, cg.mainClassName, node.name,
                    TypeUtils.descForFullType(g.type));
            return;
        }
        throw new RuntimeException(
                "CodeGen internal: variable '" + node.name + "' not found");
    }

    //  Field and array access (read)

    void emitFieldRead(FieldAccessNode node) {
        node.base.accept(cg);
        String baseType = node.base.inferredType;
        if (baseType.endsWith("[]")) {
            baseType = baseType.substring(0, baseType.length() - 2);
        }
        Signatures.CollSig sig = cg.collections.get(baseType);
        if (sig == null) {
            throw new RuntimeException(
                    "CodeGen internal: '" + baseType + "' is not a collection");
        }
        int idx = sig.fieldNames.indexOf(node.field);
        if (idx < 0) {
            throw new RuntimeException(
                    "CodeGen internal: field '" + node.field + "' not in " + baseType);
        }
        String fieldFullType = sig.fieldTypes.get(idx);
        boolean fieldIsArr = fieldFullType.endsWith("[]");
        String fieldBase = fieldIsArr
                ? fieldFullType.substring(0, fieldFullType.length() - 2)
                : fieldFullType;
        String desc = TypeUtils.typeDescriptor(fieldBase, fieldIsArr);
        cg.mv.visitFieldInsn(Opcodes.GETFIELD, baseType, node.field, desc);
    }

    void emitArrayRead(ArrayAccessNode node) {
        node.base.accept(cg);
        node.index.accept(cg);
        String baseType = node.base.inferredType;
        if (!baseType.endsWith("[]")) {
            throw new RuntimeException(
                    "CodeGen internal: array access on non-array " + baseType);
        }
        String elem = baseType.substring(0, baseType.length() - 2);
        if (TypeUtils.isInt(elem))           cg.mv.visitInsn(Opcodes.IALOAD);
        else if (TypeUtils.isBool(elem))     cg.mv.visitInsn(Opcodes.BALOAD);
        else if (TypeUtils.isFloat(elem))    cg.mv.visitInsn(Opcodes.FALOAD);
        else                                 cg.mv.visitInsn(Opcodes.AALOAD);
    }

    void emitNewArray(NewArrayNode node) {
        node.size.accept(cg);
        String elem = node.elementType;
        if (TypeUtils.isInt(elem))           cg.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
        else if (TypeUtils.isFloat(elem))    cg.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
        else if (TypeUtils.isBool(elem))     cg.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
        else if (TypeUtils.isString(elem))   cg.mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        else                                 cg.mv.visitTypeInsn(Opcodes.ANEWARRAY, elem);
    }

    // Unary

    void emitUnary(UnaryOpNode node) {
        if ("-".equals(node.op)) {
            node.expr.accept(cg);
            if (TypeUtils.isFloat(node.expr.inferredType)) cg.mv.visitInsn(Opcodes.FNEG);
            else                                            cg.mv.visitInsn(Opcodes.INEG);
            return;
        }
        if ("not".equals(node.op)) {
            // not x  ==  x XOR 1   (BOOL is 0 or 1 on the stack)
            node.expr.accept(cg);
            cg.mv.visitInsn(Opcodes.ICONST_1);
            cg.mv.visitInsn(Opcodes.IXOR);
            return;
        }
        throw new UnsupportedOperationException("TODO: unary operator '" + node.op + "'");
    }

    // Assignment (var, field, array)

    void emitAssignment(BinaryOpNode node) {
        if (node.left instanceof VarRefNode) {
            String varName = ((VarRefNode) node.left).name;

            Signatures.VarInfo info = cg.scope.lookup(varName);
            if (info != null) {
                node.right.accept(cg);
                String rightType = node.right.inferredType;
                boolean isArr = info.type.endsWith("[]");
                String baseLeft = isArr
                        ? info.type.substring(0, info.type.length() - 2) : info.type;
                if (TypeUtils.isFloat(baseLeft) && TypeUtils.isInt(rightType) && !isArr) {
                    cg.mv.visitInsn(Opcodes.I2F);
                }
                storeLocal(info.type, info.slot);
                return;
            }
            Signatures.GlobalInfo g = cg.globals.get(varName);
            if (g != null) {
                node.right.accept(cg);
                String rightType = node.right.inferredType;
                boolean isArr = g.type.endsWith("[]");
                String baseLeft = isArr
                        ? g.type.substring(0, g.type.length() - 2) : g.type;
                if (TypeUtils.isFloat(baseLeft) && TypeUtils.isInt(rightType) && !isArr) {
                    cg.mv.visitInsn(Opcodes.I2F);
                }
                cg.mv.visitFieldInsn(Opcodes.PUTSTATIC, cg.mainClassName, varName,
                        TypeUtils.descForFullType(g.type));
                return;
            }
            throw new RuntimeException(
                    "CodeGen internal: variable '" + varName + "' not found");
        }

        if (node.left instanceof FieldAccessNode) {
            FieldAccessNode fa = (FieldAccessNode) node.left;
            fa.base.accept(cg);
            node.right.accept(cg);

            String objType = fa.base.inferredType;
            if (objType.endsWith("[]")) {
                objType = objType.substring(0, objType.length() - 2);
            }
            Signatures.CollSig sig = cg.collections.get(objType);
            int idx = sig.fieldNames.indexOf(fa.field);
            String fieldFullType = sig.fieldTypes.get(idx);
            boolean fieldIsArr = fieldFullType.endsWith("[]");
            String fieldBase = fieldIsArr
                    ? fieldFullType.substring(0, fieldFullType.length() - 2)
                    : fieldFullType;

            if (TypeUtils.isFloat(fieldBase) && TypeUtils.isInt(node.right.inferredType) && !fieldIsArr) {
                cg.mv.visitInsn(Opcodes.I2F);
            }

            String desc = TypeUtils.typeDescriptor(fieldBase, fieldIsArr);
            cg.mv.visitFieldInsn(Opcodes.PUTFIELD, objType, fa.field, desc);
            return;
        }

        if (node.left instanceof ArrayAccessNode) {
            ArrayAccessNode aa = (ArrayAccessNode) node.left;
            aa.base.accept(cg);
            aa.index.accept(cg);
            node.right.accept(cg);

            String baseType = aa.base.inferredType;
            String elem = baseType.substring(0, baseType.length() - 2);

            if (TypeUtils.isFloat(elem) && TypeUtils.isInt(node.right.inferredType)) {
                cg.mv.visitInsn(Opcodes.I2F);
            }

            if (TypeUtils.isInt(elem))         cg.mv.visitInsn(Opcodes.IASTORE);
            else if (TypeUtils.isBool(elem))   cg.mv.visitInsn(Opcodes.BASTORE);
            else if (TypeUtils.isFloat(elem))  cg.mv.visitInsn(Opcodes.FASTORE);
            else                               cg.mv.visitInsn(Opcodes.AASTORE);
            return;
        }

        throw new UnsupportedOperationException(
                "TODO: assignment to " + node.left.getClass().getSimpleName());
    }

    // Arithmetic and string concat

    void emitArithmetic(BinaryOpNode node, String lt, String rt) {
        boolean resultIsFloat = TypeUtils.isFloat(lt) || TypeUtils.isFloat(rt);

        node.left.accept(cg);
        if (resultIsFloat && TypeUtils.isInt(lt)) cg.mv.visitInsn(Opcodes.I2F);

        node.right.accept(cg);
        if (resultIsFloat && TypeUtils.isInt(rt)) cg.mv.visitInsn(Opcodes.I2F);

        if (resultIsFloat) {
            switch (node.op) {
                case "+": cg.mv.visitInsn(Opcodes.FADD); break;
                case "-": cg.mv.visitInsn(Opcodes.FSUB); break;
                case "*": cg.mv.visitInsn(Opcodes.FMUL); break;
                case "/": cg.mv.visitInsn(Opcodes.FDIV); break;
                case "%": cg.mv.visitInsn(Opcodes.FREM); break;
                default: throw new RuntimeException("unreachable");
            }
        } else {
            switch (node.op) {
                case "+": cg.mv.visitInsn(Opcodes.IADD); break;
                case "-": cg.mv.visitInsn(Opcodes.ISUB); break;
                case "*": cg.mv.visitInsn(Opcodes.IMUL); break;
                case "/": cg.mv.visitInsn(Opcodes.IDIV); break;
                case "%": cg.mv.visitInsn(Opcodes.IREM); break;
                default: throw new RuntimeException("unreachable");
            }
        }
    }

    void emitStringConcat(BinaryOpNode node) {
        node.left.accept(cg);
        node.right.accept(cg);
        cg.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/lang/String", "concat",
                "(Ljava/lang/String;)Ljava/lang/String;", false);
    }

    // Calls

    void emitCollectionConstructor(Signatures.CollSig sig, List<ExprNode> args) {
        cg.mv.visitTypeInsn(Opcodes.NEW, sig.name);
        cg.mv.visitInsn(Opcodes.DUP);
        for (int i = 0; i < args.size(); i++) {
            ExprNode arg = args.get(i);
            arg.accept(cg);
            String expected = sig.fieldTypes.get(i);
            boolean expectedIsArr = expected.endsWith("[]");
            String expectedBase = expectedIsArr
                    ? expected.substring(0, expected.length() - 2) : expected;
            if (TypeUtils.isFloat(expectedBase) && TypeUtils.isInt(arg.inferredType) && !expectedIsArr) {
                cg.mv.visitInsn(Opcodes.I2F);
            }
        }
        cg.mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                sig.name, "<init>", sig.constructorDescriptor, false);
    }

    void emitUserCall(Signatures.FunSig sig, List<ExprNode> args) {
        for (int i = 0; i < args.size(); i++) {
            ExprNode arg = args.get(i);
            arg.accept(cg);
            String expected = sig.paramTypes.get(i);
            boolean expectedIsArr = expected.endsWith("[]");
            String expectedBase = expectedIsArr
                    ? expected.substring(0, expected.length() - 2) : expected;
            if (TypeUtils.isFloat(expectedBase) && TypeUtils.isInt(arg.inferredType) && !expectedIsArr) {
                cg.mv.visitInsn(Opcodes.I2F);
            }
        }
        cg.mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                cg.mainClassName, sig.name, sig.descriptor, false);
    }

    void emitPrintBuiltin(String builtinName, ExprNode arg) {
        boolean ln = builtinName.equals("println");
        String javaMethod = ln ? "println" : "print";

        cg.mv.visitFieldInsn(Opcodes.GETSTATIC,
                "java/lang/System", "out", "Ljava/io/PrintStream;");

        arg.accept(cg);

        String descriptor;
        if (builtinName.equals("print_INT")) {
            descriptor = "(I)V";
        } else if (builtinName.equals("print_FLOAT")) {
            descriptor = "(F)V";
        } else {
            String t = arg.inferredType;
            if      (TypeUtils.isInt(t))    descriptor = "(I)V";
            else if (TypeUtils.isFloat(t))  descriptor = "(F)V";
            else if (TypeUtils.isBool(t))   descriptor = "(Z)V";
            else if (TypeUtils.isString(t)) descriptor = "(Ljava/lang/String;)V";
            else                            descriptor = "(Ljava/lang/String;)V";
        }

        cg.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream", javaMethod, descriptor, false);
    }

    void emitReadBuiltin(String name) {
        cg.mv.visitFieldInsn(Opcodes.GETSTATIC,
                cg.mainClassName, CodeGenerator.SCANNER_FIELD, CodeGenerator.SCANNER_DESC);
        if ("read_INT".equals(name)) {
            cg.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    "java/util/Scanner", "nextInt", "()I", false);
        } else if ("read_FLOAT".equals(name)) {
            cg.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    "java/util/Scanner", "nextFloat", "()F", false);
        } else if ("read_STRING".equals(name)) {
            cg.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    "java/util/Scanner", "next", "()Ljava/lang/String;", false);
        } else {
            throw new RuntimeException("unknown read builtin: " + name);
        }
    }
}
