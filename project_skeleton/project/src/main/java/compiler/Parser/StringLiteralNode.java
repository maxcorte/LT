package compiler.Parser;

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
}
