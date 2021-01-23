package symboltable;

public class VariableEntry {
	public String name;
	public String type;
	public int register;

	public VariableEntry(String type, String name) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		return this.type.toString() + ": " + this.name;
	}
}