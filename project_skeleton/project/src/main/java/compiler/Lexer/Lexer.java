package compiler.Lexer;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

public class Lexer {

    private Reader input;
    private int currentChar = -1;
    private int line = 1;
    private int column = 0;
    private boolean eof = false;

    public static class Sym {
        public static final int EOF = 0;
        
        // Mots-clés
        public static final int FINAL = 1;
        public static final int COLL = 2;
        public static final int DEF = 3;
        public static final int FOR = 4;
        public static final int WHILE = 5;
        public static final int IF = 6;
        public static final int ELSE = 7;
        public static final int RETURN = 8;
        public static final int NOT = 9;
        public static final int ARRAY = 10;
        
        // Identifiants
        public static final int ID = 11;        // Identifiant normal (commence par minuscule ou _)
        public static final int TYPE_ID = 12;   // Type ou collection (commence par majuscule)
        
        // Littéraux
        public static final int INT_LIT = 13;
        public static final int FLOAT_LIT = 14;
        public static final int STRING_LIT = 15;
        public static final int TRUE_LIT = 16;
        public static final int FALSE_LIT = 17;
        
        // Opérateurs et délimiteurs
        public static final int ASSIGN = 20;   // =
        public static final int PLUS = 21;     // +
        public static final int MINUS = 22;    // -
        public static final int STAR = 23;     // *
        public static final int SLASH = 24;    // /
        public static final int PERCENT = 25;  // %
        public static final int EQ = 26;       // ==
        public static final int NEQ = 27;      // =/=
        public static final int LT = 28;       // <
        public static final int LE = 29;       // <=
        public static final int GT = 30;       // >
        public static final int GE = 31;       // >=
        public static final int AND = 32;      // &&
        public static final int OR = 33;       // ||
        public static final int ARROW = 34;    // ->
        public static final int LPAREN = 35;   // (
        public static final int RPAREN = 36;   // )
        public static final int LBRACE = 37;   // {
        public static final int RBRACE = 38;   // }
        public static final int LBRACK = 39;   // [
        public static final int RBRACK = 40;   // ]
        public static final int SEMI = 41;     // ;
        public static final int COMMA = 42;    // ,
        public static final int DOT = 43;      // .


        // Function IO

        public static final int READ_INT =44;
        public static final int READ_FLOAT=45;
        public static final int READ_STRING =46;
        public static final int PRINT_INT =47;
        public static final int PRINT_FLOAT=48;
        public static final int PRINT=49;
        public static final int PRINTLN=50;

        //
        public static final int PLUS_EQ = 51; // +=
        public static final int MINUS_EQ =52; // -=
        public static final int MULTI_EQ =53; // *=
        public static final int SLASH_EQ = 54; // /=
        public static final int PLUS_ONE = 55; // ++
        public static final int MINUS_ONE = 56; // --

    }
    
    // Ensemble des mots-clés du langage
    private static final Set<String> keywords = new HashSet<>();
    // Ensemble des types de base
    private static final Set<String> baseTypes = new HashSet<>();
    
    static {
        keywords.add("final");
        keywords.add("coll");
        keywords.add("def");
        keywords.add("for");
        keywords.add("while");
        keywords.add("if");
        keywords.add("else");
        keywords.add("return");
        keywords.add("not");
        keywords.add("ARRAY");
        keywords.add("true");
        keywords.add("false");
        
        baseTypes.add("INT");
        baseTypes.add("FLOAT");
        baseTypes.add("BOOL");
        baseTypes.add("STRING");
    }
    
    public Lexer(Reader input) {
        this.input = input;
        nextChar(); // Initialiser le premier caractère
    }
    
