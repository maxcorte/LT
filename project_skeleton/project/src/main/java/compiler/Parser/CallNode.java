package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;
import java.util.List;

// Appel de fonction / constructeur : f(...), Point(...), read_INT()
public class CallNode extends ExprNode {

  public final ExprNode callee;      // VarRefNode ou autre (pour éventuellement supporter obj.method())
  public final List<ExprNode> args;  // peut être vide

  public CallNode(ExprNode callee, List<ExprNode> args) {
    this.callee = callee;
    this.args = args;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Call");
    callee.print(indent + "  ");
    for (ExprNode a : args) {
      a.print(indent + "  ");
    }
  }

  @Override
  public void accept(Visitor visitor) {

  }
}
