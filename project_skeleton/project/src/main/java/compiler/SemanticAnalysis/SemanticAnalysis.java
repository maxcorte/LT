package compiler.SemanticAnalysis;

import compiler.Lexer.Lexer.Sym;
import compiler.Parser.ProgramNode;

public class SemanticAnalysis {

  private ProgramNode ast;
  private SymbolTable tables;

  public SemanticAnalysis(ProgramNode ast) {
    this.ast = ast;
  }

  public void analyze(ProgramNode root){
    this.tables = new SymbolTable();
    check(root);
  }

  private void check(ProgramNode root) {

  }
// comme dans parser il faut check chaque node et lever une exception

}
