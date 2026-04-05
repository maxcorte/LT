package compiler.Parser;

public class IdentifierNode extends ASTNode {

  public final String name;

  public IdentifierNode(String name) {
    this.name = name;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Identifier, " + name);
  }
}
