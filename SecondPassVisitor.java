
import generators.Generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import symboltable.Scope;
import symboltable.VariableEntry;
import syntaxtree.AllocationExpression;
import syntaxtree.AndExpression;
import syntaxtree.ArrayAllocationExpression;
import syntaxtree.ArrayAssignmentStatement;
import syntaxtree.ArrayLength;
import syntaxtree.ArrayLookup;
import syntaxtree.ArrayType;
import syntaxtree.AssignmentStatement;
import syntaxtree.Block;
import syntaxtree.BooleanType;
import syntaxtree.BracketExpression;
import syntaxtree.ClassDeclaration;
import syntaxtree.ClassExtendsDeclaration;
import syntaxtree.Clause;
import syntaxtree.CompareExpression;
import syntaxtree.Expression;
import syntaxtree.ExpressionList;
import syntaxtree.ExpressionTail;
import syntaxtree.ExpressionTerm;
import syntaxtree.FalseLiteral;
import syntaxtree.FormalParameter;
import syntaxtree.FormalParameterList;
import syntaxtree.FormalParameterTail;
import syntaxtree.FormalParameterTerm;
import syntaxtree.Goal;
import syntaxtree.Identifier;
import syntaxtree.IfStatement;
import syntaxtree.IntegerLiteral;
import syntaxtree.IntegerType;
import syntaxtree.MainClass;
import syntaxtree.MessageSend;
import syntaxtree.MethodDeclaration;
import syntaxtree.MinusExpression;
import syntaxtree.NotExpression;
import syntaxtree.PlusExpression;
import syntaxtree.PrimaryExpression;
import syntaxtree.PrintStatement;
import syntaxtree.Statement;
import syntaxtree.ThisExpression;
import syntaxtree.TimesExpression;
import syntaxtree.TrueLiteral;
import syntaxtree.Type;
import syntaxtree.TypeDeclaration;
import syntaxtree.VarDeclaration;
import syntaxtree.WhileStatement;
import visitor.GJDepthFirst;
import classinfo.ClassInfo;
import classinfo.MethodInfo;


public class SecondPassVisitor extends GJDepthFirst<ReturnItem, Scope> {
	
	public String finalSpigletCode;

	private Generator labels;
	private Generator registers;

 	private void emit(String toEmit) {
		this.finalSpigletCode += toEmit + '\n';

	}

	private void constructGlobalVTables() {

		for (Map.Entry<String, ClassInfo> entry : this.classes.entrySet()) {
			ClassInfo clazz = entry.getValue();
			List<String> methods = clazz.getMethods();

			String vTableLabel = clazz.name + "_vTable";
			String labelRegister = this.registers.next();
			emit("\t\tMOVE " + labelRegister + " " + vTableLabel);

			int vTableSize = methods.size()*4;
			String methodCodeRegister = this.registers.next();

			emit("\t\tMOVE " + methodCodeRegister + " HALLOCATE " + vTableSize);
			emit("\t\tHSTORE " + labelRegister + " 0 " + methodCodeRegister);

			int offset = 0;
			String methodReg = this.registers.next();
			for (String method : methods) {
				emit("\t\tMOVE " + methodReg + " " + method);
				emit("\t\tHSTORE " + methodCodeRegister + " " + offset + " " + methodReg);
				offset += 4;
			}
		}

	}

	private String adressRegister;
	private int argumentCounter;
	private List<String> argumentRegisters;
	private boolean justID;
	private boolean excludeFields;


	private Map<String, ClassInfo> classes;


	private List<String> tempParameters;


	private String lookupField(Scope scope, String fieldName) {

		String ret = scope.lookup(fieldName);
		if (ret != null)
			return ret;

		String className = scope.currentClassName;
		while (className != null) {
			ClassInfo superClass = this.classes.get(className);
			if (superClass.fields.containsKey(fieldName))
				return superClass.fields.get(fieldName).toString();

			className = this.classes.get(className).extendName;
		}
		return null;
	}

	private MethodInfo lookupMethod(String className, String methodName) {
		ClassInfo classInfo = this.classes.get(className);


		if (classInfo.methods.containsKey(methodName))
			return classInfo.methods.get(methodName);

		String extendName = classInfo.extendName;
		while (extendName != null) {
			classInfo = this.classes.get(extendName);
			if (classInfo.methods.containsKey(methodName))
				return classInfo.methods.get(methodName);
			extendName = classInfo.extendName;
		}
		return null;
	}


