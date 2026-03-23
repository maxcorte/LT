import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.ProgramNode;
import compiler.Parser.VarDeclNode;
import compiler.Parser.IntLiteralNode;
import compiler.Parser.FloatLiteralNode;
import compiler.Parser.StringLiteralNode;
import compiler.Parser.BoolLiteralNode;
import compiler.Parser.CollDeclNode;
import compiler.Parser.FieldDeclNode;
import compiler.Parser.FunDefNode;
import compiler.Parser.ParamNode;
import compiler.Parser.ReturnNode;
import compiler.Parser.BinaryOpNode;
import compiler.Parser.VarRefNode;
import compiler.Parser.BlockNode;
import compiler.Parser.ForNode;

import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public class TestParser {

    // Helper commun
    private ProgramNode parse(String source) {
        Lexer lexer = new Lexer(new StringReader(source));
        Parser parser = new Parser(lexer);
        ProgramNode ast = parser.getAST();
        assertNotNull("AST ne doit pas être null", ast);
        return ast;
    }

    @Test
    public void testFinalIntVar() {
        String src = "final INT i = 3;";

        ProgramNode ast = parse(src);

        assertEquals(1, ast.topLevels.size());
        assertTrue(ast.topLevels.get(0) instanceof VarDeclNode);

        VarDeclNode decl = (VarDeclNode) ast.topLevels.get(0);
        assertTrue(decl.isFinal);
        assertEquals("INT", decl.type.name);
        assertEquals("i", decl.id.name);

        assertNotNull(decl.init);
        assertTrue(decl.init instanceof IntLiteralNode);
        IntLiteralNode lit = (IntLiteralNode) decl.init;
        assertEquals(3, lit.value);
    }

    @Test
    public void testFinalLiteralsAllTypes() {
        String src =
                "final INT    i = 3;\n" +
                        "final FLOAT  j = 3.2 * 5.0;\n" +
                        "final STRING s = \"Hello\";\n" +
                        "final BOOL   b = true;\n";

        ProgramNode ast = parse(src);

        assertEquals(4, ast.topLevels.size());

        // INT
        VarDeclNode vi = (VarDeclNode) ast.topLevels.get(0);
        assertEquals("INT", vi.type.name);
        assertTrue(vi.init instanceof IntLiteralNode);

        // FLOAT avec *
        VarDeclNode vj = (VarDeclNode) ast.topLevels.get(1);
        assertEquals("FLOAT", vj.type.name);
        assertTrue(vj.init instanceof BinaryOpNode);
        BinaryOpNode mul = (BinaryOpNode) vj.init;
        assertEquals("*", mul.op);
        assertTrue(mul.left  instanceof FloatLiteralNode);
        assertTrue(mul.right instanceof FloatLiteralNode);

        // STRING
        VarDeclNode vs = (VarDeclNode) ast.topLevels.get(2);
        assertEquals("STRING", vs.type.name);
        assertTrue(vs.init instanceof StringLiteralNode);
        assertEquals("Hello", ((StringLiteralNode) vs.init).value);

        // BOOL
        VarDeclNode vb = (VarDeclNode) ast.topLevels.get(3);
        assertEquals("BOOL", vb.type.name);
        assertTrue(vb.init instanceof BoolLiteralNode);
        assertTrue(((BoolLiteralNode) vb.init).value);
    }

    @Test
    public void testCollPoint() {
        String src =
                "coll Point {\n" +
                        "  INT x;\n" +
                        "  INT y;\n" +
                        "}\n";

        ProgramNode ast = parse(src);

        assertEquals(1, ast.topLevels.size());
        assertTrue(ast.topLevels.get(0) instanceof CollDeclNode);

        CollDeclNode coll = (CollDeclNode) ast.topLevels.get(0);
        assertEquals("Point", coll.name);
        assertEquals(2, coll.fields.size());

        FieldDeclNode f0 = coll.fields.get(0);
        assertEquals("INT", f0.type.name);
        assertEquals("x", f0.id.name);

        FieldDeclNode f1 = coll.fields.get(1);
        assertEquals("INT", f1.type.name);
        assertEquals("y", f1.id.name);
    }

    @Test
    public void testCollPerson() {
        String src =
                "coll Point {\n" +
                        "  INT x;\n" +
                        "  INT y;\n" +
                        "}\n" +
                        "coll Person {\n" +
                        "  STRING name;\n" +
                        "  Point  location;\n" +
                        "  INT[]  history;\n" +
                        "}\n";

        ProgramNode ast = parse(src);

        assertEquals(2, ast.topLevels.size());

        // Person est le 2e top-level
        CollDeclNode person = (CollDeclNode) ast.topLevels.get(1);
        assertEquals("Person", person.name);
        assertEquals(3, person.fields.size());

        FieldDeclNode nameField = person.fields.get(0);
        assertEquals("STRING", nameField.type.name);
        assertEquals("name", nameField.id.name);

        FieldDeclNode locField = person.fields.get(1);
        assertEquals("Point", locField.type.name);
        assertEquals("location", locField.id.name);

        FieldDeclNode histField = person.fields.get(2);
        assertEquals("INT", histField.type.name);
        assertTrue(histField.type.isArray);
        assertEquals("history", histField.id.name);
    }

    @Test
    public void testFunSquare() {
        String src =
                "def INT square(INT v) {\n" +
                        "  return v * v;\n" +
                        "}\n";

        ProgramNode ast = parse(src);

        assertEquals(1, ast.topLevels.size());
        assertTrue(ast.topLevels.get(0) instanceof FunDefNode);

        FunDefNode fun = (FunDefNode) ast.topLevels.get(0);
        assertEquals("square", fun.name);
        assertNotNull(fun.returnType);
        assertEquals("INT", fun.returnType.name);

        assertEquals(1, fun.params.size());
        ParamNode p0 = fun.params.get(0);
        assertEquals("INT", p0.type.name);
        assertEquals("v", p0.id.name);

        // corps : 1 statement = Return
        assertEquals(1, fun.body.statements.size());
        assertTrue(fun.body.statements.get(0) instanceof ReturnNode);
        ReturnNode ret = (ReturnNode) fun.body.statements.get(0);

        // expression de retour : v * v
        assertTrue(ret.expr instanceof BinaryOpNode);
        BinaryOpNode mul = (BinaryOpNode) ret.expr;
        assertEquals("*", mul.op);
        assertTrue(mul.left  instanceof VarRefNode);
        assertTrue(mul.right instanceof VarRefNode);
        assertEquals("v", ((VarRefNode) mul.left).name);
        assertEquals("v", ((VarRefNode) mul.right).name);
    }

    @Test
    public void testFunCopyPointsWithFor() {
        String src =
                "def main() {\n" +
                        "  INT i;\n" +
                        "  for (i; 1 -> 100; i+1) {\n" +
                        "  }\n" +
                        "}\n";

        ProgramNode ast = parse(src);

        assertEquals(1, ast.topLevels.size());
        assertTrue(ast.topLevels.get(0) instanceof FunDefNode);

        FunDefNode main = (FunDefNode) ast.topLevels.get(0);
        assertEquals("main", main.name);

        // Dans le bloc de main : déclaration INT i; puis for(...)
        assertEquals(2, main.body.statements.size());
        assertTrue(main.body.statements.get(1) instanceof ForNode);

        ForNode forNode = (ForNode) main.body.statements.get(1);
        assertEquals("i", forNode.varId.name);
        assertTrue(forNode.start instanceof IntLiteralNode);
        assertTrue(forNode.end   instanceof IntLiteralNode);
        assertTrue(forNode.step  instanceof BinaryOpNode);
        assertEquals(1, ((IntLiteralNode) forNode.start).value);
        assertEquals(100, ((IntLiteralNode) forNode.end).value);
    }
}

