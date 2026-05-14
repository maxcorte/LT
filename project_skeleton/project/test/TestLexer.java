import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.ProgramNode;

import java.io.StringReader;

public class MainTestParser {
    public static void main(String[] args) {
        try {

            // 🔥 TEST 1 : déclaration (ton cas actuel)
            String input = "final INT i = 3;";
            // 🔥 TEST 2 : assignation simple
            String input2 = "INT i = 2;";

            // 🔥 TEST 4 : opérateur composé (si implémenté)
            String input4 = "INT x +=0 ;";

            run(input2);
            run(input4);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void run(String input) {
        try {
            System.out.println("\n====================");
            System.out.println("INPUT: " + input);

            Lexer lexer = new Lexer(new StringReader(input));
            Parser parser = new Parser(lexer);

            ProgramNode ast = parser.getAST();

            System.out.println("AST OK");
            ast.print("");

        } catch (Exception e) {
            System.out.println("ERROR on input: " + input);
            e.printStackTrace();
        }
    }
}