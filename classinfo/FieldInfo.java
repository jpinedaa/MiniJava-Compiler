package classinfo;

public class FieldInfo {
	public String typeName;
	public VariableType type;
	
	public FieldInfo(String name, VariableType type) {
		this.typeName = name;
		this.type = type;
	}
	
	@Override
	public String toString() {
		if (typeName != null) 
			return this.typeName;
		else
			return this.type.toString();
	}
}
