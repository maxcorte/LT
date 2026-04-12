import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.ProgramNode;
import compiler.SemanticAnalysis.SemanticAnalysis;
import compiler.SemanticAnalysis.SemanticException;

import org.junit.Test;
import java.io.StringReader;

import static org.junit.Assert.*;

public class TestSemanticAnalysis {

    private ProgramNode parse(String source) {
        Lexer lexer = new Lexer(new StringReader(source));
        Parser parser = new Parser(lexer);
        ProgramNode ast = parser.getAST();
        assertNotNull("AST ne doit pas etre null", ast);
        return ast;
    }

    private void analyze(String source) {
        ProgramNode ast = parse(source);
        new SemanticAnalysis().analyze(ast);
    }

    private SemanticException expectError(String source) {
        try {
            analyze(source);
            fail("Une SemanticException etait attendue mais n'a pas ete levee");
            return null;
        } catch (SemanticException e) {
            return e;
        }
    }

    private void assertKeyword(SemanticException e, String keyword) {
        assertTrue(
                "Le message '" + e.getMessage() + "' devrait contenir '" + keyword + "'",
                e.getMessage().contains(keyword)
        );
    }


    // TypeError


    @Test
    public void testTypeError_assignStringToInt() {
        // INT value = "Bonjour"  -> TypeError
        String src =
                "def main() {\n" +
                        "  INT value = \"Bonjour\";\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "TypeError");
    }

    @Test
    public void testTypeError_assignBoolToFloat() {
        String src =
                "def main() {\n" +
                        "  FLOAT x = true;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "TypeError");
    }

    @Test
    public void testTypeError_assignIntToString() {
        String src =
                "def main() {\n" +
                        "  STRING s = 42;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "TypeError");
    }

    @Test
    public void testTypeError_noError_intToFloat() {
        // INT -> FLOAT : promotion implicite autorisee
        String src =
                "def main() {\n" +
                        "  FLOAT x = 3;\n" +
                        "}\n";

        // Ne doit PAS lever d'exception
        analyze(src);
    }


    // CollectionError

    @Test
    public void testCollectionError_duplicate() {
        // coll Point declare deux fois -> CollectionError
        String src =
                "coll Point {\n" +
                        "  INT x;\n" +
                        "  INT y;\n" +
                        "}\n" +
                        "coll Point {\n" +
                        "  BOOL z;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "CollectionError");
    }

    @Test
    public void testCollectionError_reservedTypeName() {
        // coll INT : ecrase un type primitif -> CollectionError
        String src =
                "coll INT {\n" +
                        "  BOOL flag;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "CollectionError");
    }

    @Test
    public void testCollectionError_noError_valid() {
        String src =
                "coll Point {\n" +
                        "  INT x;\n" +
                        "  INT y;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "}\n";

