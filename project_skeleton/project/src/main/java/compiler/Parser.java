package compiler;

import compiler.Lexer.Lexer;
import compiler.Lexer.Lexer.Sym;
import compiler.Lexer.Symbol;

import java.util.ArrayList;
import java.util.List;

// AST de base

abstract class ASTNode {
    public abstract void print(String indent);
    protected void printIndent(String indent) {
        System.out.print(indent);
    }
}

// Programme complet
class ProgramNode extends ASTNode {
    public final List<ASTNode> topLevels = new ArrayList<>();

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Program");
        for (ASTNode n : topLevels) {
            n.print(indent + "  ");
        }
    }
}

// Types & identifiants

class TypeNode extends ASTNode {
    public final String name;   // "INT", "FLOAT", "Point", ...
    public final boolean isArray;

    public TypeNode(String name, boolean isArray) {
        this.name = name;
        this.isArray = isArray;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Type, " + name + (isArray ? "[]" : ""));
    }
}

class IdentifierNode extends ASTNode {
    public final String name;

    public IdentifierNode(String name) {
        this.name = name;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Identifier, " + name);
    }
}

//Déclarations

abstract class TopLevelNode extends ASTNode { }

class VarDeclNode extends TopLevelNode {
    public final boolean isFinal;
    public final TypeNode type;
    public final IdentifierNode id;
    public final ExprNode init; // peut être null

    public VarDeclNode(boolean isFinal, TypeNode type, IdentifierNode id, ExprNode init) {
        this.isFinal = isFinal;
        this.type = type;
        this.id = id;
        this.init = init;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("VarDecl " + (isFinal ? "FINAL" : ""));
        type.print(indent + "  ");
        id.print(indent + "  ");
        if (init != null) {
            printIndent(indent + "  ");
            System.out.println("AssignmentOperator");
            init.print(indent + "    ");
        }
    }
}

// Déclaration d’un champ dans une collection
class FieldDeclNode extends ASTNode {
    public final TypeNode type;
    public final IdentifierNode id;

    public FieldDeclNode(TypeNode type, IdentifierNode id) {
        this.type = type;
        this.id = id;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("FieldDecl");
        type.print(indent + "  ");
        id.print(indent + "  ");
    }
}

// Déclaration de collection : coll Point { ... }
class CollDeclNode extends TopLevelNode {
    public final String name;
    public final List<FieldDeclNode> fields = new ArrayList<>();

    public CollDeclNode(String name) {
        this.name = name;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Collection, " + name);
        for (FieldDeclNode f : fields) {
            f.print(indent + "  ");
        }
    }
}

// Fonctions

class ParamNode extends ASTNode {
    public final TypeNode type;
    public final IdentifierNode id;

    public ParamNode(TypeNode type, IdentifierNode id) {
        this.type = type;
        this.id = id;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Param");
        type.print(indent + "  ");
        id.print(indent + "  ");
    }
}

class FunDefNode extends TopLevelNode {
    public final TypeNode returnType; // peut être null (ex: main)
    public final String name;
    public List<ParamNode> params = new ArrayList<>();
    public final BlockNode body;

    public FunDefNode(TypeNode returnType, String name, List<ParamNode> params, BlockNode body) {
        this.returnType = returnType;
        this.name = name;
        this.params = params;
        this.body = body;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Function, " + name);
        if (returnType != null) {
            returnType.print(indent + "  ");
        } else {
            printIndent(indent + "  ");
            System.out.println("Type, void");
        }
        for (ParamNode p : params) {
            p.print(indent + "  ");
        }
        body.print(indent + "  ");
    }
}

// Instructions

abstract class StmtNode extends ASTNode { }

class BlockNode extends StmtNode {
    public final List<StmtNode> statements = new ArrayList<>();

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Block");
        for (StmtNode s : statements) {
            s.print(indent + "  ");
        }
    }
}

// Déclaration locale (on réutilise VarDeclNode)
class VarDeclStmtNode extends StmtNode {
    public final VarDeclNode decl;
    public VarDeclStmtNode(VarDeclNode decl) {
        this.decl = decl;
    }
    @Override
    public void print(String indent) {
        decl.print(indent);
    }
}

class ExprStmtNode extends StmtNode {
    public final ExprNode expr;
    public ExprStmtNode(ExprNode expr) { this.expr = expr; }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("ExprStmt");
        expr.print(indent + "  ");
    }
}

class IfNode extends StmtNode {
    public final ExprNode condition;
    public final BlockNode thenBlock;
    public final BlockNode elseBlock; // peut être null

