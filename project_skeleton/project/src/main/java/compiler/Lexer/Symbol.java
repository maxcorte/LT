package compiler.Lexer;

import compiler.Lexer.Lexer.Sym;

public class Symbol {
    public int sym;        // Type du symbole (constante Sym.*)
    public Object value;   // Valeur du symbole (String, Integer, Float, Boolean...)
    public int line;       // Numéro de ligne
    public int column;     // Numéro de colonne
    
    /**
     * Constructeur d'un symbole.
     * @param sym Type du symbole
     * @param value Valeur associée (lexème ou valeur convertie)
     * @param line Ligne dans le code source
     * @param column Colonne dans le code source
     */
    public Symbol(int sym, Object value, int line, int column) {
        this.sym = sym;
        this.value = value;
        this.line = line;
        this.column = column;
    }
    
    @Override
    public String toString() {
        String symName = getSymbolName(sym);
        return String.format("< %s, %s>",
                            symName, value, line, column);
    }
    
    /**
     * Retourne le nom du symbole pour debug
     */
    private String getSymbolName(int sym) {
        switch(sym) {
            case Lexer.Sym.EOF: return "EOF";
            case Lexer.Sym.FINAL: return "FINAL";
            case Lexer.Sym.COLL: return "COLL";
            case Lexer.Sym.DEF: return "DEF";
            case Lexer.Sym.FOR: return "FOR";
            case Lexer.Sym.WHILE: return "WHILE";
            case Lexer.Sym.IF: return "IF";
            case Lexer.Sym.ELSE: return "ELSE";
            case Lexer.Sym.RETURN: return "RETURN";
            case Lexer.Sym.NOT: return "NOT";
            case Lexer.Sym.ARRAY: return "ARRAY";
            case Lexer.Sym.ID: return "ID";
            case Lexer.Sym.TYPE_ID: return "TYPE_ID";
            case Lexer.Sym.INT_LIT: return "INT_LIT";
            case Lexer.Sym.FLOAT_LIT: return "FLOAT_LIT";
            case Lexer.Sym.STRING_LIT: return "STRING_LIT";
            case Lexer.Sym.TRUE_LIT: return "TRUE";
            case Lexer.Sym.FALSE_LIT: return "FALSE";
            case Lexer.Sym.ASSIGN: return "ASSIGN";
            case Lexer.Sym.PLUS: return "PLUS";
            case Lexer.Sym.MINUS: return "MINUS";
            case Lexer.Sym.STAR: return "STAR";
            case Lexer.Sym.SLASH: return "SLASH";
            case Lexer.Sym.PERCENT: return "PERCENT";
            case Lexer.Sym.EQ: return "EQ";
            case Lexer.Sym.NEQ: return "NEQ";
            case Lexer.Sym.LT: return "LT";
            case Lexer.Sym.LE: return "LE";
            case Lexer.Sym.GT: return "GT";
            case Lexer.Sym.GE: return "GE";
            case Lexer.Sym.AND: return "AND";
            case Lexer.Sym.OR: return "OR";
            case Lexer.Sym.ARROW: return "ARROW";
            case Lexer.Sym.LPAREN: return "LPAREN";
            case Lexer.Sym.RPAREN: return "RPAREN";
            case Lexer.Sym.LBRACE: return "LBRACE";
            case Lexer.Sym.RBRACE: return "RBRACE";
            case Lexer.Sym.LBRACK: return "LBRACK";
            case Lexer.Sym.RBRACK: return "RBRACK";
            case Lexer.Sym.SEMI: return "SEMI";
            case Lexer.Sym.COMMA: return "COMMA";
            case Lexer.Sym.DOT: return "DOT";
            case Sym.READ_INT: return "READ_INT";
            case Sym.READ_FLOAT: return "READ_FLOAT";
            case Sym.READ_STRING: return "READ_STRING";
            case Sym.PRINT_INT: return "PRINT_INT";
            case Sym.PRINT_FLOAT: return "PRINT_FLOAT";
            case Sym.PRINT: return "PRINT";
            case Sym.PRINTLN: return "PRINTLN";
            default: return "UNKNOWN(" + sym + ")";
        }
    }
}



