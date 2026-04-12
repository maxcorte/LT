package compiler.Parser;

import java.util.ArrayList;
import java.util.List;
import compiler.SemanticAnalysis.Visitor;

public class BlockNode extends StmtNode {

  public final List<StmtNode> statements = new ArrayList<>();

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Block");
    for (StmtNode s : statements) {
      s.print(indent + "  ");
    }
  }

  @Override
  public void accept(Visitor visitor) { visitor.visit(this); }
}