    public IfNode(ExprNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("If");
        condition.print(indent + "  ");
        thenBlock.print(indent + "  ");
        if (elseBlock != null) {
            printIndent(indent);
            System.out.println("Else");
            elseBlock.print(indent + "  ");
        }
    }
}

class WhileNode extends StmtNode {
    public final ExprNode condition;
    public final BlockNode body;

    public WhileNode(ExprNode condition, BlockNode body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("While");
        condition.print(indent + "  ");
        body.print(indent + "  ");
    }
}

// for (INT i; 1 -> 100; i + 1) { ... }
class ForNode extends StmtNode {
    public final TypeNode varType;
    public final IdentifierNode varId;
    public final ExprNode start;
    public final ExprNode end;
    public final ExprNode step;
    public final BlockNode body;

    public ForNode(TypeNode varType, IdentifierNode varId,
                   ExprNode start, ExprNode end, ExprNode step,
                   BlockNode body) {
        this.varType = varType;
        this.varId = varId;
        this.start = start;
        this.end = end;
        this.step = step;
        this.body = body;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("For");
        varType.print(indent + "  ");
        varId.print(indent + "  ");
        printIndent(indent + "  ");
        System.out.println("Range");
        start.print(indent + "    ");
        end.print(indent + "    ");
        printIndent(indent + "  ");
        System.out.println("Step");
        step.print(indent + "    ");
        body.print(indent + "  ");
    }
}

class ReturnNode extends StmtNode {
    public final ExprNode expr; // peut être null

    public ReturnNode(ExprNode expr) {
        this.expr = expr;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Return");
        if (expr != null) {
            expr.print(indent + "  ");
        }
    }
}

//  Expressions

abstract class ExprNode extends ASTNode { }

class IntLiteralNode extends ExprNode {
    public final int value;
    public IntLiteralNode(int value) { this.value = value; }
    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Integer, " + value);
    }
}

class FloatLiteralNode extends ExprNode {
    public final float value;
    public FloatLiteralNode(float value) { this.value = value; }
    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Float, " + value);
    }
}

class StringLiteralNode extends ExprNode {
    public final String value;
    public StringLiteralNode(String value) { this.value = value; }
    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("String, " + value);
    }
}

class BoolLiteralNode extends ExprNode {
    public final boolean value;
    public BoolLiteralNode(boolean value) { this.value = value; }
    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Bool, " + value);
    }
}

class VarRefNode extends ExprNode {
    public final String name;
    public VarRefNode(String name) { this.name = name; }
    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Identifier, " + name);
    }
}

// Op binaire (arithmétique, comparaison, logique)
class BinaryOpNode extends ExprNode {
    public final String op; // "+", "*", "==", "=/=", "&&", ...
    public final ExprNode left, right;

    public BinaryOpNode(String op, ExprNode left, ExprNode right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("BinaryOp, " + op);
        left.print(indent + "  ");
        right.print(indent + "  ");
    }
}

class UnaryOpNode extends ExprNode {
    public final String op; // "-", "not"
    public final ExprNode expr;
    public UnaryOpNode(String op, ExprNode expr) {
        this.op = op;
        this.expr = expr;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("UnaryOp, " + op);
        expr.print(indent + "  ");
    }
}

// Appel de fonction / constructeur : f(...), Point(...), read_INT()
class CallNode extends ExprNode {
    public final ExprNode callee;      // VarRefNode ou autre (pour éventuellement supporter obj.method())
    public final List<ExprNode> args;  // peut être vide

    public CallNode(ExprNode callee, List<ExprNode> args) {
        this.callee = callee;
        this.args = args;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("Call");
        callee.print(indent + "  ");
        for (ExprNode a : args) {
            a.print(indent + "  ");
        }
    }
}

// Accès tableau : base[index]
class ArrayAccessNode extends ExprNode {
    public final ExprNode base;
    public final ExprNode index;

    public ArrayAccessNode(ExprNode base, ExprNode index) {
        this.base = base;
        this.index = index;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("ArrayAccess");
        base.print(indent + "  ");
        index.print(indent + "  ");
    }
}

// Accès champ : base.field
class FieldAccessNode extends ExprNode {
    public final ExprNode base;
    public final String field;

    public FieldAccessNode(ExprNode base, String field) {
        this.base = base;
        this.field = field;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("FieldAccess, " + field);
        base.print(indent + "  ");
    }
}

// Création de tableau : INT ARRAY[5]
class NewArrayNode extends ExprNode {
    public final String elementType;
    public final ExprNode size;

