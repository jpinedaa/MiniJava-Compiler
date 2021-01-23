/**
 *	Needed for passing two different values between visits, due to parallel 
 *	execution of type-checking with intermediate code generation.
 */
public class ReturnItem {
	public String type;
	public String register;
	
	public ReturnItem(String type, String register) {
		this.type = type;
		this.register = register;
	}

}
