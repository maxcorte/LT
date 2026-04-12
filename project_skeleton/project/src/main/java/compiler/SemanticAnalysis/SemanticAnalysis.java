package compiler.SemanticAnalysis;

import compiler.Parser.*;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalysis implements Visitor {

    private SymbolTable           table;
    private String                currentType;
    private SymbolTable.FunSymbol currentFunction;

    // entree public

    public void analyze(ProgramNode root) {
        table = new SymbolTable();
        registerBuiltins();

        // Passe 1 : enregistrement des fonctions et collections
        for (ASTNode node : root.topLevels) {
            if      (node instanceof FunDefNode)   registerFunction((FunDefNode)   node);
            else if (node instanceof CollDeclNode) registerCollection((CollDeclNode) node);
        }

        // Passe 2 : analyse complete
        table.pushScope();
        for (ASTNode node : root.topLevels) {
            node.accept(this);
        }
        table.popScope();
    }

    // Builtins

    private void registerBuiltins() {
        SymbolTable.VarSymbol pStr   = p("s", "STRING", false);
        SymbolTable.VarSymbol pInt   = p("n", "INT",    false);
        SymbolTable.VarSymbol pFloat = p("f", "FLOAT",  false);

        builtin("print",        null,     false, List.of(pStr));
        builtin("println",      null,     false, List.of(pStr));
        builtin("write",        null,     false, List.of(pStr));
        builtin("print_INT",    null,     false, List.of(pInt));
        builtin("print_FLOAT",  null,     false, List.of(pFloat));
        builtin("read_INT",     "INT",    false, List.of());
        builtin("read_FLOAT",   "FLOAT",  false, List.of());
        builtin("read_STRING",  "STRING", false, List.of());
    }

    private SymbolTable.VarSymbol p(String n, String t, boolean arr) {
        return new SymbolTable.VarSymbol(n, t, arr, false);
    }

    private void builtin(String name, String ret, boolean retArr,
                         List<SymbolTable.VarSymbol> params) {
        table.declareFunction(
                new SymbolTable.FunSymbol(name, ret, retArr, params, true));
    }


    private void registerFunction(FunDefNode node) {
        String  retType  = node.returnType != null ? node.returnType.name  : null;
        boolean retIsArr = node.returnType != null && node.returnType.isArray;

        List<SymbolTable.VarSymbol> params = new ArrayList<>();
        for (ParamNode param : node.params) {
            params.add(new SymbolTable.VarSymbol(
                    param.id.name, param.type.name, param.type.isArray, false));
        }
        table.declareFunction(
                new SymbolTable.FunSymbol(node.name, retType, retIsArr, params, false));
    }

    private void registerCollection(CollDeclNode node) {
        SymbolTable.CollSymbol coll = new SymbolTable.CollSymbol(node.name);
        for (FieldDeclNode f : node.fields) {
            coll.fields.put(f.id.name,
                    new SymbolTable.VarSymbol(f.id.name, f.type.name, f.type.isArray, false));
        }
        table.declareCollection(coll);
    }

    // visitor : declarations top-level

    @Override
    public void visit(TopLevelNode node) {
        throw new SemanticException("Internal: visit called on abstract TopLevelNode");
    }

    @Override
    public void visit(FunDefNode node) {
        currentFunction = table.lookupFunction(node.name);
        table.pushScope();
        for (ParamNode param : node.params) {
            param.accept(this);
        }
        node.body.accept(this);
        table.popScope();
        currentFunction = null;
    }

    @Override
    public void visit(CollDeclNode node) {
        for (FieldDeclNode f : node.fields) {
            f.accept(this);
        }
    }

    @Override
    public void visit(VarDeclNode node) {
        String  declaredType = node.type.name;
        boolean isArray      = node.type.isArray;

        if (!isPrimitive(declaredType) && !table.isCollection(declaredType)) {
            throw new SemanticException(
                    "TypeError: unknown type '" + declaredType +
                            "' for variable '" + node.id.name + "'");
        }

        if (node.init != null) {
            String initType = inferType(node.init);
            checkTypeError(declaredType, isArray, initType,
                    "variable '" + node.id.name + "'");
        } else if (node.isFinal) {
            throw new SemanticException(
                    "TypeError: final variable '" + node.id.name + "' must be initialized");
        }

        table.declareVar(new SymbolTable.VarSymbol(
                node.id.name, declaredType, isArray, node.isFinal));
    }

    @Override public void visit(VarDeclStmtNode node) { node.decl.accept(this); }

    @Override
    public void visit(ParamNode node) {
        if (!isPrimitive(node.type.name) && !table.isCollection(node.type.name)) {
            throw new SemanticException(
                    "TypeError: unknown type '" + node.type.name +
                            "' for parameter '" + node.id.name + "'");
        }
        table.declareVar(new SymbolTable.VarSymbol(
                node.id.name, node.type.name, node.type.isArray, false));
    }

    @Override
    public void visit(FieldDeclNode node) {
        if (!isPrimitive(node.type.name) && !table.isCollection(node.type.name)) {
            throw new SemanticException(
                    "TypeError: unknown type '" + node.type.name +
                            "' for field '" + node.id.name + "'");
        }
    }

    // visitor: instructions

    @Override
    public void visit(BlockNode node) {
        table.pushScope();
        for (StmtNode s : node.statements) {
            s.accept(this);
        }
        table.popScope();
    }

    @Override
    public void visit(IfNode node) {
        String condType = inferType(node.condition);
        if (!"BOOL".equals(condType)) {
            throw new SemanticException(
                    "MissingConditionError: if condition must be BOOL, got '" + condType + "'");
        }
        node.thenBlock.accept(this);
        if (node.elseBlock != null) node.elseBlock.accept(this);
    }

    @Override
    public void visit(WhileNode node) {
        String condType = inferType(node.condition);
        if (!"BOOL".equals(condType)) {
            throw new SemanticException(
                    "MissingConditionError: while condition must be BOOL, got '" + condType + "'");
        }
        node.body.accept(this);
    }

    @Override
    public void visit(ForNode node) {
        table.pushScope();

        if (node.varType != null) {
            if (!"INT".equals(node.varType.name)) {
                throw new SemanticException(
                        "TypeError: for loop variable must be INT, got '" + node.varType.name + "'");
            }
            table.declareVar(
                    new SymbolTable.VarSymbol(node.varId.name, "INT", false, false));
        } else {
            SymbolTable.VarSymbol v = table.lookupVar(node.varId.name);
            if (v == null) {
                throw new SemanticException(
                        "ScopeError: undefined variable '" + node.varId.name + "' in for loop");
            }
            if (!"INT".equals(v.type)) {
                throw new SemanticException(
                        "TypeError: for loop variable '" + node.varId.name +
                                "' must be INT, got '" + v.type + "'");
            }
        }

        requireNumeric(inferType(node.start), "for start");
        requireNumeric(inferType(node.end),   "for end");
        requireNumeric(inferType(node.step),  "for step");

        node.body.accept(this);
        table.popScope();
    }

    @Override
    public void visit(ReturnNode node) {
        String expected = currentFunction != null ? currentFunction.returnType : null;

        if (node.expr == null) {
            if (expected != null) {
                throw new SemanticException(
                        "ReturnError: function '" + currentFunction.name +
                                "' must return a value of type '" + expected + "'");
            }
        } else {
            String actual = inferType(node.expr);
            if (expected == null) {
                throw new SemanticException(
                        "ReturnError: void function '" +
                                (currentFunction != null ? currentFunction.name : "?") +
                                "' must not return a value");
            }
            if (!isAssignable(expected, currentFunction.returnIsArray, actual)) {
                throw new SemanticException(
                        "ReturnError: function '" + currentFunction.name +
                                "' must return '" +
                                (currentFunction.returnIsArray ? expected + "[]" : expected) +
                                "', but got '" + actual + "'");
            }
        }
    }

    @Override public void visit(ExprStmtNode node) { node.expr.accept(this); }

    // visitor : expressions

    @Override
    public void visit(BinaryOpNode node) {
        switch (node.op) {

            case "=": {
                checkLValue(node.left);
                String leftType  = inferType(node.left);
                String rightType = inferType(node.right);
                boolean leftArr  = leftType.endsWith("[]");
                String  leftBase = leftArr
                        ? leftType.substring(0, leftType.length() - 2)
                        : leftType;
                checkTypeError(leftBase, leftArr, rightType, "assignment");
                currentType = leftType;
                break;
            }

            case "+": case "-": case "*": case "/": case "%": {
                String l = inferType(node.left);
                String r = inferType(node.right);
                if (!isNumeric(l) || !isNumeric(r)) {
                    throw new SemanticException(
                            "OperatorError: operator '" + node.op +
                                    "' requires numeric operands, got '" + l + "' and '" + r + "'");
                }
                currentType = (l.equals("FLOAT") || r.equals("FLOAT")) ? "FLOAT" : "INT";
                break;
            }

            case "<": case "<=": case ">": case ">=": {
                String l = inferType(node.left);
                String r = inferType(node.right);
                if (!isNumeric(l) || !isNumeric(r)) {
                    throw new SemanticException(
                            "OperatorError: operator '" + node.op +
                                    "' requires numeric operands, got '" + l + "' and '" + r + "'");
                }
                currentType = "BOOL";
                break;
            }

            case "==": case "=/=": {
                String l = inferType(node.left);
                String r = inferType(node.right);
                if (!l.equals(r) && !(isNumeric(l) && isNumeric(r))) {
                    throw new SemanticException(
                            "OperatorError: operator '" + node.op +
                                    "' requires compatible types, got '" + l + "' and '" + r + "'");
                }
                currentType = "BOOL";
                break;
            }

            case "&&": case "||": {
                String l = inferType(node.left);
                String r = inferType(node.right);
                if (!"BOOL".equals(l) || !"BOOL".equals(r)) {
                    throw new SemanticException(
                            "OperatorError: operator '" + node.op +
                                    "' requires BOOL operands, got '" + l + "' and '" + r + "'");
                }
                currentType = "BOOL";
                break;
            }

            default:
                throw new SemanticException(
                        "OperatorError: unknown operator '" + node.op + "'");
        }
    }

    @Override
    public void visit(UnaryOpNode node) {
        String t = inferType(node.expr);
        switch (node.op) {
            case "-":
                if (!isNumeric(t)) {
                    throw new SemanticException(
                            "OperatorError: unary '-' requires a numeric operand, got '" + t + "'");
                }
                currentType = t;
                break;
            case "not":
                if (!"BOOL".equals(t)) {
                    throw new SemanticException(
                            "OperatorError: 'not' requires a BOOL operand, got '" + t + "'");
                }
                currentType = "BOOL";
                break;
            default:
                throw new SemanticException(
                        "OperatorError: unknown unary operator '" + node.op + "'");
        }
    }

    @Override
    public void visit(VarRefNode node) {
        SymbolTable.VarSymbol var = table.lookupVar(node.name);
        if (var != null) { currentType = var.fullType(); return; }

        SymbolTable.FunSymbol fun = table.lookupFunction(node.name);
        if (fun != null) {
            currentType = fun.returnType != null ? fun.returnType : "void";
            return;
        }

        if (table.isCollection(node.name)) { currentType = node.name; return; }

        throw new SemanticException(
                "ScopeError: undefined identifier '" + node.name + "'");
    }

    @Override
    public void visit(CallNode node) {
        String funName;
        if (node.callee instanceof VarRefNode) {
            funName = ((VarRefNode) node.callee).name;
        } else {
            node.callee.accept(this);
            for (ExprNode arg : node.args) arg.accept(this);
            currentType = "UNKNOWN";
            return;
        }

        SymbolTable.CollSymbol coll = table.lookupCollection(funName);
        if (coll != null) {
            if (node.args.size() != coll.fields.size()) {
                throw new SemanticException(
                        "ArgumentError: constructor '" + funName + "' expects " +
                                coll.fields.size() + " arguments, got " + node.args.size());
            }
            int i = 0;
            for (SymbolTable.VarSymbol field : coll.fields.values()) {
                String argType = inferType(node.args.get(i));
                if (!isAssignable(field.type, field.isArray, argType)) {
                    throw new SemanticException(
                            "ArgumentError: constructor '" + funName +
                                    "' field '" + field.name +
                                    "' expects '" + field.fullType() +
                                    "', got '" + argType + "'");
                }
                i++;
            }
            currentType = funName;
            return;
        }

        SymbolTable.FunSymbol fun = table.lookupFunction(funName);
        if (fun == null) {
            throw new SemanticException(
                    "ScopeError: undefined function '" + funName + "'");
        }

        if (node.args.size() != fun.params.size()) {
            throw new SemanticException(
                    "ArgumentError: function '" + funName + "' expects " +
                            fun.params.size() + " argument(s), got " + node.args.size());
        }

        for (int i = 0; i < node.args.size(); i++) {
            String argType = inferType(node.args.get(i));
            if (!fun.isBuiltin) {
                SymbolTable.VarSymbol expected = fun.params.get(i);
                if (!isAssignable(expected.type, expected.isArray, argType)) {
                    throw new SemanticException(
                            "ArgumentError: function '" + funName +
                                    "' parameter " + (i + 1) +
                                    " ('" + expected.name + "') expects '" + expected.fullType() +
                                    "', got '" + argType + "'");
                }
            }
        }

        currentType = fun.returnType != null
                ? (fun.returnIsArray ? fun.returnType + "[]" : fun.returnType)
                : "void";
    }

    @Override
    public void visit(ArrayAccessNode node) {
        String baseType = inferType(node.base);
        if (!baseType.endsWith("[]")) {
            throw new SemanticException(
                    "TypeError: '[]' applied to non-array type '" + baseType + "'");
        }
        String indexType = inferType(node.index);
        if (!"INT".equals(indexType)) {
            throw new SemanticException(
                    "TypeError: array index must be INT, got '" + indexType + "'");
        }
        currentType = baseType.substring(0, baseType.length() - 2);
    }

    @Override
    public void visit(FieldAccessNode node) {
        String baseType = inferType(node.base);
        SymbolTable.CollSymbol coll = table.lookupCollection(baseType);
        if (coll == null) {
            throw new SemanticException(
                    "TypeError: '." + node.field +
                            "' applied to non-collection type '" + baseType + "'");
        }
        SymbolTable.VarSymbol field = coll.fields.get(node.field);
        if (field == null) {
            throw new SemanticException(
                    "ScopeError: unknown field '" + node.field +
                            "' in collection '" + baseType + "'");
        }
        currentType = field.fullType();
    }

    @Override
    public void visit(NewArrayNode node) {
        String sizeType = inferType(node.size);
        if (!"INT".equals(sizeType)) {
            throw new SemanticException(
                    "TypeError: array size must be INT, got '" + sizeType + "'");
        }
        if (!isPrimitive(node.elementType) && !table.isCollection(node.elementType)) {
            throw new SemanticException(
                    "TypeError: unknown element type '" + node.elementType + "'");
        }
        currentType = node.elementType + "[]";
    }

    // litteraux

    @Override public void visit(IntLiteralNode    node) { currentType = "INT";    }
    @Override public void visit(FloatLiteralNode  node) { currentType = "FLOAT";  }
    @Override public void visit(BoolLiteralNode   node) { currentType = "BOOL";   }
    @Override public void visit(StringLiteralNode node) { currentType = "STRING"; }

    @Override public void visit(IdentifierNode node) {}
    @Override public void visit(TypeNode       node) {}

    // helpers

    private String inferType(ExprNode expr) {
        expr.accept(this);
        return currentType;
    }

    private boolean isPrimitive(String type) {
        return "INT".equals(type) || "FLOAT".equals(type)
                || "BOOL".equals(type) || "STRING".equals(type);
    }

    private boolean isNumeric(String type) {
        return "INT".equals(type) || "FLOAT".equals(type);
    }

    private void requireNumeric(String type, String context) {
        if (!isNumeric(type)) {
            throw new SemanticException(
                    "TypeError: " + context + " must be numeric, got '" + type + "'");
        }
    }

    private void checkTypeError(String expected, boolean expectedArr,
                                String actual, String context) {
        if (!isAssignable(expected, expectedArr, actual)) {
            String expectedFull = expectedArr ? expected + "[]" : expected;
            throw new SemanticException(
                    "TypeError: cannot assign '" + actual +
                            "' to '" + expectedFull + "' in " + context);
        }
    }

    private boolean isAssignable(String expected, boolean expectedArr, String actual) {
        String expectedFull = expectedArr ? expected + "[]" : expected;
        if (expectedFull.equals(actual)) return true;
        if ("FLOAT".equals(expected) && !expectedArr && "INT".equals(actual)) return true;
        return false;
    }

    private void checkLValue(ExprNode expr) {
        if (expr instanceof VarRefNode) {
            SymbolTable.VarSymbol sym = table.lookupVar(((VarRefNode) expr).name);
            if (sym != null && sym.isFinal) {
                throw new SemanticException(
                        "TypeError: cannot assign to final variable '" + sym.name + "'");
            }
        } else if (expr instanceof ArrayAccessNode || expr instanceof FieldAccessNode) {
            // OK
        } else {
            throw new SemanticException(
                    "TypeError: left side of '=' is not assignable");
        }
    }
}