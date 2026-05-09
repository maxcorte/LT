package compiler.CodeGen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

final class Scope {

    private final Deque<Map<String, Signatures.VarInfo>> stack = new ArrayDeque<>();
    private int nextSlot;

    // Clear and set the starting slot (1 for static main, 0 for static fns)
    void reset(int initialSlot) {
        stack.clear();
        nextSlot = initialSlot;
    }

    void push() { stack.push(new HashMap<>()); }
    void pop()  { stack.pop(); }

    Signatures.VarInfo lookup(String name) {
        for (Map<String, Signatures.VarInfo> s : stack) {
            Signatures.VarInfo v = s.get(name);
            if (v != null) return v;
        }
        return null;
    }

    Signatures.VarInfo declare(String name, String type) {
        Signatures.VarInfo info = new Signatures.VarInfo(type, nextSlot);
        nextSlot += 1;   // no long/double in this language
        stack.peek().put(name, info);
        return info;
    }

    int getNextSlot() { return nextSlot; }
}
