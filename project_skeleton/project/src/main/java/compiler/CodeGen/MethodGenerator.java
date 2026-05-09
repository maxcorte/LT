package compiler.CodeGen;

import compiler.Parser.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class MethodGenerator {

    private final CodeGenerator cg;

    MethodGenerator(CodeGenerator cg) { this.cg = cg; }

    void generateMainMethod(FunDefNode fn) {
        cg.currentFunction = cg.functions.get(fn.name);
        cg.mv = cg.currentClassWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null, null);
        cg.mv.visitCode();

        cg.scope.reset(1);   // slot 0 = String[] args

        fn.body.accept(cg);

        cg.mv.visitInsn(Opcodes.RETURN);
        cg.mv.visitMaxs(0, 0);
        cg.mv.visitEnd();
        cg.mv = null;
        cg.currentFunction = null;
    }

    void generateUserFunction(FunDefNode fn) {
        Signatures.FunSig sig = cg.functions.get(fn.name);
        cg.currentFunction = sig;

        cg.mv = cg.currentClassWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                fn.name,
                sig.descriptor,
                null, null);
        cg.mv.visitCode();

        cg.scope.reset(0);
        cg.scope.push();

        for (int i = 0; i < fn.params.size(); i++) {
            ParamNode p = fn.params.get(i);
            String full = sig.paramTypes.get(i);
            cg.scope.declare(p.id.name, full);
        }

        fn.body.accept(cg);

        // For void functions, add a final RETURN as a safety net.
        // For non-void, we rely on every path having an explicit return;
        // otherwise the JVM verifier will reject the .class.
        if (sig.returnType == null) {
            cg.mv.visitInsn(Opcodes.RETURN);
        }

        cg.scope.pop();
        cg.mv.visitMaxs(0, 0);
        cg.mv.visitEnd();
        cg.mv = null;
        cg.currentFunction = null;
    }

    void generateClinit() {
        cg.mv = cg.currentClassWriter.visitMethod(
                Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        cg.mv.visitCode();

        cg.scope.reset(0);
        cg.scope.push();
        cg.currentFunction = null;

        // Init the scanner first so global initializers can call read_*
        if (cg.usesScanner) {
            cg.mv.visitTypeInsn(Opcodes.NEW, "java/util/Scanner");
            cg.mv.visitInsn(Opcodes.DUP);
            cg.mv.visitFieldInsn(Opcodes.GETSTATIC,
                    "java/lang/System", "in", "Ljava/io/InputStream;");
            cg.mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false);
            cg.mv.visitFieldInsn(Opcodes.PUTSTATIC,
                    cg.mainClassName, CodeGenerator.SCANNER_FIELD, CodeGenerator.SCANNER_DESC);
        }

        for (VarDeclNode g : cg.globalDecls) {
            emitGlobalInit(g);
        }

        cg.scope.pop();
        cg.mv.visitInsn(Opcodes.RETURN);
        cg.mv.visitMaxs(0, 0);
        cg.mv.visitEnd();
        cg.mv = null;
    }

    private void emitGlobalInit(VarDeclNode node) {
        // No initializer => leave the field at JVM default (0/0.0/null).
        if (node.init == null) return;

        node.init.accept(cg);
        String baseType = node.type.name;
        boolean isArr = node.type.isArray;
        if (TypeUtils.isFloat(baseType) && TypeUtils.isInt(node.init.inferredType) && !isArr) {
            cg.mv.visitInsn(Opcodes.I2F);
        }
        String desc = TypeUtils.typeDescriptor(baseType, isArr);
        cg.mv.visitFieldInsn(Opcodes.PUTSTATIC, cg.mainClassName, node.id.name, desc);
    }

    void emitDefaultConstructor() {
        MethodVisitor c = cg.currentClassWriter.visitMethod(
                Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        c.visitCode();
        c.visitVarInsn(Opcodes.ALOAD, 0);
        c.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        c.visitInsn(Opcodes.RETURN);
        c.visitMaxs(0, 0);
        c.visitEnd();
    }

    /** Generate the constructor for a coll class on the given (separate) ClassWriter. */
    void generateCollConstructor(ClassWriter cw, CollDeclNode coll, Signatures.CollSig sig) {
        MethodVisitor cv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                sig.constructorDescriptor, null, null);
        cv.visitCode();

        // super()
        cv.visitVarInsn(Opcodes.ALOAD, 0);
        cv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);

        // this.fieldN = paramN
        int paramSlot = 1;
        for (int i = 0; i < coll.fields.size(); i++) {
            FieldDeclNode f = coll.fields.get(i);
            String desc = TypeUtils.typeDescriptor(f.type.name, f.type.isArray);

            cv.visitVarInsn(Opcodes.ALOAD, 0);
            cv.visitVarInsn(TypeUtils.loadOpcode(f.type.name, f.type.isArray), paramSlot);
            cv.visitFieldInsn(Opcodes.PUTFIELD, coll.name, f.id.name, desc);
            paramSlot += 1;
        }

        cv.visitInsn(Opcodes.RETURN);
        cv.visitMaxs(0, 0);
        cv.visitEnd();
    }
}
