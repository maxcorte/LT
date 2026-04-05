package compiler.Parser;

public abstract class ASTNode {

  public abstract void print(String indent);

  protected void printIndent(String indent) {
    System.out.print(indent);
  }
}
