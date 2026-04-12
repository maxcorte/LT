package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

// Accès champ : base.field
public class FieldAccessNode extends ExprNode {

  public final ExprNode base;
  public final String field;

  public FieldAccessNode(ExprNode base, String field) {
    this.base = base;
    this.field = field;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("FieldAccess, " + field);
    base.print(indent + "  ");
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
