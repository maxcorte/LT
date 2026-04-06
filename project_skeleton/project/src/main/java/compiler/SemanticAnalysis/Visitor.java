package compiler.SemanticAnalysis;

import compiler.Parser.ArrayAccessNode;
import compiler.Parser.BinaryOpNode;
import compiler.Parser.BlockNode;
import compiler.Parser.BoolLiteralNode;
import compiler.Parser.CallNode;
import compiler.Parser.CollDeclNode;
import compiler.Parser.ExprNode;
import compiler.Parser.ExprStmtNode;
import compiler.Parser.FieldAccessNode;
import compiler.Parser.FieldDeclNode;
import compiler.Parser.FloatLiteralNode;
import compiler.Parser.ForNode;
import compiler.Parser.FunDefNode;
import compiler.Parser.IdentifierNode;
import compiler.Parser.IfNode;
import compiler.Parser.IntLiteralNode;
import compiler.Parser.NewArrayNode;
import compiler.Parser.ParamNode;
import compiler.Parser.ReturnNode;
import compiler.Parser.StmtNode;
import compiler.Parser.StringLiteralNode;
import compiler.Parser.TopLevelNode;
import compiler.Parser.TypeNode;
import compiler.Parser.UnaryOpNode;
import compiler.Parser.VarDeclNode;
import compiler.Parser.VarDeclStmtNode;
import compiler.Parser.VarRefNode;
import compiler.Parser.WhileNode;

public interface Visitor {

  public void visit(ArrayAccessNode arrayAccessNode);
  public void visit(BinaryOpNode binaryOpNode);
  public void visit(FloatLiteralNode floatLiteralNode);
  public void visit(BlockNode blockNode);
  public void visit(BoolLiteralNode boolLiteralNode);
  public void visit(CallNode callNode);
  public void visit(CollDeclNode collDeclNode);
  public void visit(ExprStmtNode exprStmtNode);
  public void visit(FieldAccessNode fieldAccessNode);
  public void visit(FieldDeclNode fieldDeclNode);
  public void visit(ForNode forNode);
  public void visit(IntLiteralNode intLiteralNode);
  public void visit(FunDefNode funDefNode);
  public void visit(IdentifierNode identifierNode);
  public void visit(IfNode ifNode);
  public void visit(NewArrayNode newArrayNode);
  public void visit(ParamNode paramNode);
  public void visit(ReturnNode returnNode);
  public void visit(TopLevelNode topLevelNode);
  public void visit(StringLiteralNode stringLiteralNode);
  public void visit(TypeNode typeNode);
  public void visit(UnaryOpNode unaryOpNode);
  public void visit(VarDeclNode varDeclNode);
  public void visit(VarDeclStmtNode varDeclStmtNode);
  public void visit(VarRefNode varRefNode);
  public void visit(WhileNode whileNode);

}
