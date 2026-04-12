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
import compiler.Parser.ProgramNode;
import compiler.Parser.ReturnNode;
import compiler.Parser.StmtNode;
import compiler.Parser.StringLiteralNode;
import compiler.Parser.TypeNode;
import compiler.Parser.UnaryOpNode;
import compiler.Parser.VarDeclNode;
import compiler.Parser.VarDeclStmtNode;
import compiler.Parser.VarRefNode;
import compiler.Parser.WhileNode;

public interface Visitor {


  public void visit(ArrayAccessNode arrayAccessNode) throws Exception;
  public void visit(VarRefNode varRefNode) throws Exception;
  public void visit(BinaryOpNode binaryOpNode) throws Exception;
  public void visit(FloatLiteralNode floatLiteralNode);
  public void visit(BlockNode blockNode);
  public void visit(BoolLiteralNode boolLiteralNode);
  public void visit(CallNode callNode) throws Exception;
  public void visit(CollDeclNode collDeclNode) throws Exception;
  public void visit(ExprStmtNode exprStmtNode);
  public void visit(FieldAccessNode fieldAccessNode) throws Exception;
  public void visit(FieldDeclNode fieldDeclNode) throws Exception;
  public void visit(ForNode forNode) throws Exception;
  public void visit(IntLiteralNode intLiteralNode);
  public void visit(FunDefNode funDefNode) throws Exception;
  public void visit(IdentifierNode identifierNode);
  public void visit(IfNode ifNode) throws Exception;
  public void visit(NewArrayNode newArrayNode) throws Exception;
  public void visit(ParamNode paramNode) throws Exception;
  public void visit(ReturnNode returnNode) throws Exception;
  public void visit(StringLiteralNode stringLiteralNode);
  public void visit(TypeNode typeNode);
  public void visit(UnaryOpNode unaryOpNode) throws Exception;
  public void visit(VarDeclNode varDeclNode);
  public void visit(VarDeclStmtNode varDeclStmtNode) throws Exception;
  public void visit(WhileNode whileNode) throws Exception;

  public void visit(ProgramNode programNode);

}