    public Symbol getNextSymbol() {
        skipWhitespace();
        
        if (eof) {
            return new Symbol(Sym.EOF, null, line, column);
        }
        
        int startLine = line;
        int startColumn = column;
        
        // Commentaires
        if (currentChar == '#') {
            skipComment();
            return getNextSymbol(); 
        }
        
        // Chaînes de caractères
        if (currentChar == '"') {
            return readString(startLine, startColumn);
        }
        
        // Identifiants et mots-clés
        if (isLetter(currentChar) || currentChar == '_') {
            return readIdentifier(startLine, startColumn);
        }
        
        // Nombres (y compris .234 pour floats)
        if (isDigit(currentChar)) {
            return readNumber(startLine, startColumn);
        }
        
        // Cas spécial: . peut être DOT ou début de float (.234)
        if (currentChar == '.') {
            // Regarder le caractère suivant sans le consommer
            int saved = currentChar;
            nextChar();
            if (isDigit(currentChar)) {
                // C'est un float comme .234
                // Revenir en arrière conceptuellement
                return readFloatStartingWithDot(startLine, startColumn);
            } else {
                // C'est juste un point
                return new Symbol(Sym.DOT, ".", startLine, startColumn);
            }
        }
        
        // Opérateurs et délimiteurs
        switch (currentChar) {
            case '(':
                nextChar();
                return new Symbol(Sym.LPAREN, "(", startLine, startColumn);
            
            case ')':
                nextChar();
                return new Symbol(Sym.RPAREN, ")", startLine, startColumn);
            
            case '{':
                nextChar();
                return new Symbol(Sym.LBRACE, "{", startLine, startColumn);
            
            case '}':
                nextChar();
                return new Symbol(Sym.RBRACE, "}", startLine, startColumn);
            
            case '[':
                nextChar();
                return new Symbol(Sym.LBRACK, "[", startLine, startColumn);
            
            case ']':
                nextChar();
                return new Symbol(Sym.RBRACK, "]", startLine, startColumn);
            
            case ';':
                nextChar();
                return new Symbol(Sym.SEMI, ";", startLine, startColumn);
            
            case ',':
                nextChar();
                return new Symbol(Sym.COMMA, ",", startLine, startColumn);
            
            case '+':
                nextChar();
                if (currentChar == '='){
                    nextChar();
                    return new Symbol(Sym.PLUS_EQ,"+=",startLine,startColumn);
                }
                if (currentChar == '+'){
                    nextChar();
                    return new Symbol(Sym.PLUS_ONE,"++",startLine,startColumn);
                }
                return new Symbol(Sym.PLUS, "+", startLine, startColumn);
            
            case '*':
                nextChar();
                if (currentChar == '='){
                    nextChar();
                    return new Symbol(Sym.MULTI_EQ,"*=",startLine,startColumn);
                }
                return new Symbol(Sym.STAR, "*", startLine, startColumn);
            
            case '/':
                nextChar();
                if (currentChar == '='){
                    nextChar();
                    return new Symbol(Sym.SLASH_EQ,"/=",startLine,startColumn);
                }
                return new Symbol(Sym.SLASH, "/", startLine, startColumn);
            
            case '%':
                nextChar();
                return new Symbol(Sym.PERCENT, "%", startLine, startColumn);
            
            case '=':
                nextChar();
                if (currentChar == '/') {
                    nextChar();
                    if (currentChar == '=') {
                        nextChar();
                        return new Symbol(Sym.NEQ, "=/=", startLine, startColumn);
                    }
                    // Erreur: =/ sans =
                    throw new RuntimeException("Lexer error: unexpected =/");
                } else if (currentChar == '=') {
                    nextChar();
                    return new Symbol(Sym.EQ, "==", startLine, startColumn);
                }
                return new Symbol(Sym.ASSIGN, "=", startLine, startColumn);
            
            case '<':
                nextChar();
                if (currentChar == '=') {
                    nextChar();
                    return new Symbol(Sym.LE, "<=", startLine, startColumn);
                }
                return new Symbol(Sym.LT, "<", startLine, startColumn);
            
            case '>':
                nextChar();
                if (currentChar == '=') {
                    nextChar();
                    return new Symbol(Sym.GE, ">=", startLine, startColumn);
                }
                return new Symbol(Sym.GT, ">", startLine, startColumn);
            
            case '&':
                nextChar();
                if (currentChar == '&') {
                    nextChar();
                    return new Symbol(Sym.AND, "&&", startLine, startColumn);
                }
                throw new RuntimeException("Lexer error: expected && at line " + startLine);
            
            case '|':
                nextChar();
                if (currentChar == '|') {
                    nextChar();
                    return new Symbol(Sym.OR, "||", startLine, startColumn);
                }
                throw new RuntimeException("Lexer error: expected || at line " + startLine);
            
            case '-':
                nextChar();
                if (currentChar == '>') {
                    nextChar();
                    return new Symbol(Sym.ARROW, "->", startLine, startColumn);
                }
                if (currentChar == '='){
                    nextChar();
                    return new Symbol(Sym.MINUS_EQ,"-=",startLine,startColumn);
                }
                if (currentChar == '-'){
                    nextChar();
                    return new Symbol(Sym.MINUS_ONE,"--",startLine,startColumn);
                }
                return new Symbol(Sym.MINUS, "-", startLine, startColumn);


            default:
                throw new RuntimeException("Lexer error: unexpected character '" + 
                                         (char)currentChar + "' at line " + line + ", column " + column);
        }
    }
    
