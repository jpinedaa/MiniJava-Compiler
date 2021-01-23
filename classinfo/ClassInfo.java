package classinfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClassInfo {
	
	public String name;
	public String extendName;
	public ClassInfo parent;
	
	public Map<String, FieldInfo> fields;
	public Map<String, MethodInfo> methods;
	
	public ClassInfo() {
		this.fields = new LinkedHashMap<String, FieldInfo>();
		this.methods = new LinkedHashMap<String, MethodInfo>();
	}
	
	public List<String> getMethods() {
		List<String> ret = new ArrayList<String>();
		Set<String> overridden = new HashSet<String>();
		if (this.parent != null) {
			List<String> parentMethods = this.parent.getMethods();
			// Override parent methods
			for (String parentMethod : parentMethods) {
				String simple = parentMethod.substring(this.parent.name.length() + 1, parentMethod.length());
				if (this.methods.containsKey(simple)) {
					parentMethods.set(parentMethods.indexOf(parentMethod), parentMethod.replaceFirst(this.parent.name, this.name));
					overridden.add(simple);
				}
			}
			ret.addAll(parentMethods);
		}
		
		for (Entry<String, MethodInfo> entry : this.methods.entrySet()) {
			String method = entry.getKey();
			if (!overridden.contains(method))
				ret.add(this.name + "_" + method);
		}
		
		return ret;
	}
	
	public int getFieldNumber() {
		int fieldNo = 0;
		if (this.parent != null)
			fieldNo = this.parent.getFieldNumber();
		fieldNo += this.fields.size();
		return fieldNo;
	}
	
	public int getFieldPosition(String fieldname) {
		int ret = -1;
		if (this.fields.containsKey(fieldname)) {
			// Get index
			int index = 1;
			for (Map.Entry<String, FieldInfo> entry : this.fields.entrySet()) {
				if (entry.getKey() == fieldname)
					break;
				index++;
			}
			
			if (this.parent != null)
				ret = index + this.parent.getFieldNumber();
			else 
				ret = index;
		}
		else 
			ret = this.parent.getFieldPosition(fieldname);
		
		return ret;
	}
	
	
	public void initParent(Map<String, ClassInfo> classes) {
		if (this.extendName != null) 
			this.parent = classes.get(this.extendName);
	}
	
	public void print() {
		System.out.println("CLASS: " + this.name);
		System.out.println("    extends " + this.extendName);
		System.out.println("    FIELDS:");
		for (Map.Entry<String, FieldInfo> entry : this.fields.entrySet()) 
			System.out.println("           " + entry.getKey() + " -> " + entry.getValue().toString());
		System.out.println("    METHODS:");
		for (Map.Entry<String, MethodInfo> entry : this.methods.entrySet()) 
			System.out.println("           " + entry.getKey() + " -> " + entry.getValue().toString());
	}
}
