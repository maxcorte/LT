public class TestSemanticAnalysis {
//tests for each case :
  //TypeError
  String typeError = " x = 0- \"a\" ";
  //CollectionError
  String CollectionError = " coll person {\n"
      + "    STRING name;\n"
      + "}\n ";
  //OperatorError
  String OperatorError = " -\"stringword\" ";
  //ArgumentError
  String ArgumentError = " ";
  //MissingConditionError
  String MissingConditionError = " if(int i = 01) {return true; }";
  //ReturnError
  String ReturnError = " ";

  //ScopeError
  String ScopeError = "     for (i; 1 -> 100.0; i+1) {\n"
      + "int x = 0}"
      + "x = x+1";

}
