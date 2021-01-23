package classinfo;

public enum VariableType {
	INT,
	BOOLEAN,
	INT_ARRAY,
	USER_DEFINED;

	@Override
	public String toString() {
		switch(this) {
		case INT:
			return "INT";
		case BOOLEAN:
			return "BOOLEAN";
		case INT_ARRAY:
			return "INT_ARRAY";
		case USER_DEFINED:
			return "USER_DEFINED";
		default:
			return "UNKNOWN";
		}
	}
}