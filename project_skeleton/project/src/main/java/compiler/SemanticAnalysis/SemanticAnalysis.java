package compiler.SemanticAnalysis;

import compiler.Lexer.Lexer.Sym;
import compiler.Parser.ASTNode;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.framework.qual.DefaultQualifier;

public class SemanticAnalysis implements Visitor {

  private ProgramNode ast;
  private SymbolTable tables;
  private String type;
  private TypeNode returnType;

  private HashMap<String, Map<String,String>> collections;


  public SemanticAnalysis(ProgramNode ast) {
    this.ast = ast;
  }

  public void analyze(ProgramNode root){
    this.tables = new SymbolTable(null);
    check(root);
  }

  private void check(ProgramNode programNode) {
    this.tables =  new SymbolTable(null);
    programNode.accept(this);

  }

  @Override
  public void visit(ProgramNode programNode) {
    for  (ASTNode node : programNode.topLevels){
      node.accept(this);
    }
  }

  @Override
  public void visit(ArrayAccessNode arrayAccessNode) throws Exception {
    arrayAccessNode.base.accept(this);
    String baseType = this.type;
    arrayAccessNode.index.accept(this);
    String indexType = this.type;


    if (!baseType.contains("[") && !baseType.contains("]")){
      throw new Exception("TypeError ");
    }
    if (!indexType.equals("INT")){
      throw new Exception("TypeError ");
    }

  }

  @Override
  public void visit(BinaryOpNode binaryOpNode) throws Exception {
    binaryOpNode.left.accept(this);
    String leftType = type;
    binaryOpNode.right.accept(this);
    String rightType = type;

    switch (binaryOpNode.op){
      // + - * / accept float and int and string for "+"
      case "+","-", "*", "/" ->{

        if ((!leftType.equals("INT")) && (!leftType.equals("FLOAT"))){
          throw new Exception("TypeError ");
        }
        if ((!leftType.equals(rightType))){
          throw new Exception("TypeError ");
        }

        if (binaryOpNode.op.equals("+")){
          if (!leftType.equals(rightType) && !leftType.equals("String")){
            throw new Exception("TypeError ");
          }
        }


      }

      // && || only BOOL
      case "&&", "||" ->{
        if ((!leftType.equals("BOOL")) ){
          throw new Exception("TypeError ");
        }
        if ((!rightType.equals("BOOL")) ){
          throw new Exception("TypeError ");
        }
        type = "BOOL";

      }

      // == int and float accepeted
      // == (equals) et =/= (!=)
      case "==", "=/="->{
        if ((!leftType.equals(rightType)) ){
          throw new Exception("TypeError ");
        }
        type = "BOOL";

      }

      case ">","<","<=",">=" ->{
        if ((!leftType.equals("INT")) && (!leftType.equals("FLOAT"))){
          throw new Exception("TypeError ");
        }
        type = "BOOL";
      }

      default -> throw new Exception("OperatorError : unknwon");

    }

  }



  @Override
  public void visit(BlockNode blockNode) {

    SymbolTable currentTable = this.tables;
    this.tables = new SymbolTable(this.tables);
    for (StmtNode stmtNode : blockNode.statements){
      stmtNode.accept(this);
    }
    this.tables = currentTable;


  }


  public void visit(FunDefNode  funDefNode) throws Exception {

    if (tables.contains(funDefNode.name)){
      throw new Exception("ScopeError : ");
    }

    tables.add(funDefNode.name, funDefNode.returnType.name);

    this.returnType = funDefNode.returnType;

    SymbolTable currentTable = this.tables;
    this.tables = new SymbolTable(this.tables);
    for ( ParamNode paramNode : funDefNode.params){
      paramNode.accept(this);
    }
    this.tables = currentTable;

  }

  @Override
  public void visit(CallNode callNode) throws Exception {
    callNode.callee.accept(this);

    if (!tables.contains(callNode.callee.toString())){
      throw new Exception("ScopeError : never declared");
    }


    SymbolTable currentTable = this.tables;
    this.tables = new SymbolTable(this.tables);
    if (!callNode.args.isEmpty()){
      for (ExprNode e : callNode.args) {
        e.accept(this);
      }
    }
    this.tables = currentTable;


  }

  // expr
  @Override
  public void visit(ExprStmtNode exprStmtNode) {
    exprStmtNode.expr.accept(this);
  }

  @Override
  public void visit(CollDeclNode collDeclNode) throws Exception {

    // verifie si condition de nommage respecter,
    if (collDeclNode.name.charAt(0) == collDeclNode.name.toLowerCase().charAt(0)){
      throw new Exception("CollectionError : the name don't start with a Majuscule");
    }

    // verifie si condition de nommage respecter, person
    if (tables.containsLocal(collDeclNode.name) || tables.containsParent(collDeclNode.name)){
      throw new Exception("CollectionError : the name already exist");
    }

    // verifie si condition de nommage respecter  while , int,...

    // ajouter a la Symboltable pour garder a jour ce qu'on a vu jusqu'a mtn dans le code
    tables.add(collDeclNode.name, "COLL");

    String collName = collDeclNode.name;
    Map<String,String> fields = collections.get(collName);

    if (fields == null){
      fields = new HashMap<>();
      collections.put(collName,fields);
    }

    SymbolTable currentTable = this.tables;
    this.tables = new SymbolTable(this.tables);

    for (FieldDeclNode f : collDeclNode.fields){
      f.accept(this);
      fields.put(f.id.name,f.type.name);
    }
    this.tables = currentTable;


  }
  @Override
  public void visit(FieldDeclNode fieldDeclNode) throws Exception {
    if (tables.containsLocal(fieldDeclNode.id.toString())){
      throw new Exception("ScopeError : the name already exist");
    }
    tables.add(fieldDeclNode.id.toString(), fieldDeclNode.type.name);

  }

