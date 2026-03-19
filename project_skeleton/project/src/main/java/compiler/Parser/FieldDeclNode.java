package compiler.Parser;

// Déclaration d’un champ dans une collection
public class FieldDeclNode extends ASTNode {

  public final TypeNode type;
  public final IdentifierNode id;

  public FieldDeclNode(TypeNode type, IdentifierNode id) {
    this.type = type;
    this.id = id;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("FieldDecl");
    type.print(indent + "  ");
    id.print(indent + "  ");
  }
}
