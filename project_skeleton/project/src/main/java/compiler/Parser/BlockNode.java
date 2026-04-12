package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;
import java.util.ArrayList;
import java.util.List;

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
  public void accept(Visitor visitor) {

  }

}
