package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

public class VarRefNode extends ExprNode {

  public final String name;

  public VarRefNode(String name) {
    this.name = name;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Identifier, " + name);
  }
    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
