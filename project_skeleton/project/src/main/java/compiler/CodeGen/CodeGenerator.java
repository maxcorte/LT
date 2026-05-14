package compiler.CodeGen;

import compiler.CodeGen.Signatures.VarInfo;
import compiler.Parser.*;
import compiler.SemanticAnalysis.SymbolTable;
import compiler.SemanticAnalysis.Visitor;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 4: JVM bytecode generation.
 *
 * Public entry point that holds all shared state (current ClassWriter,
 * MethodVisitor, scope, globals, functions, collections, ...) and
 * implements the Visitor interface.
 *
 * Each visit(...) method is short: it either inspects the node enough to
 * dispatch, or delegates to one of the helper classes (ExpressionEmitter,
 * BooleanEmitter, MethodGenerator, ClassGenerator).
 *
 * Helpers are package-private and access the state via this object's
 * package-private fields. There is intentionally no getter/setter
 * boilerplate.
 */
public class CodeGenerator implements Visitor {


    static final String SCANNER_FIELD = "__scanner";
    static final String SCANNER_DESC  = "Ljava/util/Scanner;";


    final SymbolTable symbolTable;
    final String      mainClassName;
    final String      outputDir;

    ClassWriter   currentClassWriter;
    MethodVisitor mv;

    final Scope scope = new Scope();
    Signatures.FunSig currentFunction;

    final Map<String, Signatures.FunSig>     functions   = new HashMap<>();
    final Map<String, Signatures.CollSig>    collections = new HashMap<>();
    final Map<String, Signatures.GlobalInfo> globals     = new LinkedHashMap<>();
    final List<VarDeclNode>                  globalDecls = new ArrayList<>();
    boolean usesScanner;

    final ClassGenerator    classGen;
    final MethodGenerator   methodGen;
    final ExpressionEmitter exprEmit;
    final BooleanEmitter    boolEmit;

    public CodeGenerator(SymbolTable symbolTable, String mainClassName, String outputDir) {
        this.symbolTable   = symbolTable;
        this.mainClassName = mainClassName;
        this.outputDir     = outputDir;
        this.classGen  = new ClassGenerator(this);
        this.methodGen = new MethodGenerator(this);
        this.exprEmit  = new ExpressionEmitter(this);
        this.boolEmit  = new BooleanEmitter(this);
    }

    public void generate(ProgramNode program) throws IOException {
        // Pass 0a: register collection signatures (for forward refs)
        for (ASTNode top : program.topLevels) {
            if (top instanceof CollDeclNode) {
                registerCollection((CollDeclNode) top);
            }
        }
        // Pass 0b: register globals + detect read_* usage
        for (ASTNode top : program.topLevels) {
            if (top instanceof VarDeclNode) {
                registerGlobal((VarDeclNode) top);
            }
        }
        usesScanner = containsReadCall(program);

        // Pass 1: one .class per collection
        for (ASTNode top : program.topLevels) {
            if (top instanceof CollDeclNode) {
                classGen.generateCollClass((CollDeclNode) top);
            }
        }
        // Pass 2: main class
        classGen.generateMainClass(program);
    }

    private void registerCollection(CollDeclNode coll) {
        List<String> fieldNames = new ArrayList<>();
        List<String> fieldTypes = new ArrayList<>();
        StringBuilder ctorDesc = new StringBuilder("(");
        for (FieldDeclNode f : coll.fields) {
            String full = f.type.isArray ? f.type.name + "[]" : f.type.name;
            fieldNames.add(f.id.name);
            fieldTypes.add(full);
            ctorDesc.append(TypeUtils.typeDescriptor(f.type.name, f.type.isArray));
        }
        ctorDesc.append(")V");
        collections.put(coll.name,
                new Signatures.CollSig(coll.name, fieldNames, fieldTypes, ctorDesc.toString()));
    }

    private void registerGlobal(VarDeclNode node) {
        boolean isArr = node.type.isArray;
        String full   = isArr ? node.type.name + "[]" : node.type.name;
        globals.put(node.id.name, new Signatures.GlobalInfo(full, node.isFinal));
        globalDecls.add(node);
    }

