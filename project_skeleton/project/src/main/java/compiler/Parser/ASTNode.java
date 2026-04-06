package compiler.Parser;

import compiler.SemanticAnalysis.Visitor;

// element qui fait herite des concreteElement
public abstract class ASTNode {

  public abstract void print(String indent);

  protected void printIndent(String indent) {
    System.out.print(indent);
  }


  public abstract void accept(Visitor visitor) ;


}
