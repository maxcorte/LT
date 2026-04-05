package compiler.Parser;

public class ExprStmtNode extends StmtNode {

  public final ExprNode expr;

  public ExprStmtNode(ExprNode expr) {
    this.expr = expr;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("ExprStmt");
    expr.print(indent + "  ");
  }
}
