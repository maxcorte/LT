package compiler;

import compiler.Lexer.Lexer;
import compiler.Lexer.Lexer.Sym;
import compiler.Lexer.Symbol;
import compiler.Parser.Parser;
import compiler.Parser.ProgramNode;
import compiler.SemanticAnalysis.SemanticAnalysis;
import compiler.SemanticAnalysis.SemanticException;
import compiler.CodeGen.CodeGenerator;

import java.io.File;

public class Compiler {
    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("  <source.lang> [-o <target.class>]    (code generation, default)");
            System.err.println("  -lexer    <source.lang>              (tokens only)");
            System.err.println("  -parser   <source.lang>              (parse tree only)");
            System.err.println("  -semantic <source.lang>              (semantic check only)");
            System.err.println("  -codegen  <source.lang> [-o <target.class>]");
            System.exit(1);
            return;
        }

        if (args[0].startsWith("-")) {
            runLegacyMode(args);
            return;
        }

        runCodegen(args, 0);
    }


    /** sourceArgIndex is the position of the .lang file in args. */
    private static void runCodegen(String[] args, int sourceArgIndex) {
        try {
            String sourceFile = args[sourceArgIndex];

            // Parse optional -o flag anywhere after the source argument
            String outputArg = null;
            for (int i = sourceArgIndex + 1; i < args.length - 1; i++) {
                if ("-o".equals(args[i])) {
                    outputArg = args[i + 1];
                    break;
                }
            }

            // Resolve class name and output directory
            String className;
            String outputDir;
            if (outputArg != null) {
                File out = new File(outputArg);
                String filename = out.getName();
                int dot = filename.lastIndexOf('.');
                className = (dot < 0) ? filename : filename.substring(0, dot);
                String parent = out.getParent();
                outputDir = (parent == null) ? "." : parent;
            } else {
                // No -o: produce test.class in cwd
                className = "test";
                outputDir = ".";
            }

            // 1. Lexing + Parsing
            Lexer lexer = new Lexer(new java.io.FileReader(sourceFile));
            Parser parser = new Parser(lexer);
            ProgramNode ast = parser.getAST();

            // 2. Semantic analysis
            SemanticAnalysis sa = new SemanticAnalysis();
            sa.analyze(ast);

            // 3. Bytecode generation
            CodeGenerator gen = new CodeGenerator(
                    sa.getSymbolTable(), className, outputDir);
            gen.generate(ast);

        } catch (SemanticException e) {
            System.err.println(e.getMessage());
            System.exit(2);

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runLegacyMode(String[] args) {
        if (args.length < 2) {
            System.err.println("Pas assez d'arguments pour " + args[0]);
            System.exit(1);
            return;
        }

        if (args[0].equals("-lexer")) {
            try {
                Lexer lexer = new Lexer(new java.io.FileReader(args[1]));
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
        else if (args[0].equals("-parser")) {
            try {
                Lexer lexer = new Lexer(new java.io.FileReader(args[1]));
                Parser parser = new Parser(lexer);
                ProgramNode ast = parser.getAST();
                System.out.println("AST:");
                ast.print("");
            } catch (Exception e) {
                System.err.println("Erreur lors de l'analyse: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (args[0].equals("-semantic")) {
            try {
                Lexer lexer = new Lexer(new java.io.FileReader(args[1]));
                Parser parser = new Parser(lexer);
                ProgramNode ast = parser.getAST();
                SemanticAnalysis sa = new SemanticAnalysis();
                sa.analyze(ast);
                System.out.println("Analyse semantique reussie.");
            } catch (SemanticException e) {
                System.err.println(e.getMessage());
                System.exit(2);
            } catch (Exception e) {
                System.err.println("Erreur: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
        else if (args[0].equals("-codegen")) {
            // Same as default mode, but with the source at index 1
            runCodegen(args, 1);
        }
        else {
            System.err.println("Option inconnue: " + args[0]);
            System.exit(1);
        }
    }
}