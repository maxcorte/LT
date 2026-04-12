package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;
import java.util.ArrayList;
import java.util.List;

public class FunDefNode extends TopLevelNode {

  public final TypeNode returnType; // peut être null (ex: main)
  public final String name;
  public List<ParamNode> params = new ArrayList<>();
  public final BlockNode body;

  public FunDefNode(TypeNode returnType, String name, List<ParamNode> params, BlockNode body) {
    this.returnType = returnType;
    this.name = name;
    this.params = params;
    this.body = body;
  }

  @Override
  public void print(String indent) {
    printIndent(indent);
    System.out.println("Function, " + name);
    if (returnType != null) {
      returnType.print(indent + "  ");
    } else {
      printIndent(indent + "  ");
      System.out.println("Type, void");
    }
    for (ParamNode p : params) {
      p.print(indent + "  ");
    }
    body.print(indent + "  ");
  }

  @Override
  public void accept(Visitor visitor) {

  }

}