	public SecondPassVisitor(Map<String, ClassInfo> classes) {
		this.classes = classes;
		for (ClassInfo clazz : this.classes.values())
			clazz.initParent(classes);
		this.tempParameters = new ArrayList<String>();
		this.argumentRegisters = new ArrayList<String>();

		this.finalSpigletCode = "";
		this.labels = new Generator("Label ");

		this.registers = new Generator("NAME ");
	}



   	public ReturnItem visit(Goal n, Scope scope) throws Exception {
   		n.f0.accept(this, null);
   		for (int i = 0; i < n.f1.size(); i++) {
   			n.f1.elementAt(i).accept(this,null);
   		}
   		this.classes.clear();
   		return null;
   	}


   	public ReturnItem visit(MainClass n, Scope scope) throws Exception {
   		emit("Intermediate code for main: ");

//   		this.constructGlobalVTables();
			emit("Expr(");
   		Scope main = new Scope(null);
   		this.justID = true;
   		ReturnItem ret = n.f1.accept(this, null);
   		main.currentClassName = ret.type;
   		this.justID = false;

   		// Check variable declarations
   		for (int i = 0; i < n.f14.size(); i++)
 		   n.f14.elementAt(i).accept(this, main);

   		// Check statements
   		for (int i = 0; i < n.f15.size(); i++)
   			n.f15.elementAt(i).accept(this, main);

			emit(")");
   		main.clear();
   		emit("MAIN END");
   		return null;
   	}


   	public ReturnItem visit(TypeDeclaration n, Scope scope) throws Exception {
   		Scope typeDeclaration = new Scope(null);
   		n.f0.accept(this, typeDeclaration);
   		typeDeclaration.clear();
   		return null;
   	}


   public ReturnItem visit(ClassDeclaration n, Scope scope) throws Exception {
	   this.justID = true;
	   ReturnItem ret = n.f1.accept(this, null);
	   this.justID = false;

	   // Get class name
	   scope.currentClassName = ret.type;

	   // Get fields
	   this.excludeFields = true;
	   for (int i = 0; i < n.f3.size(); i++)
		   n.f3.elementAt(i).accept(this, scope);
	   this.excludeFields = false;

	   // Check methods
	   for (int i = 0; i < n.f4.size(); i++)
		   n.f4.elementAt(i).accept(this, scope);
	   return null;
   }


   public ReturnItem visit(ClassExtendsDeclaration n, Scope scope) throws Exception {
	   this.justID = true;
	   ReturnItem ret = n.f1.accept(this, null);
	   this.justID = false;

	   // Get class name
	   scope.currentClassName = ret.type;

	   // Check fields
	   this.excludeFields = true;
	   for (int i = 0; i < n.f5.size(); i++)
		   n.f5.elementAt(i).accept(this, scope);
	   this.excludeFields = false;

	   // Check methods
	   for (int i = 0; i < n.f6.size(); i++)
		   n.f6.elementAt(i).accept(this, scope);
	   return null;
   }

 
   public ReturnItem visit(VarDeclaration n, Scope scope) throws Exception {
	   	this.justID = true;
	   	ReturnItem retf0 = n.f0.accept(this, null);
   		String type = retf0.type;

   		ReturnItem retf1 = n.f1.accept(this, scope);
   		String varName = retf1.type;
   		this.justID = false;

   		if (!this.excludeFields) {
   			// Add entry to scope
	   		String newRegister = this.registers.next();
	   		VariableEntry toInsert = new VariableEntry(type, varName);
	   		toInsert.register = Integer.parseInt(newRegister.substring(5, newRegister.length()));
	   		scope.insert(toInsert);
   		}

   		return null;
   }


   	public ReturnItem visit(MethodDeclaration n, Scope scope) throws Exception {
	   Scope method = new Scope(scope);

	   this.justID = true;
	   String functionLabel = scope.currentClassName + "$" + n.f2.accept(this, scope).type;
	   this.justID = false;

	   // Get parameters
	   this.argumentCounter = 1;
	   if (n.f4.present())
		   n.f4.accept(this, method);
	   this.registers.reset(argumentCounter);


	   int argumentNo = this.argumentCounter;
	   this.argumentCounter = -1;
	   emit('\n' + functionLabel);
	   emit("BEGIN");

	   // Check variable declarations
	   for (int i = 0; i < n.f7.size(); i++)
		   n.f7.elementAt(i).accept(this, method);


	   // Check statements
	   for (int i = 0; i < n.f8.size(); i++)
		   n.f8.elementAt(i).accept(this, method);


	   ReturnItem retf10 = n.f10.accept(this, method);
	   emit("RETURN");
	   emit("\t\t" + retf10.register);
	   emit("END");

	   method.clear();
	   return null;
   	}

