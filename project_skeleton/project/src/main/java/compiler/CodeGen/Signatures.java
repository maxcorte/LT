package compiler.CodeGen;

import java.util.List;

final class Signatures {

    // local
    static final class VarInfo {
        final String type;
        final int    slot;
        VarInfo(String type, int slot) {
            this.type = type;
            this.slot = slot;
        }
    }

    // global
    static final class GlobalInfo {
        final String  type;
        final boolean isFinal;
        GlobalInfo(String type, boolean isFinal) {
            this.type = type;
            this.isFinal = isFinal;
        }
    }

    // A user-defined function's signature
    static final class FunSig {
        final String       name;
        final String       returnType;       // null for void
        final boolean      returnIsArray;
        final List<String> paramTypes;       // full types ("INT", "INT[]", "Point", ...)
        final String       descriptor;       // JVM descriptor, e.g. "(I)I"
        FunSig(String name, String returnType, boolean returnIsArray,
               List<String> paramTypes, String descriptor) {
            this.name = name;
            this.returnType = returnType;
            this.returnIsArray = returnIsArray;
            this.paramTypes = paramTypes;
            this.descriptor = descriptor;
        }
    }

    // A collection's signature for constructor calls and field access.
    static final class CollSig {
        final String       name;
        final List<String> fieldNames;       // ordered as in source
        final List<String> fieldTypes;       // full types
        final String       constructorDescriptor;
        CollSig(String name, List<String> fieldNames, List<String> fieldTypes,
                String constructorDescriptor) {
            this.name = name;
            this.fieldNames = fieldNames;
            this.fieldTypes = fieldTypes;
            this.constructorDescriptor = constructorDescriptor;
        }
    }

    private Signatures() {}
}
