package polyml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CompileRequest {
	String prelude;
	String fileName;
	String src;
	
	/** id of current version of buffer sent for compilation */
	String sentParseID;
	/** id of last parsed version of the buffer, same as curParseID if currently parsed */
	String lastParseID;
	/** id of previous parsed entry; so we can remove it from table when redundant */
	String prevParseID;
	/** compile result */
	private CompileResult result;
	/** cache of type information against this request */
	private final List<PolyMarkup> typeInfos;
	
	/**
	 * Create a new compile request; initially assumed to be unparsed. 
	 * @param prelude request prelude
	 * @param fileName user-end file path
	 * @param src not entirely sure...
	 * @param sentParseID for this version of the parse
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
		typeInfos = new ArrayList<>();
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
		prelude = compileRequest.prelude;
		this.sentParseID = sentParseID;
		this.typeInfos.clear();
		result = null;
	}

	public Iterator<PolyMarkup> getTypeInfo() {
		return typeInfos.iterator();
	}
	public void setTypeInfo(Collection<PolyMarkup> info) {
		typeInfos.clear();
		typeInfos.addAll(info);
	}
	
//	/**
//	 * Adds the detail of a supplemental CompileResult (i.e. type check)
//	 * @param b the buffer associated with this result.
//	 * @param cr the result itself.
//	 */
//	public void setTypeInfo(Buffer b, CompileResult cr) {
//		for (PolyMLError e : cr.errors) {
//			if (e.kind == PolyMarkup.KIND_TYPE_INFO) {
//				typeInfos.add( ?? );
//			}
//		}
//	}
	public CompileResult getResult() {
		return result;
	}
	/**
	 * Sets the compile result.  Also takes the opportunity to attach flexible
	 * positions to any errors contained within the result.
	 * Finally fires off a {@link PolyMsgType} BUFFER_UPDATE message so that
	 * the display is informed of the updated information.
	 */
	public void setResult(CompileResult result) {
		this.result = result;
		PolyMLError.associateAllErrors(result, fileName);
		new PolyEBMessage(this, PolyMsgType.COMPILE_RESULT, this).send();
	}
}