   public ReturnItem visit(FormalParameterList n, Scope scope) throws Exception {
	   n.f0.accept(this, scope);
	   n.f1.accept(this, scope);
	   return null;
   }

   public ReturnItem visit(FormalParameter n, Scope scope) throws Exception {
	   	this.justID = true;
	   	ReturnItem retf0 = n.f0.accept(this, null);
	   	String type = retf0.type;

	   	ReturnItem retf1 = n.f1.accept(this, null);
	   	String name = retf1.type;
	   	this.justID = false;

	   	VariableEntry toInsert = new VariableEntry(type, name);
	   	toInsert.register = this.argumentCounter++;
	   	scope.insert(toInsert);

	   	return null;
   }


   public ReturnItem visit(FormalParameterTail n, Scope scope) throws Exception {
	   	for (int i = 0; i < n.f0.size(); i++)
	   		n.f0.elementAt(i).accept(this, scope);
	   	return null;
   }

  
   public ReturnItem visit(FormalParameterTerm n, Scope scope) throws Exception {
	   n.f1.accept(this, scope);

	   return null;
   }


   	public ReturnItem visit(Type n, Scope scope) throws Exception {
   		return  n.f0.accept(this, null);
   	}

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   	public ReturnItem visit(ArrayType n, Scope scope) throws Exception {
      	return new ReturnItem("INT_ARRAY", null);
   	}

   /**
    * f0 -> "boolean"
    */
   	public ReturnItem visit(BooleanType n, Scope scope) throws Exception {
     	 return new ReturnItem("BOOLEAN", null);
   	}

   /**
    * f0 -> "int"
    */
   	public ReturnItem visit(IntegerType n, Scope scope) throws Exception {
   		return new ReturnItem("INT", null);
   	}

   /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
   public ReturnItem visit(Statement n, Scope scope) throws Exception {
	   n.f0.accept(this, scope);
	   return null;
   }