    private boolean containsReadCall(ASTNode node) {
        if (node == null) return false;
        if (node instanceof ProgramNode) {
            for (ASTNode top : ((ProgramNode) node).topLevels) {
                if (containsReadCall(top)) return true;
            }
            return false;
        }
        if (node instanceof CallNode) {
            CallNode c = (CallNode) node;
            if (c.callee instanceof VarRefNode
                    && ((VarRefNode) c.callee).name.startsWith("read_")) {
                return true;
            }
            for (ExprNode arg : c.args) if (containsReadCall(arg)) return true;
            return containsReadCall(c.callee);
        }
        if (node instanceof FunDefNode) return containsReadCall(((FunDefNode) node).body);
        if (node instanceof BlockNode) {
            for (StmtNode s : ((BlockNode) node).statements) {
                if (containsReadCall(s)) return true;
            }
            return false;
        }
        if (node instanceof IfNode) {
            IfNode i = (IfNode) node;
            return containsReadCall(i.condition)
                    || containsReadCall(i.thenBlock)
                    || containsReadCall(i.elseBlock);
        }
        if (node instanceof WhileNode) {
            WhileNode w = (WhileNode) node;
            return containsReadCall(w.condition) || containsReadCall(w.body);
        }
        if (node instanceof ForNode) {
            ForNode f = (ForNode) node;
            return containsReadCall(f.start) || containsReadCall(f.end)
                    || containsReadCall(f.step) || containsReadCall(f.body);
        }
        if (node instanceof ReturnNode)      return containsReadCall(((ReturnNode) node).expr);
        if (node instanceof ExprStmtNode)    return containsReadCall(((ExprStmtNode) node).expr);
        if (node instanceof VarDeclStmtNode) return containsReadCall(((VarDeclStmtNode) node).decl);
        if (node instanceof VarDeclNode)     return containsReadCall(((VarDeclNode) node).init);
        if (node instanceof BinaryOpNode) {
            BinaryOpNode b = (BinaryOpNode) node;
            return containsReadCall(b.left) || containsReadCall(b.right);
        }
        if (node instanceof UnaryOpNode)     return containsReadCall(((UnaryOpNode) node).expr);
        if (node instanceof FieldAccessNode) return containsReadCall(((FieldAccessNode) node).base);
        if (node instanceof ArrayAccessNode) {
            ArrayAccessNode a = (ArrayAccessNode) node;
            return containsReadCall(a.base) || containsReadCall(a.index);
        }
        if (node instanceof NewArrayNode)    return containsReadCall(((NewArrayNode) node).size);
        return false;
    }

    @Override
    public void visit(BlockNode node) {
        scope.push();
        for (StmtNode s : node.statements) s.accept(this);
        scope.pop();
    }

    @Override
    public void visit(ExprStmtNode node) {
        node.expr.accept(this);
        if (node.expr instanceof CallNode) {
            String t = node.expr.inferredType;
            if (!TypeUtils.isVoidStackType(t)) {
                mv.visitInsn(Opcodes.POP);
            }
        }
    }

    @Override
    public void visit(VarDeclStmtNode node) { node.decl.accept(this); }

    @Override
    public void visit(VarDeclNode node) {
        String varName  = node.id.name;
        String baseType = node.type.name;
        boolean isArr   = node.type.isArray;
        String fullType = isArr ? baseType + "[]" : baseType;

        Signatures.VarInfo info = scope.declare(varName, fullType);

        if (node.init != null) {
            node.init.accept(this);
            String initType = node.init.inferredType;
            if (TypeUtils.isFloat(baseType) && TypeUtils.isInt(initType) && !isArr) {
                mv.visitInsn(Opcodes.I2F);
            }
            exprEmit.storeLocal(fullType, info.slot);
        } else {
            exprEmit.emitDefaultValue(fullType);
            exprEmit.storeLocal(fullType, info.slot);
        }
    }

    @Override
    public void visit(IfNode node) {
        Label elseLabel = new Label();
        Label endLabel  = new Label();
        boolean hasElse = node.elseBlock != null;

        boolEmit.emitJumpIfFalse(node.condition, hasElse ? elseLabel : endLabel);
        node.thenBlock.accept(this);

        if (hasElse) {
            mv.visitJumpInsn(Opcodes.GOTO, endLabel);
            mv.visitLabel(elseLabel);
            node.elseBlock.accept(this);
        }
        mv.visitLabel(endLabel);
    }