  @Override
  public void visit(FieldAccessNode fieldAccessNode) throws Exception {
    fieldAccessNode.base.accept(this);
    String baseType = this.type;
    String baseName = fieldAccessNode.base.toString();

    if (!tables.contains(baseName)){
      throw new Exception("ScopeError : never declared");
    }


    Map<String, String> baseCollFields = collections.get(baseType);

    if (!baseCollFields.containsKey(fieldAccessNode.field)){
      throw new Exception("ScopeError : fields don't exist");

    }
    this.type = baseCollFields.get(fieldAccessNode.field);

  }





  @Override
  public void visit(ForNode forNode) throws Exception {

    // for (INT i; 1 -> 100; i + 1) { ... }
    // varType : INT
    // varName : i
    // startType : INT (1)
    // endType : INT (100)
    // stepType : i+1 resultType INT
    // body {...}
    // FLOAT accepter

    SymbolTable currentTable = this.tables;
    this.tables = new SymbolTable(this.tables);

    forNode.varType.accept(this);
    String varType = forNode.varType.name;

    forNode.varId.accept(this);
    String varName = forNode.varId.name;

    tables.add(varName,varType);

    forNode.start.accept(this);
    String startType = this.type;
    forNode.end.accept(this);
    String endType = this.type;
    forNode.step.accept(this);
    String stepType = this.type;
    forNode.body.accept(this);

    if (!varType.equals("INT") || !startType.equals("INT")|| !endType.equals("INT") || !stepType.equals("INT")){
      throw new Exception("ErrorType : the type don't corresponds");

    }

    this.tables = currentTable;

  }

  @Override
  public void visit(IdentifierNode identifierNode) {

    // verifier que

  }

  @Override
  public void visit(IfNode ifNode) throws Exception {

    // if ( condition) { } else { }

    ifNode.condition.accept(this);
    ifNode.elseBlock.accept(this);
    ifNode.thenBlock.accept(this);

    if (!type.equals("BOOL")){
      throw new Exception("MissingConditionError ");
    }

  }

  @Override
  public void visit(NewArrayNode newArrayNode) throws Exception {
    newArrayNode.size.accept(this);

  }

  @Override
  public void visit(ParamNode paramNode) throws Exception {
    paramNode.id.accept(this);
    paramNode.type.accept(this);

    // verifier scope
    if (!tables.contains(paramNode.id.name)) {
      throw new Exception("ScopeError : never declared");

    }

    tables.add(paramNode.id.name,paramNode.type.name);

  }

  @Override
  public void visit(ReturnNode returnNode) throws Exception {
    // recuperer si j'envoie le return correct
    returnNode.expr.accept(this);

    if (!type.equals(returnType.name)){
      throw new Exception("ReturnError : not the typeReturn expected");
    }

  }

  @Override
  public void visit(TypeNode typeNode) {
    type = typeNode.name;


  }

  @Override
  public void visit(UnaryOpNode unaryOpNode) throws Exception {
    unaryOpNode.expr.accept(this);

    if (unaryOpNode.op.equals("-")) {
      if (type.equals("FLOAT") || type.equals("INT")){
        throw new Exception("OperatorError : '-' unary sign not associated with FLOAT or INT");
      }
    } else if (unaryOpNode.op.equals("not")) {
      if (type.equals("BOOL")){
        throw new Exception("OperatorError : '-' unary sign not associated with BOOL");
      }
    }else {
      throw new Exception("OperatorError : no unary sign found");
    }


  }
  @Override
  public void visit(VarRefNode varRefNode) throws Exception {


    if (!tables.contains(varRefNode.name)) {
      throw new Exception("ScopeError : never declared");

    }
    type = tables.getValue(varRefNode.name);

  }

  @Override
  public void visit(WhileNode whileNode) throws Exception {
    whileNode.body.accept(this);
    whileNode.condition.accept(this);

    if (!type.equals("BOOL")){
      throw new Exception("MissingConditionError ");
    }


  }

  // Decl
  @Override
  public void visit(VarDeclStmtNode varDeclStmtNode) throws Exception {
    if (tables.containsLocal(varDeclStmtNode.decl.id.name)){
      throw new Exception("ScopeError : the name already exist");
    }
    varDeclStmtNode.decl.accept(this);

  }

  @Override
  public void visit(VarDeclNode varDeclNode) {
    varDeclNode.type.accept(this);
    varDeclNode.id.accept(this);
    if (varDeclNode.init!=null){
      varDeclNode.init.accept(this);
    }

    tables.add(varDeclNode.id.toString(), varDeclNode.type.name);

  }

  // LITERAL
  @Override
  public void visit(BoolLiteralNode boolLiteralNode) {
      type = "BOOL" ;

  }
  @Override
  public void visit(FloatLiteralNode floatLiteralNode) {
    type = "FLOAT";

  }
  @Override
  public void visit(IntLiteralNode intLiteralNode) {
    type = "INT";

  }
  @Override
  public void visit(StringLiteralNode stringLiteralNode) {
    type = "String";

  }
// comme dans parser il faut check chaque node et lever une exception

}
