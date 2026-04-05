package compiler.Parser;

// for (INT i; 1 -> 100; i + 1) { ... }
public class ForNode extends StmtNode {

  public final TypeNode varType;
  public final IdentifierNode varId;
  public final ExprNode start;
  public final ExprNode end;
  public final ExprNode step;
  public final BlockNode body;

  public ForNode(TypeNode varType, IdentifierNode varId,
      ExprNode start, ExprNode end, ExprNode step,
      BlockNode body) {
    this.varType = varType;
    this.varId = varId;
    this.start = start;
    this.end = end;
    this.step = step;
    this.body = body;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("For");
      if (varType != null) {
          varType.print(indent + "  ");
      }
    varId.print(indent + "  ");
    printIndent(indent + "  ");
    System.out.println("Range");
    start.print(indent + "    ");
    end.print(indent + "    ");
    printIndent(indent + "  ");
    System.out.println("Step");
    step.print(indent + "    ");
    body.print(indent + "  ");
  }
}
