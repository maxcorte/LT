package compiler.Parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Lexer.Sym;
import compiler.Lexer.Symbol;

import compiler.SemanticAnalysis.SemanticException;
import java.util.ArrayList;
import java.util.List;
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

    // Program

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

        Symbol nameSym = null;
        try {
            nameSym = expect(Sym.TYPE_ID);
        }catch(Exception e){
            throw new SemanticException(
                "CollectionError: collection name '" + nameSym +
                    "' must start with an uppercase letter");
        }

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

        TypeNode varType = null;
        IdentifierNode varId;

        // Deux cas :
        // 1) Type ID ;(déclaration dans le for)
        // 2) ID ; (variable déjà déclarée)
        if (current().sym == Sym.TYPE_ID) {
            varType = parseType();
            Symbol idSym = expect(Sym.ID);
            varId = new IdentifierNode((String) idSym.value);
        } else {
            Symbol idSym = expect(Sym.ID);
            varId = new IdentifierNode((String) idSym.value);
        }

        expect(Sym.SEMI);
        ExprNode start = parseExpr();
        expect(Sym.ARROW);
        ExprNode end = parseExpr();
        expect(Sym.SEMI);
        ExprNode step = parseExpr();
        expect(Sym.RPAREN);

        BlockNode body = parseBlock();
        return new ForNode(varType, varId, start, end, step, body);
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
        }else if (current().sym == Sym.PLUS_EQ) {
            pos++;
            ExprNode right = parseAssign();
            // x+=y  --> x = x + y
            return new BinaryOpNode("=",left,new BinaryOpNode("+",left,right));
        }else if (current().sym == Sym.MINUS_EQ) {
            pos++;
            ExprNode right = parseAssign();
            // x-=y  --> x = x - y
            return new BinaryOpNode("=",left,new BinaryOpNode("-",left,right));
        } else if (current().sym == Sym.MULTI_EQ) {
            pos++;
            ExprNode right = parseAssign();
            // x*=y  --> x = x * y
            return new BinaryOpNode("=",left,new BinaryOpNode("*",left,right));

        }else if (current().sym == Sym.SLASH_EQ) {
            pos++;
            ExprNode right = parseAssign();
            // x/=y  --> x = x / y
            return new BinaryOpNode("=",left,new BinaryOpNode("/",left,right));
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
            } else if (match(Sym.PLUS_ONE)) {
                base = new UnaryPlusOneNode(base);
            }else if (match(Sym.MINUS_ONE)) {
                base = new UnaryMinusOneNode(base);
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
                //  1) INT ARRAY[5] -> NewArrayNode
                //  2) Point( ... ) -> constructeur (CallNode sur VarRefNode)
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

