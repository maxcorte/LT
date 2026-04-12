package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;

// Déclaration locale (on réutilise VarDeclNode)

public class VarDeclStmtNode extends StmtNode {

  public final VarDeclNode decl;

  public VarDeclStmtNode(VarDeclNode decl) {
    this.decl = decl;
  }

  @Override
  public void print(String indent) {
    decl.print(indent);
  }

  @Override
  public void accept(Visitor visitor) {

  }


}
