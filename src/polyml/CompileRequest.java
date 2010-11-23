package polyml;

public class CompileRequest {
	String prelude;
	String fileName;
	String src;
	
	String sentParseID; // id of current version of buffer sent for compilation
	String lastParseID; // id of last parsed version of the buffer, same as curParseID if currently parsed
	String prevParseID; // id of previous parsed entry; so we can remove it from table when redundant
	CompileResult result; // compile result
	CompileRequest request; // compilation request
	
	/**
	 * Create a new compile request
	 * Initially assumed to be unparsed. 
	 * 
	 * @param b
	 * @param p 
	 * @param pid id for this version of the parse
	 */
	public CompileRequest(String prelude, String fileName, String src, String sentParseID) {
		super();
		this.prelude = prelude;
		this.fileName = fileName;
		this.src = src;
		this.sentParseID = sentParseID;
		lastParseID = null;
		prevParseID = null;
		result = null;
		request = null;
	}
	
	public CompileRequest(String prelude, String fileName, String src) {
		this(prelude, fileName, src, null);
	}

	/**
	 * Reuses this object for a new CompileRequest on the same file.
	 * @param compileRequest the new request
	 * @param sentParseID the parseId of the new request
	 */
	public void freshRequest(CompileRequest compileRequest, String sentParseID) {
		src = compileRequest.src;
		result = null;
		prelude = compileRequest.prelude;
		this.sentParseID = sentParseID;
	}
}