    public NewArrayNode(String elementType, ExprNode size) {
        this.elementType = elementType;
        this.size = size;
    }

    @Override
    public void print(String indent) {
        printIndent(indent);
        System.out.println("NewArray, " + elementType);
        size.print(indent + "  ");
    }
}

// Parser

public class Parser {
    private final List<Symbol> tokens = new ArrayList<>();
    private int pos = 0;

    public Parser(Lexer lexer) {
        // On lit tous les tokens du lexer
        Symbol s;
        do {
            s = lexer.getNextSymbol();
            tokens.add(s);
        } while (s.sym != Sym.EOF);
    }

    private Symbol current() {
        return tokens.get(pos);
    }

    private Symbol lookahead(int k) {
        int i = pos + k;
        if (i >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(i);
    }

    private boolean match(int sym) {
        if (current().sym == sym) {
            pos++;
            return true;
        }
        return false;
    }

    private Symbol expect(int sym) {
        Symbol c = current();
        if (c.sym != sym) {
            throw new RuntimeException("Parse error at line " + c.line +
                    ", column " + c.column + ": expected " + sym + " but found " + c.sym);
        }
        pos++;
        return c;
    }

    // renvoie la racine de l’AST
    public ProgramNode getAST() {
        return parseProgram();
    }

    // --------------- Program ---------------

    private ProgramNode parseProgram() {
        ProgramNode prog = new ProgramNode();
        while (current().sym != Sym.EOF) {
            prog.topLevels.add(parseTopLevel());
        }
        return prog;
    }

    private TopLevelNode parseTopLevel() {
        if (current().sym == Sym.FINAL || current().sym == Sym.TYPE_ID) {
            // Var global
            return (TopLevelNode) parseVarDecl(false);
        } else if (current().sym == Sym.COLL) {
            return parseCollDecl();
        } else if (current().sym == Sym.DEF) {
            return parseFunDef();
        } else {
            Symbol c = current();
            throw new RuntimeException("Unexpected token at top level: " + c + " line " + c.line);
        }
    }

    // Types

    private TypeNode parseType() {
        Symbol t = expect(Sym.TYPE_ID);
        String name = (String) t.value;
        boolean isArray = false;
        // On ne consomme pas ici les [] (ils sont gérés à part pour VarDecl/Param/Field),
        // mais pour simplifier tu peux aussi les gérer ici.
        return new TypeNode(name, isArray);
    }

    //VarDecl (top-level ou locale)

    private VarDeclNode parseVarDecl(boolean asStmt) {
        boolean isFinal = false;
        if (match(Sym.FINAL)) {
            isFinal = true;
        }
        TypeNode type = parseType();
        // type[] ?
        if (match(Sym.LBRACK)) {
            expect(Sym.RBRACK);
            type = new TypeNode(type.name, true);
        }
        Symbol idSym = expect(Sym.ID);
        IdentifierNode id = new IdentifierNode((String) idSym.value);

        ExprNode init = null;
        if (match(Sym.ASSIGN)) {
            // Cas spécial: INT[] c = INT ARRAY[5];
            if (current().sym == Sym.TYPE_ID && lookahead(1).sym == Sym.ARRAY) {
                // on laisse parseExpr gérer ça (NewArrayNode)
            }
            init = parseExpr();
        }

        expect(Sym.SEMI);
        return new VarDeclNode(isFinal, type, id, init);
    }

    // Collections

    private CollDeclNode parseCollDecl() {
        expect(Sym.COLL);
        Symbol nameSym = expect(Sym.TYPE_ID);
        CollDeclNode coll = new CollDeclNode((String) nameSym.value);
        expect(Sym.LBRACE);
        while (current().sym != Sym.RBRACE) {
            // FieldDecl: Type ["[" "]"] ID ";"
            TypeNode t = parseType();
            if (match(Sym.LBRACK)) {
                expect(Sym.RBRACK);
                t = new TypeNode(t.name, true);
            }
            Symbol idSym = expect(Sym.ID);
            expect(Sym.SEMI);
            coll.fields.add(new FieldDeclNode(t, new IdentifierNode((String) idSym.value)));
        }
        expect(Sym.RBRACE);
        return coll;
    }

    // Fonctions

    private FunDefNode parseFunDef() {
        expect(Sym.DEF);
        TypeNode returnType = null;
        // def main() ...  (pas de type)
        if (current().sym == Sym.TYPE_ID) {
            returnType = parseType();
        }
        Symbol nameSym = expect(Sym.ID);
        String name = (String) nameSym.value;

        expect(Sym.LPAREN);
        List<ParamNode> params = new ArrayList<>();
        if (current().sym != Sym.RPAREN) {
            params.add(parseParam());
            while (match(Sym.COMMA)) {
                params.add(parseParam());
            }
        }
        expect(Sym.RPAREN);
        BlockNode body = parseBlock();
        return new FunDefNode(returnType, name, params, body);
    }

    private ParamNode parseParam() {
        TypeNode type = parseType();
        if (match(Sym.LBRACK)) {
            expect(Sym.RBRACK);
            type = new TypeNode(type.name, true);
        }
        Symbol idSym = expect(Sym.ID);
        return new ParamNode(type, new IdentifierNode((String) idSym.value));
    }

    // Bloc et instructions

    private BlockNode parseBlock() {
        BlockNode block = new BlockNode();
        expect(Sym.LBRACE);
        while (current().sym != Sym.RBRACE) {
            block.statements.add(parseStmt());
        }
        expect(Sym.RBRACE);
        return block;
    }

    private StmtNode parseStmt() {
        switch (current().sym) {
            case Sym.FINAL:
            case Sym.TYPE_ID:
                // Déclaration locale
                return new VarDeclStmtNode(parseVarDecl(true));
            case Sym.IF:
                return parseIf();
            case Sym.WHILE:
                return parseWhile();
            case Sym.FOR:
                return parseFor();
            case Sym.RETURN:
                return parseReturn();
            case Sym.LBRACE:
                return parseBlock();
            default:
                // instruction d’expression (appel de fonction, assign, etc.)
                ExprNode e = parseExpr();
                expect(Sym.SEMI);
                return new ExprStmtNode(e);
        }
    }

    private IfNode parseIf() {
        expect(Sym.IF);
        expect(Sym.LPAREN);
        ExprNode cond = parseExpr();
        expect(Sym.RPAREN);
        BlockNode thenBlock = parseBlock();
        BlockNode elseBlock = null;
        if (match(Sym.ELSE)) {
            elseBlock = parseBlock();
        }
        return new IfNode(cond, thenBlock, elseBlock);
    }

    private WhileNode parseWhile() {
        expect(Sym.WHILE);
        expect(Sym.LPAREN);
        ExprNode cond = parseExpr();
        expect(Sym.RPAREN);
        BlockNode body = parseBlock();
        return new WhileNode(cond, body);
    }

    private ForNode parseFor() {
        expect(Sym.FOR);
        expect(Sym.LPAREN);
        TypeNode t = parseType();
        Symbol idSym = expect(Sym.ID);
        IdentifierNode id = new IdentifierNode((String) idSym.value);
        expect(Sym.SEMI);
        ExprNode start = parseExpr();
        expect(Sym.ARROW);
        ExprNode end = parseExpr();
        expect(Sym.SEMI);
        ExprNode step = parseExpr();
        expect(Sym.RPAREN);
        BlockNode body = parseBlock();
        return new ForNode(t, id, start, end, step, body);
    }

    private ReturnNode parseReturn() {
        expect(Sym.RETURN);
        ExprNode expr = null;
        if (current().sym != Sym.SEMI) {
            expr = parseExpr();
        }
        expect(Sym.SEMI);
        return new ReturnNode(expr);
    }

    // Expressions (précédence)

    private ExprNode parseExpr() {
        // Support de l'affectation (=) comme opérateur de plus faible priorité (right-associative)
        return parseAssign();
    }

    // Ajout : gestion des assignations a = b = c
    private ExprNode parseAssign() {
        ExprNode left = parseOr();
        if (current().sym == Sym.ASSIGN) {
            // consommer '='
            Symbol op = current(); pos++;
            ExprNode right = parseAssign(); // right-associatif
            return new BinaryOpNode((String)op.value, left, right);
        }
        return left;
    }

    private ExprNode parseOr() {
        ExprNode left = parseAnd();
        while (current().sym == Sym.OR) {
            Symbol op = current(); pos++;
            ExprNode right = parseAnd();
            left = new BinaryOpNode((String) op.value, left, right);
        }
        return left;
    }

    private ExprNode parseAnd() {
        ExprNode left = parseEquality();
        while (current().sym == Sym.AND) {
            Symbol op = current(); pos++;
            ExprNode right = parseEquality();
            left = new BinaryOpNode((String) op.value, left, right);
        }
        return left;
    }

    private ExprNode parseEquality() {
        ExprNode left = parseRel();
        while (current().sym == Sym.EQ || current().sym == Sym.NEQ) {
            Symbol op = current(); pos++;
            ExprNode right = parseRel();
            left = new BinaryOpNode((String) op.value, left, right);
        }
        return left;
    }

    private ExprNode parseRel() {
        ExprNode left = parseAdd();
        while (current().sym == Sym.LT || current().sym == Sym.LE
                || current().sym == Sym.GT || current().sym == Sym.GE) {
            Symbol op = current(); pos++;
            ExprNode right = parseAdd();
            left = new BinaryOpNode((String) op.value, left, right);
        }
        return left;
    }

    private ExprNode parseAdd() {
        ExprNode left = parseMul();
        while (current().sym == Sym.PLUS || current().sym == Sym.MINUS) {
            Symbol op = current(); pos++;
            ExprNode right = parseMul();
            left = new BinaryOpNode((String) op.value, left, right);
        }
        return left;
    }

    private ExprNode parseMul() {
        ExprNode left = parseUnary();
        while (current().sym == Sym.STAR || current().sym == Sym.SLASH
                || current().sym == Sym.PERCENT) {
            Symbol op = current(); pos++;
            ExprNode right = parseUnary();
            left = new BinaryOpNode((String) op.value, left, right);
        }
        return left;
    }

    private ExprNode parseUnary() {
        if (current().sym == Sym.MINUS || current().sym == Sym.NOT) {
            Symbol op = current(); pos++;
            ExprNode expr = parseUnary();
            return new UnaryOpNode((String) op.value, expr);
        }
        return parsePostfix();
    }

    // Postfix : appels, index, champ
    private ExprNode parsePostfix() {
        ExprNode base = parsePrimary();
        boolean loop = true;
        while (loop) {
            if (match(Sym.LBRACK)) {
                ExprNode index = parseExpr();
                expect(Sym.RBRACK);
                base = new ArrayAccessNode(base, index);
            } else if (match(Sym.DOT)) {
                Symbol idSym = expect(Sym.ID);
                base = new FieldAccessNode(base, (String) idSym.value);
            } else if (match(Sym.LPAREN)) {
                List<ExprNode> args = new ArrayList<>();
                if (current().sym != Sym.RPAREN) {
                    args.add(parseExpr());
                    while (match(Sym.COMMA)) {
                        args.add(parseExpr());
                    }
                }
                expect(Sym.RPAREN);
                base = new CallNode(base, args);
            } else {
                loop = false;
            }
        }
        return base;
    }

    private ExprNode parsePrimary() {
        Symbol c = current();
        switch (c.sym) {
            case Sym.INT_LIT:
                pos++;
                return new IntLiteralNode((Integer) c.value);
            case Sym.FLOAT_LIT:
                pos++;
                return new FloatLiteralNode((Float) c.value);
            case Sym.STRING_LIT:
                pos++;
                return new StringLiteralNode((String) c.value);
            case Sym.TRUE_LIT:
                pos++;
                return new BoolLiteralNode(true);
            case Sym.FALSE_LIT:
                pos++;
                return new BoolLiteralNode(false);
            case Sym.ID:
            case Sym.READ_INT:
            case Sym.READ_FLOAT:
            case Sym.READ_STRING:
            case Sym.PRINT:
            case Sym.PRINT_INT:
            case Sym.PRINT_FLOAT:
            case Sym.PRINTLN: {
                // ident / fonction IO -> on commence par une variable, puis parsePostfix gérera l’appel
                pos++;
                return new VarRefNode((String) c.value);
            }
            case Sym.TYPE_ID:
                // Deux cas qu’on veut supporter ici:
                //  1) INT ARRAY[5]        -> NewArrayNode
                //  2) Point( ... )        -> constructeur (CallNode sur VarRefNode)
                if (lookahead(1).sym == Sym.ARRAY) {
                    String elemType = (String) c.value;
                    pos++; // TYPE_ID
                    expect(Sym.ARRAY);
                    expect(Sym.LBRACK);
                    ExprNode size = parseExpr();
                    expect(Sym.RBRACK);
                    return new NewArrayNode(elemType, size);
                } else {
                    // On considère TYPE_ID comme un identifiant de fonction/constructeur
                    pos++;
                    return new VarRefNode((String) c.value);
                }
            case Sym.LPAREN:
                pos++;
                ExprNode e = parseExpr();
                expect(Sym.RPAREN);
                return e;
            default:
                throw new RuntimeException("Unexpected token in expression: " + c);
        }
    }
}
