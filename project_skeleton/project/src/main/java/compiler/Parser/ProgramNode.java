package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;
import java.util.ArrayList;
import java.util.List;

// Programme complet
public class ProgramNode extends ASTNode {

  public final List<ASTNode> topLevels = new ArrayList<>();

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Program");
    for (ASTNode n : topLevels) {
      n.print(indent + "  ");
    }
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
