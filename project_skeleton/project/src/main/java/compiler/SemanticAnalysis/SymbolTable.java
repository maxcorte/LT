package compiler.SemanticAnalysis;

import java.util.*;

public class SymbolTable {

    private static final Set<String> RESERVED_TYPES =
            Set.of("INT", "FLOAT", "BOOL", "STRING");

    private static final Set<String> KEYWORDS =
            Set.of("final", "coll", "def", "for", "while", "if", "else",
                    "return", "not", "ARRAY", "true", "false");

    // symboles

    public static class VarSymbol {
        public final String  name;
        public final String  type;
        public final boolean isArray;
        public final boolean isFinal;

        public VarSymbol(String name, String type, boolean isArray, boolean isFinal) {
            this.name    = name;
            this.type    = type;
            this.isArray = isArray;
            this.isFinal = isFinal;
        }

        public String fullType() { return isArray ? type + "[]" : type; }
    }

    public static class FunSymbol {
        public final String          name;
        public final String          returnType;
        public final boolean         returnIsArray;
        public final List<VarSymbol> params;
        public final boolean         isBuiltin;

        public FunSymbol(String name, String returnType, boolean returnIsArray,
                         List<VarSymbol> params, boolean isBuiltin) {
            this.name          = name;
            this.returnType    = returnType;
            this.returnIsArray = returnIsArray;
            this.params        = params;
            this.isBuiltin     = isBuiltin;
        }
    }

    public static class CollSymbol {
        public final String                 name;
        public final Map<String, VarSymbol> fields = new LinkedHashMap<>();

        public CollSymbol(String name) { this.name = name; }
    }

    // etat

    private final Deque<Map<String, VarSymbol>> scopes      = new ArrayDeque<>();
    private final Map<String, FunSymbol>         functions   = new HashMap<>();
    private final Map<String, CollSymbol>         collections = new HashMap<>();

    // scopes

    public void pushScope() { scopes.push(new LinkedHashMap<>()); }

    public void popScope() {
        if (scopes.isEmpty()) throw new SemanticException("Internal: no scope to pop");
        scopes.pop();
    }

    // variables

    public void declareVar(VarSymbol sym) {
        if (scopes.isEmpty()) throw new SemanticException("Internal: no active scope");
        Map<String, VarSymbol> current = scopes.peek();
        if (current.containsKey(sym.name)) {
            throw new SemanticException(
                    "ScopeError: variable '" + sym.name + "' is already declared in this scope");
        }
        current.put(sym.name, sym);
    }

    public VarSymbol lookupVar(String name) {
        for (Map<String, VarSymbol> scope : scopes) {
            VarSymbol s = scope.get(name);
            if (s != null) return s;
        }
        return null;
    }

    // fonctions

    public void declareFunction(FunSymbol sym) {
        if (functions.containsKey(sym.name)) {
            throw new SemanticException(
                    "ScopeError: function '" + sym.name + "' is already declared");
        }
        functions.put(sym.name, sym);
    }

    public FunSymbol lookupFunction(String name) { return functions.get(name); }

    // coll

    public void declareCollection(CollSymbol sym) {
        String name = sym.name;

        if (name.isEmpty() || !Character.isUpperCase(name.charAt(0))) {
            throw new SemanticException(
                    "CollectionError: collection name '" + name +
                            "' must start with an uppercase letter");
        }

        if (RESERVED_TYPES.contains(name)) {
            throw new SemanticException(
                    "CollectionError: '" + name +
                            "' is a reserved primitive type and cannot be used as a collection name");
        }

        if (KEYWORDS.contains(name)) {
            throw new SemanticException(
                    "CollectionError: '" + name +
                            "' is a reserved keyword and cannot be used as a collection name");
        }

        if (collections.containsKey(name)) {
            throw new SemanticException(
                    "CollectionError: collection '" + name + "' is already declared");
        }

        collections.put(name, sym);
    }

    public CollSymbol lookupCollection(String name) { return collections.get(name); }

    public boolean isCollection(String name) { return collections.containsKey(name); }
}