   /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
   public ReturnItem visit(Block n, Scope scope) throws Exception {
	   for (int i = 0; i < n.f1.size(); i++)
		   n.f1.elementAt(i).accept(this, scope);
	   return null;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public ReturnItem visit(AssignmentStatement n, Scope scope) throws Exception {

	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	String register = retf0.register;
	   	String adress = this.adressRegister;

	   	ReturnItem retf2 = n.f2.accept(this, scope);
	   	String expr = retf2.register;

	   	if (adress == null)
	   		emit("\t\tAssignmentStatement{" + register + " " + expr);
	   	else // Store if it an objects field
	   		emit("\t\tHSTORE " + adress + " 0 " + expr);

	   	return new ReturnItem(null, null);
   }

   /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   public ReturnItem visit(ArrayAssignmentStatement n, Scope scope) throws Exception {

	   	String memory = this.registers.next();

	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	String arrayPtr = retf0.register;
	   	// Get size
	   	String size = this.registers.next();
	   	emit("\t\tArrayAssignmentStatement " + size + " " + arrayPtr + " 0");

	   	ReturnItem retf2 = n.f2.accept(this, scope);
	   	String arrayOffset = retf2.register;
	   	// Check if offset is valid
	   	String interLabel = this.labels.next();
	   	String checkLabel = this.labels.next();
	   	String bool1 = this.registers.next();
	   	emit("\t\tMOVE " + bool1 + " LT " + arrayOffset + " 0");
	   	emit("\t\tCJUMP " + bool1 + " " + interLabel);
	   	emit("\t\tERROR");
	   	emit("\t\tMOVE " + bool1 + " LT " + arrayOffset + size);
	   	String one = this.registers.next();
	   	emit("\t\tMOVE " + one + " 1");
	   	emit("\t\tMOVE " + bool1 + " MINUS " + one + " " + bool1);
	   	emit(interLabel + "\t\tCJUMP " + bool1 + " " + checkLabel);
	   	emit("\t\tERROR");
	   	emit(checkLabel + "\t\tNOOP");

	   	// Get memory location
	   	String arrayOffset2 = this.registers.next();
	   	emit("\t\tMOVE " + arrayOffset2 + " TIMES " + arrayOffset + " 4");
	   	emit("\t\tMOVE " + memory + " PLUS " + arrayPtr + " " + arrayOffset2);

	   	// Store in memory
	   	ReturnItem retf5 = n.f5.accept(this, scope);
	   	emit("\t\tHSTORE " + memory + " 4 " + retf5.register);

	   	return null;
   }

   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   public ReturnItem visit(IfStatement n, Scope scope) throws Exception {

	   	String elsePart = this.labels.next();
	   	String end = this.labels.next();

	   	ReturnItem retf2 = n.f2.accept(this, scope);
	   	//emit("\t\tCJUMP " + retf2.register + " " + elsePart);
			emit("\t\tIF( " + retf2.register + " ){");
	   	n.f4.accept(this, scope);
	   //	emit("\t\tJUMP " + end);
		 //emit("")

	   	//emit(elsePart + "\t\tNOOP");
			emit("\t\tElse{");
	   	n.f6.accept(this, scope);
	   	emit(end + "\t\t}");


	   	return null;
   }

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public ReturnItem visit(WhileStatement n, Scope scope) throws Exception {

	   	String start = this.labels.next();
	   	String end = this.labels.next();
	   	emit(start + "\t\tNOOP");

	   	ReturnItem retf2 = n.f2.accept(this, scope);
	   	emit("\t\tCJUMP " + retf2.register + " " + end);

	   	n.f4.accept(this, scope);
	   	emit("\t\tJUMP " + start);

	   	emit(end + "\t\tNOOP");

	   	return null;
   }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public ReturnItem visit(PrintStatement n, Scope scope) throws Exception {
	   	ReturnItem retf2 = n.f2.accept(this, scope);

	   	String register = retf2.register;
	   	emit("\t\t" + register + "  Println");

	   	return null;
   }

   /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
   public ReturnItem visit(Expression n, Scope scope) throws Exception {
	   	return n.f0.accept(this, scope);
   }

   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public ReturnItem visit(AndExpression n, Scope scope) throws Exception {
	   	String label = this.labels.next();
	   	String ret = this.registers.next();

	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	emit("\t\tMOVE " + ret + " " + retf0.register);
	   	emit("\t\tCJUMP " + ret + " " + label);

	   	ReturnItem retf2 = n.f2.accept(this, scope);
	   	emit("\t\tMOVE " + ret + " " + retf2.register);

	   	emit(label + "\t\tNOOP");

	   	return new ReturnItem("BOOLEAN", ret);
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public ReturnItem visit(CompareExpression n, Scope scope) throws Exception {
	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	ReturnItem retf2 = n.f2.accept(this, scope);

	   	String register1 = retf0.register, register2 = retf2.register;
	   	String newRegister = this.registers.next();
			emit("\t\tPrimaryExpression Lessthen{");
	   	emit("\t\t\t" + newRegister + " LessThan " + register1 + " " + register2);
			emit("\t\t}");
	   	return new ReturnItem("BOOLEAN", newRegister);
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public ReturnItem visit(PlusExpression n, Scope scope) throws Exception {
	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	ReturnItem retf2 = n.f2.accept(this, scope);

	   	String register1 = retf0.register, register2 = retf2.register;
	   	String newRegister = this.registers.next();
	   	emit("\t\tMOVE " + newRegister + " PLUS " + register1 + " " + register2);

	   	return new ReturnItem("INT", newRegister);
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public ReturnItem visit(MinusExpression n, Scope scope) throws Exception {
	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	ReturnItem retf2 = n.f2.accept(this, scope);

	   	String register1 = retf0.register, register2 = retf2.register;
	   	String newRegister = this.registers.next();
	   	emit("\t\t" + newRegister + " MINUS " + register1 + " " + register2);

	   	return new ReturnItem("INT", newRegister);
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public ReturnItem visit(TimesExpression n, Scope scope) throws Exception {
	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	ReturnItem retf2 = n.f2.accept(this, scope);

	   	String register1 = retf0.register, register2 = retf2.register;
	   	String newRegister = this.registers.next();
	   	emit("\t\t" + newRegister + " TIMES " + register1 + " " + register2);

	   	return new ReturnItem("INT", newRegister);
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public ReturnItem visit(ArrayLookup n, Scope scope) throws Exception {
	   	String ret = this.registers.next();
	   	String checkLabel = this.labels.next();
	   	String interLabel = this.labels.next();

	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	String array = retf0.register;
	   	// Get size
	   	String size = this.registers.next();
	   	emit("\t\tHLOAD " + size + " " + array + " 0");

	   	ReturnItem retf2 = n.f2.accept(this, scope);
	   	String arrayOffset = retf2.register;

	   	// Check if offset is valid
	   	String bool1 = this.registers.next();
	   	emit("\t\tMOVE " + bool1 + " LT " + arrayOffset + " 0");
	   	emit("\t\tCJUMP " + bool1 + " " + interLabel);
	   	emit("\t\tERROR");
	   	emit(interLabel + "\t\tNOOP");
	   	emit("\t\tMOVE " + bool1 + " LT " + arrayOffset + " " + size);
	   	String one = this.registers.next();
	   	emit("\t\tMOVE " + one + " 1");
	   	emit("\t\tMOVE " + bool1 + " MINUS " + one + " " + bool1);
	   	emit("\t\tCJUMP " + bool1 + " " + checkLabel);
	   	emit("\t\tERROR");
	   	emit(checkLabel + "\t\tNOOP");

	   	// Lookup
	   	String offset = this.registers.next();
	   	emit("\t\tMOVE " + offset + " TIMES " + arrayOffset + " 4");
	   	emit("\t\tMOVE " + offset + " PLUS " + array + " " + offset);
	   	emit("\t\tHLOAD " + ret + " " + offset + " 4");

	   	return new ReturnItem("INT", ret);
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public ReturnItem visit(ArrayLength n, Scope scope) throws Exception {
	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	String array = retf0.register;
	   	String length = this.registers.next();
	   	emit("\t\tHLOAD" + length + " " + array + " 0");
	   	return new ReturnItem("INT", length);
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public ReturnItem visit(MessageSend n, Scope scope) throws Exception {
	   	// Get object
	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	String className = retf0.type;
	   	String object = retf0.register;
	   	ClassInfo clazz = this.classes.get(className);

//	   	// Check if object is null
//	   	String bool1 = this.registers.next();
//	   	String checkLabel = this.labels.next();
//	   	emit("\t\tMOVE " + bool1 + " LT " + object + " 1");
//	   	emit("\t\tCJUMP " + bool1 + " " + checkLabel);
//	   	emit("\t\tERROR");
//	   	emit(checkLabel + "\t\tNOOP");

	   	// Get method
	   	this.justID = true;
	   	ReturnItem retf2 = n.f2.accept(this, null);
	   	String method = retf2.type;
			//emit(method);
	   	MethodInfo methodInfo = this.lookupMethod(className, method);
	   	this.justID = false;

	   	// Find offset in vTable
	   	List<String> methods = clazz.getMethods();
	   	int offset = 0;
	   	for (String meth : methods) {
	   		int i;
	   		for (i = 0; i < meth.length(); i++)
	   			if (meth.charAt(i) == '_') break;
	   		i++;
	   		String simple = meth.substring(i, meth.length());
	   		if (simple.compareTo(method) == 0) break;
	   		offset += 4;
	   	}
			//String functionLabel = scope.currentClassName ;// "_" + n.f2.accept(this, scope).type;
	   	// Load vTable
	   	String vTable = this.registers.next();
	   	emit("\t\tVariableLoad " + vTable + " " + object + "");
	   	// Get method label
	   	String methodLabel = this.registers.next();
	   	emit("\t\tVariableLoad " + methodLabel + " " + vTable + " " + offset);

	   	String callStatement = "CALL " + methodLabel + "(" + object + " ";
	   	// Get arguments
	   	if (n.f4.present()) {
	   		n.f4.accept(this, scope);
	   		for (String register : this.argumentRegisters)
	   			callStatement += register + " ";

	   		this.tempParameters.clear();
	   		this.argumentRegisters.clear();
	   	}
	   	callStatement = callStatement.substring(0, callStatement.length() - 1);
	   	callStatement += ")";

	   	String returnReg = this.registers.next();
	   	//emit("\t\tMOVE " + returnReg + " " + callStatement);
			emit("\t\tCALL " + scope.currentClassName + "$" + method );


	   	return new ReturnItem(methodInfo.returnType.toString(), returnReg);
   }

   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public ReturnItem visit(ExpressionList n, Scope scope) throws Exception {
	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	this.tempParameters.add(retf0.type);
	   	this.argumentRegisters.add(retf0.register);
      	n.f1.accept(this, scope);
      	return null;
   }

   /**
    * f0 -> ( ExpressionTerm() )*
    */
   public ReturnItem visit(ExpressionTail n, Scope scope) throws Exception {
	   	for (int i = 0; i < n.f0.size(); i++) {
	   		ReturnItem retfi = n.f0.elementAt(i).accept(this, scope);
	   		this.tempParameters.add(retfi.type);
	   		this.argumentRegisters.add(retfi.register);
	   	}

	   	return null;
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public ReturnItem visit(ExpressionTerm n, Scope scope) throws Exception {
	   	return n.f1.accept(this, scope);
   }

   /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
   public ReturnItem visit(Clause n, Scope scope) throws Exception {
	   return n.f0.accept(this, scope);
   }

   /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
   public ReturnItem visit(PrimaryExpression n, Scope scope) throws Exception {
	   	ReturnItem retf0 = n.f0.accept(this, scope);
	   	String var = retf0.type;
	   	String type = "";
	   	if (n.f0.which == 3) { // IDENTIFIER
	   		// Check if variable has been declared
	   		type = this.lookupField(scope, var);
	   		var = type;
	   	}

	   	return new ReturnItem(var, retf0.register);
   }

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public ReturnItem visit(IntegerLiteral n, Scope scope) throws Exception {
	   	String newRegister = this.registers.next();
	   	emit("\t\tINTEGER_LITERAL  " + n.f0.toString());

	   	return new ReturnItem("INT", newRegister);
   }

   /**
    * f0 -> "true"
    */
   public ReturnItem visit(TrueLiteral n, Scope scope) throws Exception {
	   	String newRegister = this.registers.next();
	   	emit("\t\tTRUE " + 1);

	   	return new ReturnItem("BOOLEAN", newRegister);
   }

   /**
    * f0 -> "false"
    */
   public ReturnItem visit(FalseLiteral n, Scope scope) throws Exception {
	   	String newRegister = this.registers.next();
	   	emit("\t\tFALSE " + 0);

	   	return new ReturnItem("BOOLEAN", newRegister);
   }

   /**
    * f0 -> <IDENTIFIER>
    */
   	public ReturnItem visit(Identifier n, Scope scope) throws Exception {
   		String identifier = n.f0.toString();
   		if (this.justID)
   			return new ReturnItem(identifier, null);

   		// Get register holding variable
   		int registerNo = scope.lookupRegister(identifier);
   		String register = null;
   		if (registerNo == -1) {
   			// Get register from class object(this)
   			String classname = scope.currentClassName;
   			ClassInfo clazz = this.classes.get(classname);
   			int fieldPos = clazz.getFieldPosition(identifier);
   			if (fieldPos != -1) {
   				String address = this.registers.next();
   				register = this.registers.next();
   				emit("\t\tMOVE " + address + " PLUS TEMP 0 " + (fieldPos*4));
   				this.adressRegister = address;
   				emit("\t\tHLOAD " + register + " " + address + " 0");
   			}
   			else
   				throw new SemanticException("WHAT?", 0, 0);
   		}
   		else {
   			register = "TEMP " + registerNo;
   			this.adressRegister = null;
   		}
 	   	return new ReturnItem(identifier, register);
   	}

   /**
    * f0 -> "this"
    */
   public ReturnItem visit(ThisExpression n, Scope scope) throws Exception {
	   	return new ReturnItem(scope.currentClassName, "TEMP 0");
   }

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public ReturnItem visit(ArrayAllocationExpression n, Scope scope) throws Exception {
	   ReturnItem retf3 = n.f3.accept(this, scope);

	   String size = retf3.register;

	   // Check if size is >= 0
	   String check = this.registers.next();
	   emit("\t\tMOVE " + check + " LT " + size + " 1");
	   String label = this.labels.next();
	   emit("\t\tCJUMP " + check + " " + label);
	   emit("\t\tERROR");
	   emit(label + "\t\tNOOP");

	   // Allocate array
	   String size2 = this.registers.next();
	   emit("\t\tMOVE " + size2 + " PLUS " + size + " 1");
	   emit("\t\tMOVE " + size2 + " TIMES " + size2 + " 4");
	   String array = this.registers.next();
	   emit("\t\tMOVE " + array + " HALLOCATE " + size2);
	   // Store size at first position
	   emit("\t\tHSTORE " + array + " 0 " + size);

	   String bool1 = this.registers.next();
	   emit("\t\tMOVE " + bool1 + " 1");
	   String arrayPtr = this.registers.next();
	   emit("\t\tMOVE " + arrayPtr + " PLUS " + array + " 4");
	   String one = this.registers.next();
	   emit("\t\tMOVE " + one + " 1");
	   String zero = this.registers.next();
	   emit("\t\tMOVE " + zero + " 0");

	   // Initialize all elements to zero
	   String startLabel = this.labels.next();
	   String endLabel = this.labels.next();
	   emit(startLabel + "\t\tCJUMP " + bool1 + " " + endLabel);
	   emit("\t\tHSTORE " + arrayPtr + " 0 " + zero);
	   emit("\t\tMOVE " + arrayPtr + " PLUS " + arrayPtr + " 4");
	   emit("\t\tMOVE " + size + " MINUS " + size + " 1");
	   emit("\t\tMOVE " + bool1 + " LT " + size + " 1");
	   emit("\t\tMOVE " + bool1 + " MINUS " + one + " " + bool1);
	   emit("\t\tJUMP " + startLabel);
	   emit(endLabel + "\t\tNOOP");

	   return new ReturnItem("INT_ARRAY", array);
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public ReturnItem visit(AllocationExpression n, Scope scope) throws Exception {
	   	this.justID = true;
	   	ReturnItem retf1 = n.f1.accept(this, scope);
	   	String className = retf1.type;
	   	this.justID = false;

	   	ClassInfo clazz = this.classes.get(className);

		// Allocate object
	   	int size = clazz.getFieldNumber();
	   	String objectReg = this.registers.next();
	   	//emit("\t\tMOVE " + objectReg + " HALLOCATE " + ((size+1)*4));
			emit("\t\tNew " + className);
	   	String vTablePtrReg = this.registers.next();
	   //	emit("\t\tMOVE " + vTablePtrReg + " " + className + "_vTable");
	   	String vTableReg = this.registers.next();
	  // 	emit("\t\tHLOAD " + vTableReg + " " + vTablePtrReg + " 0");
	 //  	emit("\t\tHSTORE " + objectReg + " 0 " + vTableReg);
	   	String zero = this.registers.next();
	  // 	emit("\t\tMOVE " + zero + " 0");
	   	int offset = 4;
	   	for (int i = 0; i < size; i++) {
	   	//	emit("\t\tHSTORE " + objectReg + " " + offset + " " + zero);
	   		offset += 4;
	   	}

	   	// Construct vTable
		List<String> methods = clazz.getMethods();
		// Get size of vTable
		int vTableSize = methods.size()*4; // 4bytes per address
		String methodCodeRegister = this.registers.next();
		// Allocate vTable
	//	emit("\t\tMOVE " + methodCodeRegister + " HALLOCATE " + vTableSize);
	//	emit("\t\tHSTORE " + objectReg + " 0 " + methodCodeRegister);
		// Store method code in vTable
		offset = 0;
		String methodReg = this.registers.next();
		for (String method : methods) {
		//	emit("\t\tMOVE " + methodReg + " " + method);
		//	emit("\t\tHSTORE " + methodCodeRegister + " " + offset + " " + methodReg);
			offset += 4;
		}




	   	return new ReturnItem(className, objectReg);
   }

   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public ReturnItem visit(NotExpression n, Scope scope) throws Exception {
	   	ReturnItem retf1 = n.f1.accept(this, scope);
	   	String one = this.registers.next();
	   	emit("\t\tMOVE " + one + " 1");
	   	emit("\t\tMOVE " + one + " MINUS " + one + " " + retf1.register);

	   	return new ReturnItem("BOOLEAN", one);
   }

   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public ReturnItem visit(BracketExpression n, Scope scope) throws Exception {
	   return n.f1.accept(this, scope);
   }
}
