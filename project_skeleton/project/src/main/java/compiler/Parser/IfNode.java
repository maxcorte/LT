package compiler.Parser;
import compiler.SemanticAnalysis.Visitor;

public class IfNode extends StmtNode {

  public final ExprNode condition;
  public final BlockNode thenBlock;
  public final BlockNode elseBlock; // peut être null

  public IfNode(ExprNode condition, BlockNode thenBlock, BlockNode elseBlock) {
    this.condition = condition;
    this.thenBlock = thenBlock;
    this.elseBlock = elseBlock;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("If");
    condition.print(indent + "  ");
    thenBlock.print(indent + "  ");
    if (elseBlock != null) {
      printIndent(indent);
      System.out.println("Else");
      elseBlock.print(indent + "  ");
    }
  }

    @Override
    public void accept(Visitor visitor) { visitor.visit(this); }
}
