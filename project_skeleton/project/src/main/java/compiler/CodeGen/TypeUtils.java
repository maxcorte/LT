package compiler.CodeGen;

import org.objectweb.asm.Opcodes;

final class TypeUtils {

    static boolean isInt(String t)    { return "INT".equals(t);    }
    static boolean isFloat(String t)  { return "FLOAT".equals(t);  }
    static boolean isBool(String t)   { return "BOOL".equals(t);   }
    static boolean isString(String t) { return "STRING".equals(t); }

    // True for any object/reference type
    static boolean isReference(String t) {
        if (t == null) return false;
        if (isInt(t) || isFloat(t) || isBool(t)) return false;
        return true;
    }

    static boolean isVoidStackType(String t) {
        return t == null || "void".equals(t);
    }

    static String typeDescriptor(String baseType, boolean isArray) {
        String base;
        if      (isInt(baseType))    base = "I";
        else if (isFloat(baseType))  base = "F";
        else if (isBool(baseType))   base = "Z";
        else if (isString(baseType)) base = "Ljava/lang/String;";
        else                         base = "L" + baseType + ";";
        return isArray ? "[" + base : base;
    }

    // Same, but takes a "full type" string like "INT", "INT[]", "Point"
    static String descForFullType(String fullType) {
        boolean isArr = fullType.endsWith("[]");
        String base = isArr ? fullType.substring(0, fullType.length() - 2) : fullType;
        return typeDescriptor(base, isArr);
    }

    static int loadOpcode(String baseType, boolean isArr) {
        if (isArr) return Opcodes.ALOAD;
        if (isInt(baseType) || isBool(baseType)) return Opcodes.ILOAD;
        if (isFloat(baseType))                   return Opcodes.FLOAD;
        return Opcodes.ALOAD;
    }

    private TypeUtils() {}
}
