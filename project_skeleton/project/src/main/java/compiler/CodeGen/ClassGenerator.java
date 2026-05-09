package compiler.CodeGen;

import compiler.Parser.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ClassGenerator {

    private final CodeGenerator cg;

    ClassGenerator(CodeGenerator cg) { this.cg = cg; }

    void generateMainClass(ProgramNode program) throws IOException {
        cg.currentClassWriter = new ClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cg.currentClassWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,
                cg.mainClassName, null, "java/lang/Object", null);

        emitStaticFields();
        cg.methodGen.emitDefaultConstructor();

        // Pre-pass: register every function's signature so that calls inside
        // any body (including forward refs and recursion) can be resolved.
        cg.functions.clear();
        for (ASTNode top : program.topLevels) {
            if (top instanceof FunDefNode) {
                FunDefNode fn = (FunDefNode) top;
                cg.functions.put(fn.name, makeFunSig(fn));
            }
        }

        // <clinit> for global initializers and the scanner
        if (!cg.globalDecls.isEmpty() || cg.usesScanner) {
            cg.methodGen.generateClinit();
        }

        // Generate every function body (incl. main)
        for (ASTNode top : program.topLevels) {
            if (top instanceof FunDefNode) {
                FunDefNode fn = (FunDefNode) top;
                if ("main".equals(fn.name)) {
                    cg.methodGen.generateMainMethod(fn);
                } else {
                    cg.methodGen.generateUserFunction(fn);
                }
            }
        }

        cg.currentClassWriter.visitEnd();
        writeClassFile(cg.mainClassName, cg.currentClassWriter.toByteArray());
    }

    void generateCollClass(CollDeclNode coll) throws IOException {
        ClassWriter cw = new ClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, coll.name,
                null, "java/lang/Object", null);

        // Public fields, in declaration order
        for (FieldDeclNode f : coll.fields) {
            String desc = TypeUtils.typeDescriptor(f.type.name, f.type.isArray);
            cw.visitField(Opcodes.ACC_PUBLIC, f.id.name, desc, null, null).visitEnd();
        }

        Signatures.CollSig sig = cg.collections.get(coll.name);
        cg.methodGen.generateCollConstructor(cw, coll, sig);

        cw.visitEnd();
        writeClassFile(coll.name, cw.toByteArray());
    }

    private void emitStaticFields() {
        for (Map.Entry<String, Signatures.GlobalInfo> e : cg.globals.entrySet()) {
            String name = e.getKey();
            Signatures.GlobalInfo info = e.getValue();
            String desc = TypeUtils.descForFullType(info.type);
            int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
            if (info.isFinal) access |= Opcodes.ACC_FINAL;
            cg.currentClassWriter.visitField(access, name, desc, null, null).visitEnd();
        }
        if (cg.usesScanner) {
            cg.currentClassWriter.visitField(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    CodeGenerator.SCANNER_FIELD, CodeGenerator.SCANNER_DESC,
                    null, null).visitEnd();
        }
    }

    private Signatures.FunSig makeFunSig(FunDefNode fn) {
        List<String> paramTypes = new ArrayList<String>();
        StringBuilder desc = new StringBuilder("(");
        for (ParamNode p : fn.params) {
            String full = p.type.isArray ? p.type.name + "[]" : p.type.name;
            paramTypes.add(full);
            desc.append(TypeUtils.typeDescriptor(p.type.name, p.type.isArray));
        }
        desc.append(")");
        if (fn.returnType == null) desc.append("V");
        else                       desc.append(TypeUtils.typeDescriptor(
                                            fn.returnType.name, fn.returnType.isArray));
        String retType   = fn.returnType != null ? fn.returnType.name : null;
        boolean retIsArr = fn.returnType != null && fn.returnType.isArray;
        return new Signatures.FunSig(fn.name, retType, retIsArr, paramTypes, desc.toString());
    }

    private void writeClassFile(String name, byte[] bytes) throws IOException {
        File dir = new File(cg.outputDir);
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, name + ".class");
        FileOutputStream fos = new FileOutputStream(f);
        try { fos.write(bytes); } finally { fos.close(); }
        System.out.println("[codegen] wrote " + f.getAbsolutePath());
    }
}
