
public class SemanticException extends Exception {

	private static final long serialVersionUID = 1L;

	public SemanticException(String message, int lineNumber, int columnNumber) {
		super("Type Error " + message );
		
	}
}
