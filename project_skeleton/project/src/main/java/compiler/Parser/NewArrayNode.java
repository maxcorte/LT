package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

// Création de tableau : INT ARRAY[5]
public class NewArrayNode extends ExprNode {

  public final String elementType;
  public final ExprNode size;

  public NewArrayNode(String elementType, ExprNode size) {
    this.elementType = elementType;
    this.size = size;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("NewArray, " + elementType);
    size.print(indent + "  ");
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
