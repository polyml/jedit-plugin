package polyml;

public class TextRange {
	public int startLine;	
	public int start;	// offset in start line
	public int endLine;
	public int end; // offset in end line
	
	public TextRange(int startLine, int start, int endLine, int end) {
		setEnd(endLine, end);
		setStart( startLine, start);
	}
	
	public void setEnd(int endLine, int end){
		this.endLine = endLine;
		this.end = end;
	}
	
	public void setStart(int startLine, int start){
		this.startLine = startLine;
		this.start = start;
	}
}
