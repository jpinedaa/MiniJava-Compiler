package classinfo;

import java.util.ArrayList;
import java.util.List;

public class MethodInfo {

	public String name;
	public FieldInfo returnType;
	public List<FieldInfo> parameters;
	
	public MethodInfo() {
		this.parameters = new ArrayList<FieldInfo>();
	}
	
	@Override
	public String toString() {
		String ret = this.returnType + " " + this.name + "(";
		for (FieldInfo parameter : parameters)
			ret += parameter.toString() + ", ";
		if (!parameters.isEmpty())
			ret = ret.substring(0, ret.length() - 2);
		return ret + ")";
	}

}
