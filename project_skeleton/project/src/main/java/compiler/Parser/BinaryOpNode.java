package compiler.Parser;

// Op binaire (arithmétique, comparaison, logique)
public class BinaryOpNode extends ExprNode {

  public final String op; // "+", "*", "==", "=/=", "&&", ...
  public final ExprNode left, right;

  public BinaryOpNode(String op, ExprNode left, ExprNode right) {
    this.op = op;
    this.left = left;
    this.right = right;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("BinaryOp, " + op);
    left.print(indent + "  ");
    right.print(indent + "  ");
  }
}
