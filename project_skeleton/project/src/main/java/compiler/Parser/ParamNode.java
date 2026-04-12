package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

public class ParamNode extends ASTNode {

  public final TypeNode type;
  public final IdentifierNode id;

  public ParamNode(TypeNode type, IdentifierNode id) {
    this.type = type;
    this.id = id;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Param");
    type.print(indent + "  ");
    id.print(indent + "  ");
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
