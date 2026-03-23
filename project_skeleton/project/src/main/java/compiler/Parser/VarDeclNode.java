package compiler.Parser;

public class VarDeclNode extends TopLevelNode {

  public final boolean isFinal;
  public final TypeNode type;
  public final IdentifierNode id;
  public final ExprNode init; // peut être null

  public VarDeclNode(boolean isFinal, TypeNode type, IdentifierNode id, ExprNode init) {
    this.isFinal = isFinal;
    this.type = type;
    this.id = id;
    this.init = init;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("VarDecl " + (isFinal ? "FINAL" : ""));
    type.print(indent + "  ");
    id.print(indent + "  ");
    if (init != null) {
      printIndent(indent + "  ");
      System.out.println("AssignmentOperator");
      init.print(indent + "    ");
    }
  }
}
