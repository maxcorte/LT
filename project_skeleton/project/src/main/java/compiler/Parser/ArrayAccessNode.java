package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

// Accès tableau : base[index]
public class ArrayAccessNode extends ExprNode {

  public final ExprNode base;
  public final ExprNode index;

  public ArrayAccessNode(ExprNode base, ExprNode index) {
    this.base = base;
    this.index = index;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("ArrayAccess");
    base.print(indent + "  ");
    index.print(indent + "  ");
  }

  @Override
  public void accept(Visitor visitor) {
      visitor.visit(this);
  }

}
