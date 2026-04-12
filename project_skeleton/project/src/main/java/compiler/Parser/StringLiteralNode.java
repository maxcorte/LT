package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

public class StringLiteralNode extends ExprNode {

  public final String value;

  public StringLiteralNode(String value) {
    this.value = value;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("String, " + value);
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
