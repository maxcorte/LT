package compiler.SemanticAnalysis;

import compiler.Lexer.Lexer.Sym;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SymbolTable {

  private SymbolTable previousTable;
  private HashMap<String,  String> symboltbl;

  public SymbolTable(SymbolTable symbolTable) {
    this.symboltbl = new HashMap<>();
    this.previousTable = symbolTable;
  }

  public void add(String identifier, String type){
    symboltbl.put(identifier, type);
  }

  public boolean containsLocal(String identifier){
    return symboltbl.containsKey(identifier);
  }

  public boolean containsParent(String identifier){
    if (previousTable==null) {
      return false;
    }
    return previousTable.symboltbl.containsKey(identifier);
  }
  public boolean contains(String identifier){

    if (symboltbl.containsKey(identifier)){
      return  true;
    }
    if (previousTable!=null){
      previousTable.contains(identifier);
    }

    return false;
  }

  public String getValue(String identifier) {
    if (symboltbl.containsKey(identifier)){
      return symboltbl.get(identifier);
    }
    if (previousTable != null){
      previousTable.getValue(identifier);
    }

    return null;
  }
}
