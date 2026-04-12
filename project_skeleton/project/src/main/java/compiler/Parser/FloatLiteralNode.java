package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

public class FloatLiteralNode extends ExprNode {

  public final float value;

  public FloatLiteralNode(float value) {
    this.value = value;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Float, " + value);
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