     private void nextChar() {
        try {
            currentChar = input.read();
            if (currentChar == -1) {
                eof = true;
            } else if (currentChar == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        } catch (IOException e) {
            eof = true;
            currentChar = -1;
        }
    }
    
  
    // Ignore les espaces, tabulations et retours à la ligne.

    private void skipWhitespace() {
        while (!eof && (currentChar == ' ' || currentChar == '\t' || 
                       currentChar == '\n' || currentChar == '\r')) {
            nextChar();
        }
    }
    
   
     // Ignore un commentaire (# jusqu'à fin de ligne).
     
    private void skipComment() {
        while (!eof && currentChar != '\n') {
            nextChar();
        }
        if (currentChar == '\n') {
            nextChar();
        }
    }
    
    // Lit une chaîne de caractères entre guillemets.
  
    private Symbol readString(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        nextChar();
        
        while (!eof && currentChar != '"' && currentChar != '\n') {
            sb.append((char) currentChar);
            nextChar();
        }
        
        if (currentChar != '"') {
            throw new RuntimeException("Lexer error: unterminated string at line " + startLine);
        }
        
        nextChar();
        return new Symbol(Sym.STRING_LIT, sb.toString(), startLine, startColumn);
    }
    
    /*
     Lit un identifiant ou un mot-clé.
     Identifiants: [a-z_][a-zA-Z0-9_]*
     Collections/Types: [A-Z][a-zA-Z0-9_]*
     */
    private Symbol readIdentifier(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        
        while (!eof && (isLetter(currentChar) || isDigit(currentChar) || currentChar == '_')) {
            sb.append((char) currentChar);
            nextChar();
        }
        
        String identifier = sb.toString();
        
        // Vérifier si c'est un mot-clé
        if (keywords.contains(identifier)) {
            switch (identifier) {
                case "final": return new Symbol(Sym.FINAL, identifier, startLine, startColumn);
                case "coll": return new Symbol(Sym.COLL, identifier, startLine, startColumn);
                case "def": return new Symbol(Sym.DEF, identifier, startLine, startColumn);
                case "for": return new Symbol(Sym.FOR, identifier, startLine, startColumn);
                case "while": return new Symbol(Sym.WHILE, identifier, startLine, startColumn);
                case "if": return new Symbol(Sym.IF, identifier, startLine, startColumn);
                case "else": return new Symbol(Sym.ELSE, identifier, startLine, startColumn);
                case "return": return new Symbol(Sym.RETURN, identifier, startLine, startColumn);
                case "not": return new Symbol(Sym.NOT, identifier, startLine, startColumn);
                case "ARRAY": return new Symbol(Sym.ARRAY, identifier, startLine, startColumn);
                case "true": return new Symbol(Sym.TRUE_LIT, true, startLine, startColumn);
                case "false": return new Symbol(Sym.FALSE_LIT, false, startLine, startColumn);
            }
        }

        // verifier si c'est une fonction IO
        if (identifier.contains("read_INT")){
            return new Symbol(Sym.READ_INT,identifier,startLine,startColumn);
        }
        if (identifier.contains("read_FLOAT")){
            return new Symbol(Sym.READ_FLOAT,identifier,startLine,startColumn);
        }
        if (identifier.contains("read_STRING")){
            return new Symbol(Sym.READ_STRING,identifier,startLine,startColumn);
        }
        if (identifier.contains("print_INT")){
            return new Symbol(Sym.PRINT_INT,identifier,startLine,startColumn);
        }
        if (identifier.contains("print_FLOAT")){
            return new Symbol(Sym.PRINT_FLOAT,identifier,startLine,startColumn);
        }
        if (identifier.contains("println")){
            return new Symbol(Sym.PRINTLN,identifier,startLine,startColumn);
        }
        if (identifier.contains("print")){
            return new Symbol(Sym.PRINT,identifier,startLine,startColumn);
        }

        // type de base
        if (Character.isUpperCase(identifier.charAt(0)) || baseTypes.contains(identifier)) {
            return new Symbol(Sym.TYPE_ID, identifier, startLine, startColumn);
        }
        
        // Sinon, identifiant normal
        return new Symbol(Sym.ID, identifier, startLine, startColumn);
    }
    
    /*
     Lit un nombre (INT ou FLOAT).
     Gère les cas: 123, 3.14, 00342 → 342
     */
    private Symbol readNumber(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        
        // Lire les chiffres avant le point
        while (!eof && isDigit(currentChar)) {
            sb.append((char) currentChar);
            nextChar();
        }
        
        // Vérifier s'il y a un point décimal
        if (currentChar == '.') {
            sb.append('.');
            nextChar();
            
            // Lire les chiffres après le point
            while (!eof && isDigit(currentChar)) {
                sb.append((char) currentChar);
                nextChar();
            }
            
            // C'est un FLOAT
            String floatStr = sb.toString();
            try {
                float value = Float.parseFloat(floatStr);
                return new Symbol(Sym.FLOAT_LIT, value, startLine, startColumn);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Lexer error: invalid float " + floatStr + " at line " + startLine);
            }
        } else {
            // C'est un INT
            String intStr = sb.toString();
            
            // Supprimer les zéros de tête: 00342 → 342
            int value = Integer.parseInt(intStr);
            return new Symbol(Sym.INT_LIT, value, startLine, startColumn);
        }
    }
    
    /*
     Lit un float commençant par un point (.234 → 0.234).
     Appelé quand on a déjà détecté . suivi d'un chiffre.
     */
    private Symbol readFloatStartingWithDot(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder("0.");
        
        // currentChar est déjà sur le premier chiffre après le point
        while (!eof && isDigit(currentChar)) {
            sb.append((char) currentChar);
            nextChar();
        }
        
        String floatStr = sb.toString();
        try {
            float value = Float.parseFloat(floatStr);
            return new Symbol(Sym.FLOAT_LIT, value, startLine, startColumn);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Lexer error: invalid float " + floatStr + " at line " + startLine);
        }
    }
    
    //Vérifie si un caractère est une lettre (a-z, A-Z).
     
    private boolean isLetter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    
    //Vérifie si un caractère est un chiffre (0-9).
  
    private boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }
    




    //test



    public static void main(String[] args) {
        try {
            Lexer lexer = new Lexer(new java.io.FileReader("src/main/java/compiler/Lexer/code_example.txt"));
            Symbol sym;
            
            System.out.println("analyse\n");
            
            while ((sym = lexer.getNextSymbol()).sym != Sym.EOF) {
                System.out.println(sym);
            }
            
            System.out.println("\n fin");
            
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

