package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;

public class UnaryMinusOneNode extends ExprNode {

  public ExprNode exprNode;

  public UnaryMinusOneNode(ExprNode exprNode) {
    this.exprNode = exprNode;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("UnaryMinusOne");
    exprNode.print(indent + "  ");
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
