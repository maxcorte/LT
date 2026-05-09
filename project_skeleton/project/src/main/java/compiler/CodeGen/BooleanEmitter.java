package compiler.CodeGen;

import compiler.Parser.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

final class BooleanEmitter {

    private final CodeGenerator cg;

    BooleanEmitter(CodeGenerator cg) { this.cg = cg; }

    static boolean isComparison(String op) {
        return "<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op)
                || "==".equals(op) || "=/=".equals(op);
    }

    // Materialize the result of a boolean expression as 0/1 on the stack.
    void emitBoolValue(ExprNode expr) {
        Label falseLabel = new Label();
        Label endLabel   = new Label();
        emitJumpIfFalse(expr, falseLabel);
        cg.mv.visitInsn(Opcodes.ICONST_1);
        cg.mv.visitJumpInsn(Opcodes.GOTO, endLabel);
        cg.mv.visitLabel(falseLabel);
        cg.mv.visitInsn(Opcodes.ICONST_0);
        cg.mv.visitLabel(endLabel);
    }

    // Emit code that jumps to falseLabel if expr evaluates to false
    void emitJumpIfFalse(ExprNode expr, Label falseLabel) {
        if (expr instanceof UnaryOpNode) {
            UnaryOpNode un = (UnaryOpNode) expr;
            if ("not".equals(un.op)) {
                emitJumpIfTrue(un.expr, falseLabel);
                return;
            }
        }
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode bin = (BinaryOpNode) expr;
            String op = bin.op;
            if (isComparison(op)) {
                emitComparisonJump(bin, falseLabel, true);
                return;
            }
            if ("&&".equals(op)) {
                emitJumpIfFalse(bin.left, falseLabel);
                emitJumpIfFalse(bin.right, falseLabel);
                return;
            }
            if ("||".equals(op)) {
                Label trueLabel = new Label();
                emitJumpIfTrue(bin.left, trueLabel);
                emitJumpIfFalse(bin.right, falseLabel);
                cg.mv.visitLabel(trueLabel);
                return;
            }
        }
        if (expr instanceof BoolLiteralNode) {
            if (!((BoolLiteralNode) expr).value) {
                cg.mv.visitJumpInsn(Opcodes.GOTO, falseLabel);
            }
            return;
        }
        // Generic case: evaluate to 0/1, jump if 0
        expr.accept(cg);
        cg.mv.visitJumpInsn(Opcodes.IFEQ, falseLabel);
    }

    // Mirror of emitJumpIfFalse: jumps to trueLabel if expr is true.
    void emitJumpIfTrue(ExprNode expr, Label trueLabel) {
        if (expr instanceof UnaryOpNode) {
            UnaryOpNode un = (UnaryOpNode) expr;
            if ("not".equals(un.op)) {
                emitJumpIfFalse(un.expr, trueLabel);
                return;
            }
        }
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode bin = (BinaryOpNode) expr;
            String op = bin.op;
            if (isComparison(op)) {
                emitComparisonJump(bin, trueLabel, false);
                return;
            }
            if ("&&".equals(op)) {
                Label falseLabel = new Label();
                emitJumpIfFalse(bin.left, falseLabel);
                emitJumpIfTrue(bin.right, trueLabel);
                cg.mv.visitLabel(falseLabel);
                return;
            }
            if ("||".equals(op)) {
                emitJumpIfTrue(bin.left, trueLabel);
                emitJumpIfTrue(bin.right, trueLabel);
                return;
            }
        }
        if (expr instanceof BoolLiteralNode) {
            if (((BoolLiteralNode) expr).value) {
                cg.mv.visitJumpInsn(Opcodes.GOTO, trueLabel);
            }
            return;
        }
        expr.accept(cg);
        cg.mv.visitJumpInsn(Opcodes.IFNE, trueLabel);
    }

    private void emitComparisonJump(BinaryOpNode bin, Label label, boolean jumpWhenFalse) {
        String op = bin.op;
        String lt = bin.left.inferredType;
        String rt = bin.right.inferredType;

        // STRING == / =/=  : use String.equals
        if (TypeUtils.isString(lt) && TypeUtils.isString(rt)) {
            bin.left.accept(cg);
            bin.right.accept(cg);
            cg.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    "java/lang/String", "equals",
                    "(Ljava/lang/Object;)Z", false);
            boolean eqOp = "==".equals(op);
            boolean jumpWhenEqualsTrue = (eqOp != jumpWhenFalse);
            cg.mv.visitJumpInsn(jumpWhenEqualsTrue ? Opcodes.IFNE : Opcodes.IFEQ, label);
            return;
        }

        // Reference equality (collections, arrays)
        if (TypeUtils.isReference(lt) && TypeUtils.isReference(rt)) {
            bin.left.accept(cg);
            bin.right.accept(cg);
            boolean eqOp = "==".equals(op);
            boolean jumpWhenEqual = (eqOp != jumpWhenFalse);
            cg.mv.visitJumpInsn(jumpWhenEqual ? Opcodes.IF_ACMPEQ : Opcodes.IF_ACMPNE, label);
            return;
        }

        // Numeric (or BOOL == BOOL)
        boolean useFloat = TypeUtils.isFloat(lt) || TypeUtils.isFloat(rt);

        bin.left.accept(cg);
        if (useFloat && TypeUtils.isInt(lt)) cg.mv.visitInsn(Opcodes.I2F);
        bin.right.accept(cg);
        if (useFloat && TypeUtils.isInt(rt)) cg.mv.visitInsn(Opcodes.I2F);

        if (useFloat) {
            int cmpOp = (">".equals(op) || ">=".equals(op))
                    ? Opcodes.FCMPL : Opcodes.FCMPG;
            cg.mv.visitInsn(cmpOp);
            cg.mv.visitJumpInsn(floatJumpOp(op, jumpWhenFalse), label);
        } else {
            cg.mv.visitJumpInsn(intCmpJumpOp(op, jumpWhenFalse), label);
        }
    }

    private int intCmpJumpOp(String op, boolean jumpWhenFalse) {
        if (jumpWhenFalse) {
            switch (op) {
                case "<":   return Opcodes.IF_ICMPGE;
                case "<=":  return Opcodes.IF_ICMPGT;
                case ">":   return Opcodes.IF_ICMPLE;
                case ">=":  return Opcodes.IF_ICMPLT;
                case "==":  return Opcodes.IF_ICMPNE;
                case "=/=": return Opcodes.IF_ICMPEQ;
            }
        } else {
            switch (op) {
                case "<":   return Opcodes.IF_ICMPLT;
                case "<=":  return Opcodes.IF_ICMPLE;
                case ">":   return Opcodes.IF_ICMPGT;
                case ">=":  return Opcodes.IF_ICMPGE;
                case "==":  return Opcodes.IF_ICMPEQ;
                case "=/=": return Opcodes.IF_ICMPNE;
            }
        }
        throw new RuntimeException("unknown comparison op: " + op);
    }

    private int floatJumpOp(String op, boolean jumpWhenFalse) {
        if (jumpWhenFalse) {
            switch (op) {
                case "<":   return Opcodes.IFGE;
                case "<=":  return Opcodes.IFGT;
                case ">":   return Opcodes.IFLE;
                case ">=":  return Opcodes.IFLT;
                case "==":  return Opcodes.IFNE;
                case "=/=": return Opcodes.IFEQ;
            }
        } else {
            switch (op) {
                case "<":   return Opcodes.IFLT;
                case "<=":  return Opcodes.IFLE;
                case ">":   return Opcodes.IFGT;
                case ">=":  return Opcodes.IFGE;
                case "==":  return Opcodes.IFEQ;
                case "=/=": return Opcodes.IFNE;
            }
        }
        throw new RuntimeException("unknown comparison op: " + op);
    }
}
