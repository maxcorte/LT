package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;

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

  @Override
  public void accept(Visitor visitor) {

  }


}
