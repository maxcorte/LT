package compiler.Parser;

import java.util.ArrayList;
import java.util.List;
import compiler.SemanticAnalysis.Visitor;

// Déclaration de collection : coll Point { ... }
public class CollDeclNode extends TopLevelNode {

  public final String name;
  public final List<FieldDeclNode> fields = new ArrayList<>();

  public CollDeclNode(String name) {
    this.name = name;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Collection, " + name);
    for (FieldDeclNode f : fields) {
      f.print(indent + "  ");
    }
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
