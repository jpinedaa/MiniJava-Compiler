import java.util.LinkedHashMap;
import java.util.Map;

import syntaxtree.ArrayType;
import syntaxtree.BooleanType;
import syntaxtree.ClassDeclaration;
import syntaxtree.ClassExtendsDeclaration;
import syntaxtree.FormalParameter;
import syntaxtree.FormalParameterList;
import syntaxtree.FormalParameterTail;
import syntaxtree.FormalParameterTerm;
import syntaxtree.Goal;
import syntaxtree.Identifier;
import syntaxtree.IntegerType;
import syntaxtree.MainClass;
import syntaxtree.MethodDeclaration;
import syntaxtree.Type;
import syntaxtree.TypeDeclaration;
import syntaxtree.VarDeclaration;
import visitor.GJDepthFirst;
import classinfo.ClassInfo;
import classinfo.FieldInfo;
import classinfo.MethodInfo;
import classinfo.VariableType;

/**
* Performs a first pass gathering information of all classes declared in the input program and checking shallow semantic errors.
* 	ERROR CHECKING
* 	- Extends class not previously declared
* 	- Field uniqueness
* 	- Method uniqueness
* 	- Class uniqueness
* 	- Method from parent must agree on return type and parameters
*/
public class FirstPassVisitor extends GJDepthFirst<String, ClassInfo> {
	public Map<String, ClassInfo> classes;
	private int lineNumber;
	private int columnNumber;
 
