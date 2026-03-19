package compiler.Parser;

public class BoolLiteralNode extends ExprNode {

  public final boolean value;

  public BoolLiteralNode(boolean value) {
    this.value = value;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Bool, " + value);
  }
}
