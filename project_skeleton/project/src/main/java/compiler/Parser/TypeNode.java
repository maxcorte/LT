package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

public class TypeNode extends ASTNode {

  public final String name;   // "INT", "FLOAT", "Point", ...
  public final boolean isArray;

  public TypeNode(String name, boolean isArray) {
    this.name = name;
    this.isArray = isArray;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Type, " + name + (isArray ? "[]" : ""));
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