        analyze(src);
    }

    // OperatorError


    @Test
    public void testOperatorError_addIntAndBool() {
        // INT + BOOL -> OperatorError
        String src =
                "def main() {\n" +
                        "  INT value = 1;\n" +
                        "  BOOL value2 = true;\n" +
                        "  INT test = value + value2;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "OperatorError");
    }

    @Test
    public void testOperatorError_mulStringAndInt() {
        String src =
                "def main() {\n" +
                        "  STRING s = \"hello\";\n" +
                        "  INT n = s * 2;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "OperatorError");
    }

    @Test
    public void testOperatorError_logicalOnInt() {
        // && sur des INT -> OperatorError
        String src =
                "def main() {\n" +
                        "  INT a = 1;\n" +
                        "  INT b = 0;\n" +
                        "  BOOL r = a && b;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "OperatorError");
    }

    @Test
    public void testOperatorError_unaryMinusOnBool() {
        String src =
                "def main() {\n" +
                        "  BOOL b = true;\n" +
                        "  INT x = -b;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "OperatorError");
    }

    @Test
    public void testOperatorError_noError_intArithmetic() {
        String src =
                "def main() {\n" +
                        "  INT a = 2;\n" +
                        "  INT b = 3;\n" +
                        "  INT c = a + b;\n" +
                        "}\n";

        analyze(src);
    }


    // ArgumentError

    @Test
    public void testArgumentError_wrongParamType() {
        // square attend INT, on passe BOOL -> ArgumentError
        String src =
                "def INT square(INT v) {\n" +
                        "  return v * v;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT value = 2;\n" +
                        "  BOOL test = true;\n" +
                        "  INT result = square(test);\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ArgumentError");
    }

    @Test
    public void testArgumentError_tooManyArgs() {
        String src =
                "def INT square(INT v) {\n" +
                        "  return v * v;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT result = square(2, 3);\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ArgumentError");
    }

    @Test
    public void testArgumentError_tooFewArgs() {
        String src =
                "def INT add(INT a, INT b) {\n" +
                        "  return a + b;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT result = add(1);\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ArgumentError");
    }

    @Test
    public void testArgumentError_noError_correctCall() {
        String src =
                "def INT square(INT v) {\n" +
                        "  return v * v;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT result = square(5);\n" +
                        "}\n";

        analyze(src);
    }

    // MissingConditionError

    @Test
    public void testMissingConditionError_whileWithString() {
        // while (STRING) -> MissingConditionError
        String src =
                "def INT square(INT v) {\n" +
                        "  return v * v;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT value = 2;\n" +
                        "  INT result = square(value);\n" +
                        "  STRING message = \"Bonjour\";\n" +
                        "  while (message) {\n" +
                        "    result = result + 1;\n" +
                        "  }\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "MissingConditionError");
    }

    @Test
    public void testMissingConditionError_ifWithInt() {
        String src =
                "def main() {\n" +
                        "  INT x = 5;\n" +
                        "  if (x) {\n" +
                        "    INT y = 1;\n" +
                        "  }\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "MissingConditionError");
    }

    @Test
    public void testMissingConditionError_whileWithFloat() {
        String src =
                "def main() {\n" +
                        "  FLOAT f = 1.5;\n" +
                        "  while (f) {\n" +
                        "    f = f - 1.0;\n" +
                        "  }\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "MissingConditionError");
    }

    @Test
    public void testMissingConditionError_noError_boolCondition() {
        String src =
                "def main() {\n" +
                        "  BOOL flag = true;\n" +
                        "  while (flag) {\n" +
                        "    flag = false;\n" +
                        "  }\n" +
                        "}\n";

        analyze(src);
    }

    // ReturnError

    @Test
    public void testReturnError_wrongReturnType() {
        // def BOOL square(INT v) retourne INT -> ReturnError
        String src =
                "def BOOL square(INT v) {\n" +
                        "  return v * v;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT value = 2;\n" +
                        "  INT result = square(value);\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ReturnError");
    }

    @Test
    public void testReturnError_returnInVoidFunction() {
        String src =
                "def main() {\n" +
                        "  return 42;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ReturnError");
    }

    @Test
    public void testReturnError_returnStringWhereIntExpected() {
        String src =
                "def INT getNumber() {\n" +
                        "  return \"hello\";\n" +
                        "}\n" +
                        "def main() {\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ReturnError");
    }

    @Test
    public void testReturnError_noError_correctReturn() {
        String src =
                "def INT square(INT v) {\n" +
                        "  return v * v;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT x = square(3);\n" +
                        "}\n";

        analyze(src);
    }

    // ScopeError

    @Test
    public void testScopeError_variableOutOfScope() {
        // misdirection est locale a square, pas visible dans main -> ScopeError
        String src =
                "def INT square(INT v) {\n" +
                        "  INT misdirection = v;\n" +
                        "  return v * v;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT value = 2;\n" +
                        "  INT result = square(value);\n" +
                        "  INT misdirection2 = misdirection;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ScopeError");
    }

    @Test
    public void testScopeError_undeclaredVariable() {
        String src =
                "def main() {\n" +
                        "  INT x = y;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ScopeError");
    }

    @Test
    public void testScopeError_duplicateDeclarationInSameScope() {
        String src =
                "def main() {\n" +
                        "  INT x = 1;\n" +
                        "  INT x = 2;\n" +
                        "}\n";

        SemanticException e = expectError(src);
        assertKeyword(e, "ScopeError");
    }

    @Test
    public void testScopeError_noError_shadowingAllowed() {
        // Un parametre peut avoir le meme nom qu'une variable globale
        String src =
                "INT x = 10;\n" +
                        "def INT getX(INT x) {\n" +
                        "  return x;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT result = getX(5);\n" +
                        "}\n";

        analyze(src);
    }

    @Test
    public void testScopeError_noError_variableVisibleInNestedBlock() {
        String src =
                "def main() {\n" +
                        "  INT x = 1;\n" +
                        "  if (x == 1) {\n" +
                        "    INT y = x + 1;\n" +
                        "  }\n" +
                        "}\n";

        analyze(src);
    }

    // Cas valides (aucune exception attendue)

    @Test
    public void testValid_fullProgram() {
        String src =
                "final INT i = 3;\n" +
                        "coll Point {\n" +
                        "  INT x;\n" +
                        "  INT y;\n" +
                        "}\n" +
                        "def INT square(INT v) {\n" +
                        "  return v * v;\n" +
                        "}\n" +
                        "def main() {\n" +
                        "  INT value = 4;\n" +
                        "  INT result = square(value);\n" +
                        "  BOOL flag = result == 16;\n" +
                        "  if (flag) {\n" +
                        "    INT z = result + 1;\n" +
                        "  }\n" +
                        "}\n";

        analyze(src);
    }

    @Test
    public void testValid_forLoop() {
        String src =
                "def main() {\n" +
                        "  INT i;\n" +
                        "  for (i; 1 -> 10; i + 1) {\n" +
                        "    INT x = i;\n" +
                        "  }\n" +
                        "}\n";

        analyze(src);
    }
}

