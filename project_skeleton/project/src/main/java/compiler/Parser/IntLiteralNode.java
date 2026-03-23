package compiler.Parser;

public class IntLiteralNode extends ExprNode {

  public final int value;

  public IntLiteralNode(int value) {
    this.value = value;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Integer, " + value);
  }
}
