package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;

public class WhileNode extends StmtNode {

  public final ExprNode condition;
  public final BlockNode body;

  public WhileNode(ExprNode condition, BlockNode body) {
    this.condition = condition;
    this.body = body;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("While");
    condition.print(indent + "  ");
    body.print(indent + "  ");
  }

  @Override
  public void accept(Visitor visitor) {

  }


}
