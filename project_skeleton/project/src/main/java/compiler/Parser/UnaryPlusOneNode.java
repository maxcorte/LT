package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;

public class UnaryPlusOneNode extends ExprNode{

  public ExprNode exprNode;

  public UnaryPlusOneNode(ExprNode exprNode) {
    this.exprNode = exprNode;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("UnaryPlusOne");
    exprNode.print(indent + "  ");
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