    @Override
    public void visit(WhileNode node) {
        Label startLabel = new Label();
        Label endLabel   = new Label();

        mv.visitLabel(startLabel);
        boolEmit.emitJumpIfFalse(node.condition, endLabel);
        node.body.accept(this);
        mv.visitJumpInsn(Opcodes.GOTO, startLabel);
        mv.visitLabel(endLabel);
    }

    @Override
    public void visit(UnaryPlusOneNode node) {

        VarInfo varInfo = scope.lookup(((VarRefNode) node.exprNode).name);
        mv.visitIincInsn(varInfo.slot,1);

    }

    @Override
    public void visit(UnaryMinusOneNode node) {

        VarInfo varInfo = scope.lookup(((VarRefNode) node.exprNode).name);
        mv.visitIincInsn(varInfo.slot,-1);

    }

    @Override
    public void visit(ForNode node) {
        scope.push();

        // Resolve the loop variable: declared inline, or pre-existing local, or global
        Signatures.VarInfo loopVar = null;
        Signatures.GlobalInfo loopVarGlobal = null;
        String loopVarName = node.varId.name;
        String loopVarType;

        if (node.varType != null) {
            loopVarType = node.varType.isArray
                    ? node.varType.name + "[]" : node.varType.name;
            loopVar = scope.declare(loopVarName, loopVarType);
        } else {
            loopVar = scope.lookup(loopVarName);
            if (loopVar != null) {
                loopVarType = loopVar.type;
            } else {
                loopVarGlobal = globals.get(loopVarName);
                if (loopVarGlobal == null) {
                    throw new RuntimeException(
                            "CodeGen internal: for-loop variable '"
                                    + loopVarName + "' not in scope");
                }
                loopVarType = loopVarGlobal.type;
            }
        }

        boolean varIsFloat = TypeUtils.isFloat(loopVarType);
        boolean isGlobal = (loopVar == null);

        // i := start
        node.start.accept(this);
        if (varIsFloat && TypeUtils.isInt(node.start.inferredType)) {
            mv.visitInsn(Opcodes.I2F);
        }
        if (isGlobal) {
            mv.visitFieldInsn(Opcodes.PUTSTATIC, mainClassName, loopVarName,
                    TypeUtils.descForFullType(loopVarType));
        } else {
            exprEmit.storeLocal(loopVar.type, loopVar.slot);
        }

        Label startLabel = new Label();
        Label endLabel   = new Label();

        mv.visitLabel(startLabel);

        // Test: exit if i > end
        if (isGlobal) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, mainClassName, loopVarName,
                    TypeUtils.descForFullType(loopVarType));
        } else {
            exprEmit.loadLocal(loopVar.type, loopVar.slot);
        }
        node.end.accept(this);
        if (varIsFloat && TypeUtils.isInt(node.end.inferredType)) {
            mv.visitInsn(Opcodes.I2F);
        }
        if (varIsFloat) {
            // Exclusive upper bound: exit when i >= end
            mv.visitInsn(Opcodes.FCMPL);
            mv.visitJumpInsn(Opcodes.IFGE, endLabel);
        } else {
            mv.visitJumpInsn(Opcodes.IF_ICMPGE, endLabel);
        }

        node.body.accept(this);

        // Step: i := step expr
        node.step.accept(this);
        if (varIsFloat && TypeUtils.isInt(node.step.inferredType)) {
            mv.visitInsn(Opcodes.I2F);
        }
        if (isGlobal) {
            mv.visitFieldInsn(Opcodes.PUTSTATIC, mainClassName, loopVarName,
                    TypeUtils.descForFullType(loopVarType));
        } else {
            exprEmit.storeLocal(loopVar.type, loopVar.slot);
        }

        mv.visitJumpInsn(Opcodes.GOTO, startLabel);
        mv.visitLabel(endLabel);

        scope.pop();
    }

    @Override
    public void visit(ReturnNode node) {
        if (node.expr == null) {
            mv.visitInsn(Opcodes.RETURN);
            return;
        }

        node.expr.accept(this);

        String exprType = node.expr.inferredType;
        if (currentFunction != null
                && TypeUtils.isFloat(currentFunction.returnType)
                && TypeUtils.isInt(exprType)
                && !currentFunction.returnIsArray) {
            mv.visitInsn(Opcodes.I2F);
        }

        String rt    = currentFunction != null ? currentFunction.returnType : null;
        boolean rArr = currentFunction != null && currentFunction.returnIsArray;
        if (rArr)                                                          mv.visitInsn(Opcodes.ARETURN);
        else if (TypeUtils.isInt(rt) || TypeUtils.isBool(rt))              mv.visitInsn(Opcodes.IRETURN);
        else if (TypeUtils.isFloat(rt))                                    mv.visitInsn(Opcodes.FRETURN);
        else if (TypeUtils.isString(rt) || TypeUtils.isReference(rt))      mv.visitInsn(Opcodes.ARETURN);
        else                                                               mv.visitInsn(Opcodes.RETURN);
    }

    @Override public void visit(IntLiteralNode node)    { exprEmit.emitIntConstant(node.value); }

    @Override
    public void visit(FloatLiteralNode node) {
        if (node.value == 0.0f)      mv.visitInsn(Opcodes.FCONST_0);
        else if (node.value == 1.0f) mv.visitInsn(Opcodes.FCONST_1);
        else if (node.value == 2.0f) mv.visitInsn(Opcodes.FCONST_2);
        else                         mv.visitLdcInsn(node.value);
    }

    @Override
    public void visit(BoolLiteralNode node) {
        mv.visitInsn(node.value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
    }

    @Override public void visit(StringLiteralNode node) { mv.visitLdcInsn(node.value); }

    @Override public void visit(VarRefNode node)        { exprEmit.emitVarRef(node); }
    @Override public void visit(FieldAccessNode node)   { exprEmit.emitFieldRead(node); }
    @Override public void visit(ArrayAccessNode node)   { exprEmit.emitArrayRead(node); }
    @Override public void visit(NewArrayNode node)      { exprEmit.emitNewArray(node); }

    @Override
    public void visit(BinaryOpNode node) {
        if ("=".equals(node.op)) {
            exprEmit.emitAssignment(node);
            return;
        }
        String op = node.op;
        String lt = node.left.inferredType;
        String rt = node.right.inferredType;

        if ("+".equals(op) && TypeUtils.isString(lt) && TypeUtils.isString(rt)) {
            exprEmit.emitStringConcat(node);
            return;
        }
        if ("+".equals(op) || "-".equals(op) || "*".equals(op)
                || "/".equals(op) || "%".equals(op)) {
            exprEmit.emitArithmetic(node, lt, rt);
            return;
        }
        if (BooleanEmitter.isComparison(op) || "&&".equals(op) || "||".equals(op)) {
            boolEmit.emitBoolValue(node);
            return;
        }
        throw new UnsupportedOperationException("TODO: binary operator '" + op + "'");
    }

    @Override
    public void visit(UnaryOpNode node) { exprEmit.emitUnary(node); }

    @Override
    public void visit(CallNode node) {
        if (!(node.callee instanceof VarRefNode)) {
            throw new UnsupportedOperationException("TODO: indirect calls");
        }
        String name = ((VarRefNode) node.callee).name;

        if (name.equals("println") || name.equals("print")
                || name.equals("write")
                || name.equals("print_INT") || name.equals("print_FLOAT")) {
            exprEmit.emitPrintBuiltin(name, node.args.get(0));
            return;
        }
        if (name.equals("read_INT") || name.equals("read_FLOAT")
                || name.equals("read_STRING")) {
            exprEmit.emitReadBuiltin(name);
            return;
        }
        Signatures.CollSig collSig = collections.get(name);
        if (collSig != null) {
            exprEmit.emitCollectionConstructor(collSig, node.args);
            return;
        }
        Signatures.FunSig sig = functions.get(name);
        if (sig != null) {
            exprEmit.emitUserCall(sig, node.args);
            return;
        }
        throw new RuntimeException("CodeGen internal: unknown call '" + name + "'");
    }

    @Override
    public void visit(TopLevelNode node) {
        throw new UnsupportedOperationException("Internal: visit on abstract TopLevelNode");
    }

    @Override
    public void visit(FunDefNode node) {
        throw new UnsupportedOperationException("FunDefNode dispatched at higher level");
    }

    @Override
    public void visit(CollDeclNode node) {
        throw new UnsupportedOperationException("CollDeclNode dispatched at higher level");
    }

    @Override public void visit(ParamNode node)       { /* handled in MethodGenerator */ }
    @Override public void visit(FieldDeclNode node)   { /* handled in ClassGenerator */ }

    @Override public void visit(IdentifierNode node)  { /* helper, never visited directly */ }
    @Override public void visit(TypeNode node)        { /* helper, never visited directly */ }
}