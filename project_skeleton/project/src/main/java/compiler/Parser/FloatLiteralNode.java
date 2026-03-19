package compiler.Parser;

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
}
