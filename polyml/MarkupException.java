package polyml;

/**
 * For errors in the markup process, parsing etc.
 */
public class MarkupException extends Exception {
	private static final long serialVersionUID = 1L;

	public PolyMarkup markup;
	public Character ch;

	public MarkupException(String msg, PolyMarkup m, Character c) {
		super(msg);
		markup = m;
		ch = c;
	}
	
	public MarkupException(String msg, PolyMarkup m) {
		super(msg);
		markup = m;
		ch = null;
	}
	
	// hacky print thing for debugging
	public void printMarkupException(){
		if(markup != null) {
			System.err.println("MarkupException: got to: \n " + markup.toPrettyString());
		}
		if(ch != null) {
			System.err.println("MarkupException: ch: " + ch);
		}
		System.err.println("MarkupException: err: " + toString());
		printStackTrace();
	}
}