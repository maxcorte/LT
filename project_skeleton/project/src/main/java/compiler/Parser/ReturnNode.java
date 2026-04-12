package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

public class ReturnNode extends StmtNode {

  public final ExprNode expr; // peut être null

  public ReturnNode(ExprNode expr) {
    this.expr = expr;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Return");
    if (expr != null) {
      expr.print(indent + "  ");
    }
  }
    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
