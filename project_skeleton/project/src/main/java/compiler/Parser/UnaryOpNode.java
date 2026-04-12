package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

public class UnaryOpNode extends ExprNode {

  public final String op; // "-", "not"
  public final ExprNode expr;

  public UnaryOpNode(String op, ExprNode expr) {
    this.op = op;
    this.expr = expr;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("UnaryOp, " + op);
    expr.print(indent + "  ");
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