    public FirstPassVisitor() {
        this.classes = new LinkedHashMap<String, ClassInfo>();
        this.lineNumber = 1;
        this.columnNumber = 1;
    }

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, ClassInfo classInfo) throws Exception {
    	n.f0.accept(this, null);
    	
        for (int i = 0; i < n.f1.size(); i++) {
        	// Create ClassInfo entry
        	ClassInfo newClass = new ClassInfo();
        	n.f1.elementAt(i).accept(this, newClass);
        }
        return null;
    }


    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    public String visit(MainClass n, ClassInfo classInfo) throws Exception {
    	String name = n.f1.accept(this, null);
    	ClassInfo main = new ClassInfo();
    	main.name = name;    	
    	main.extendName = null;
    	
		//System.out.println(name);
		
    	this.classes.put(name, main);
    	return null;
    }

    /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    public String visit(TypeDeclaration n, ClassInfo classInfo) throws Exception {
        n.f0.accept(this, classInfo);
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public String visit(ClassDeclaration n, ClassInfo classInfo) throws Exception {
        // Get class name
    	String className = n.f1.accept(this, null);
    	classInfo.name = className;
    	classInfo.extendName = null;
    	
    	// Check class uniqueness
    	if (this.classes.containsKey(className))
    		throw new SemanticException("Class '" + className + "' has already been declared", this.lineNumber, this.columnNumber);
    	
    	// Check fields
    	for (int i = 0; i < n.f3.size(); i++)
    		n.f3.elementAt(i).accept(this, classInfo);
    	
    	// Check methods
    	for (int i = 0; i < n.f4.size(); i++)
    		n.f4.elementAt(i).accept(this, classInfo);
    	
    	this.classes.put(className, classInfo);
		
		//System.out.println(className);
    	
    	return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    public String visit(ClassExtendsDeclaration n, ClassInfo classInfo) throws Exception {
        // Get class name
    	String className = n.f1.accept(this, null);
    	
    	// Get extend class name
    	String extendName = n.f3.accept(this, null);
    	
    	// Check if class extends class not previously declared
    	if (!(this.classes.containsKey(extendName)))
    		throw new SemanticException("Class '" + className + "' extends class not previously declared ('" + extendName + "')", this.lineNumber, this.columnNumber);
    	
    	// Update class info
    	classInfo.name = className;
    	classInfo.extendName = extendName;
    	this.classes.put(className, classInfo);
    
    	for (int i = 0; i < n.f5.size(); i++)
    		n.f5.elementAt(i).accept(this, classInfo);
    	for (int i = 0; i < n.f6.size(); i++)
    		n.f6.elementAt(i).accept(this, classInfo);
    	
    	return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, ClassInfo classInfo) throws Exception {
    	// Get field type
    	String typeName = n.f0.accept(this, null);
    	FieldInfo type;
    	switch(typeName) {
    	case "INT":
    		type = new FieldInfo(null, VariableType.INT);
    		break;
    	case "INT_ARRAY":
    		type = new FieldInfo(null, VariableType.INT_ARRAY);
    		break;
    	case "BOOLEAN":
    		type = new FieldInfo(null, VariableType.BOOLEAN);
    		break;
    	default:
    		type = new FieldInfo(typeName, VariableType.USER_DEFINED);
    	}
    	
    	// Get field name 
    	String name = n.f1.accept(this, null);
    	
    	// Check field uniqueness
    	if (classInfo.fields.containsKey(name))
    		throw new SemanticException("Field '" + name + "' previously declared", this.lineNumber, this.columnNumber);
    	
    	// Update class info
    	classInfo.fields.put(name, type);
		
		//System.out.println(typeName + "test " + name);
    	
    	return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, ClassInfo classInfo) throws Exception {
    	// Get return type
    	String typeName = n.f1.accept(this, null);
    	FieldInfo type;
    	switch(typeName) {
    	case "INT":
    		type = new FieldInfo(null, VariableType.INT);
    		break;
    	case "INT_ARRAY":
    		type = new FieldInfo(null, VariableType.INT_ARRAY);
    		break;
    	case "BOOLEAN":
    		type = new FieldInfo(null, VariableType.BOOLEAN);
    		break;
    	default:
    		type = new FieldInfo(typeName, VariableType.USER_DEFINED);
    	}
    	
    	MethodInfo newMethod = new MethodInfo();
    	newMethod.returnType = type;
    	
    	// Get method name
    	newMethod.name = n.f2.accept(this, null);
    	
    	
    	// Check method uniqueness
    	if (classInfo.methods.containsKey(newMethod.name)) 
    		throw new SemanticException("Method '" + newMethod.name + "' previously declared", this.lineNumber, this.columnNumber);
    	
    	// Get parameters
    	String parameters = null;
    	if (n.f4.present()) 
    		parameters = n.f4.accept(this, null);
    	
    	// Parse parameters
    	if (parameters != null) {
    		String parameterArray[] = parameters.split("\\s+");
    		for (String parameter : parameterArray) {
    			FieldInfo parType = null;
    	    	switch(parameter) {
    	    	case "INT":
    	    		parType = new FieldInfo(null, VariableType.INT);
    	    		break;
    	    	case "INT_ARRAY":
    	    		parType = new FieldInfo(null, VariableType.INT_ARRAY);
    	    		break;
    	    	case "BOOLEAN":
    	    		parType = new FieldInfo(null, VariableType.BOOLEAN);
    	    		break;
    	    	default:
    	    		parType = new FieldInfo(parameter, VariableType.USER_DEFINED);
    	    	}
    	    	newMethod.parameters.add(parType);
    		}
    	}
    	
    	// Check if method overrides super class method	
		String currentClassName = classInfo.extendName;
		while (currentClassName != null) {
			ClassInfo currentClass = this.classes.get(currentClassName);
			if (currentClass.methods.containsKey(newMethod.name)) {
				MethodInfo superMethod = currentClass.methods.get(newMethod.name);
				boolean correctOverride = true;
				if ((newMethod.returnType.type != superMethod.returnType.type)
				|| (newMethod.parameters.size() != superMethod.parameters.size()))
					correctOverride = false;
				
				if (correctOverride) {
					for (int i = 0; i < newMethod.parameters.size(); i++) 
						if (newMethod.parameters.get(i).type != superMethod.parameters.get(i).type)
							correctOverride = false;
				}
				if (!correctOverride)
					throw new SemanticException("Function overrides super class method with different prototype", this.lineNumber, this.columnNumber);
			}
			currentClassName = currentClass.extendName;
		}
 
    	
    	
    	
    	// Update class info
    	classInfo.methods.put(newMethod.name, newMethod);
		
		//System.out.println(typeName + " " + newMethod.name );
    	
    	return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterList n, ClassInfo classInfo) throws Exception {
    	// Get paramater
    	String parameter = n.f0.accept(this, null);
    	// Get parameter tail
    	String parameterTail = n.f1.accept(this, null);
    	
		//System.out.println(parameter + parameterTail );
		
    	return parameter + " " + parameterTail;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, ClassInfo classInfo) throws Exception {
       return n.f0.accept(this, null);
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public String visit(FormalParameterTail n, ClassInfo classInfo) throws Exception {
    	String ret = "";
    	for (int i = 0; i < n.f0.size(); i++)
    		ret += n.f0.elementAt(i).accept(this, null) + " ";
    	return ret;
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public String visit(FormalParameterTerm n, ClassInfo classInfo) throws Exception {
    	return n.f1.accept(this, null);
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public String visit(Type n, ClassInfo classInfo) throws Exception {
       return n.f0.accept(this, classInfo);
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n, ClassInfo classInfo) throws Exception {       
    	return "INT_ARRAY";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, ClassInfo classInfo) throws Exception {
 	   	this.lineNumber = n.f0.beginLine;
 	   	this.columnNumber = n.f0.beginColumn;
    	return "BOOLEAN";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, ClassInfo classInfo) throws Exception {
    	return "INT";
    }
    
    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, ClassInfo classInfo) throws Exception {
 	   	this.lineNumber = n.f0.beginLine;
 	   	this.columnNumber = n.f0.beginColumn;	
        return n.f0.toString();
    }
}
