package polyml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import pushstream.PushStream;


/**
 * Represents PolyML IDE Markup elements.
 * See http://www.polyml.org/docs/IDEProtocol.html for details.
 * 
 * The idea is that this is a polymorphic tagged n-ary tree.
 * Each node has a kind, which may be null. null nodes have content, all 
 * other kinds of node have only sub-nodes. 
 * 
 */
public class PolyMarkup implements PushStream<Character> {
	/* special characters used when parsing */
	public final static int ESC = 0x1b;
	public final static int EOT = -1; // end of transmission
	
	/* internal status values - need to be unique, but value is not itself important */
	// exclusive possible values of status
	public final static int STATUS_OUTSIDE = 1;
	public final static int STATUS_OUTSIDE_ESC = 2;
	public final static int STATUS_OUTSIDE_IN_D = 3;
	
	public final static int STATUS_INSIDE = 4;
	public final static int STATUS_INSIDE_ESC = 5;
	public final static int STATUS_INSIDE_SC = 6; // in semi colon field - final field
	public final static int STATUS_INSIDE_ESC_IN_SC = 7; // 
	//public final static int STATUS_INSIDE_IN_D2 = 8;
	//public final static int STATUS_INSIDE_ESC_IN_D2 = 9;
	
	public final static int STATUS_COMPLETE = 10;
	
	public final static char OUTKIND_CANCEL = 'K'; /* for cancel result */
	
	public final static char KIND_DEFAULT_FIELD = ';'; /* marks end of start tag, begging of content */

	public final static char KIND_COMPILE = 'R'; /* for result of compilation */
	public final static char KIND_PROPERTIES = 'O'; /* for properties of current point in parse tree */
	public final static char KIND_TYPE_INFO = 'T'; /* for type information */
	public final static char KIND_DESCRIPTION_TAG = 'D'; /* for inline location info */

	public final static char KIND_LOC = 'I'; /* for defined location, e.g. of identifier */
	
	public final static char KIND_MOVE = 'M'; /* for moving up or down term tree. */
	public static final char KIND_HELLO = 'H';/* for making/testing contact with PolyML */
	
	
	/** status of lazy markup */
	int status;

	// data for lazy markup
	Character kind; // when content, kind is null, else it's containing kind. 
	List<PolyMarkup> fields; // when Markup is only content, this is null
	String content; // when Markup is only fields, this is null
	LinkedList<PolyMarkup> parents; // 

	PushStream<PolyMarkup> markupStream;

	/**
	 * create a new empty markup, that, when we get a completed markup, sends it onto the given stream;
	 * @param a the stream to send data to
	 */
	public PolyMarkup(PushStream<PolyMarkup> a) {
		markupStream = a;
		resetMarkup();
	}

	/**
	 * Start (again): resets (as if) having seen no markup. 
	 */
	public void resetMarkup() {
		kind = null;
		fields = null;
		content = null;
		parents = null;
		status = STATUS_OUTSIDE;
	}
	
	/**
	 * Create element with content and no sub-elements.
	 * @param c type of element
	 * @param s element content.
	 */
	public PolyMarkup(Character c, String s) {
		markupStream = null;
		kind = c;
		content = s;
		status = STATUS_COMPLETE;
		parents = null;
	}
	
	/**
	 * Create an element with no content, but with subelements. 
	 * @param c type of element
	 * @param fs list of sub-elements
	 */
	public PolyMarkup(Character c, List<PolyMarkup> fs) {
		markupStream = null;
		fields = fs;
		kind = c;
		content = null;
		status = STATUS_COMPLETE;
		parents = null;
	}
	
	
	public List<PolyMarkup> getSubs() {
		return fields;
	}
	
	public String getContent() {
		return content;
	}
	
	public Character getKind() {
		return kind;
	}
	
	public boolean hasSubs() {
		return fields != null;
	}

	public boolean hasContent() {
		return content != null;
	}
	
	public void addToContent(char c) {
		if(content == null) {content = "";}
		content += c;
	}
	
	public void addToFields(PolyMarkup f) {
		if(fields == null) {fields = new LinkedList<>(); }
		fields.add(f);
	}
	
	/**
	 * Close the stream (does nothing).
	 */
	public void close() { }

	/**
	 * Adds an empty element of given type to the markup.
	 * @param c kind of element
	 * @param isMore ignored at present.
	 */
	public void add(Character c, boolean isMore) { add(c); }
	
	
	public void enterTag(char c) {
		if(content != null) { // create new subtag with content
			addToFields(new PolyMarkup(null,content));
			content = null;
		}
		PolyMarkup parent = new PolyMarkup(kind,fields);
		parents.addFirst(parent);
		fields = null;
		kind = c;
		//System.err.println("new inside: " + kind + "; parent was: " + parent.kind);
		status = STATUS_INSIDE;
	}
	
	
	public void endTag() {
		//System.err.println("closing: " + kind);
		PolyMarkup cur;
		if(fields == null) {
			if(content == null) { content = ""; }
			cur = new PolyMarkup(kind,content);
		} else {
			if(content != null) {
				fields.add(new PolyMarkup(null,content));
			}
			cur = new PolyMarkup(kind,fields);
		}
		
		//System.err.println("cur: " + cur.toPrettyString());

		if(parents.isEmpty()) {
			status = STATUS_COMPLETE;
			// push on completed markup to markup stream
			//System.err.println("addChar: status complete!: " + cur.toXMLString());
			//PolyMLPlugin.debugBuffer.append(cur.toPrettyString());
			markupStream.add(cur);
			// if we close the last tag, we have completed parsing!
			resetMarkup();
		} else {
			PolyMarkup oldParent = parents.removeFirst();
			fields = oldParent.fields;
			kind = oldParent.kind;
			content = null;
			addToFields(cur);
			status = STATUS_INSIDE;
		}
	}
	
	/**
	 * Add a character to the markup seen so far
	 * @see pushstream.PushStream#add(java.lang.Object)
	 */
	public void add(Character c) {
		//System.err.println("PolyMarkup.add: " + c);

		if(status == STATUS_OUTSIDE_ESC) {
			if(c >= 'A' && c <= 'Z') { // new tag, markup started!
				parents = new LinkedList<>();
				kind = c;
				status = STATUS_INSIDE;
				fields = null;
				content = null;
			} else {
				status = STATUS_OUTSIDE;
			}
		} else if(status == STATUS_OUTSIDE) {
			if(c == ESC) {
				status = STATUS_OUTSIDE_ESC;
			}
		} else if(status == STATUS_INSIDE) {
			if(c == ESC) { status = STATUS_INSIDE_ESC; } 
			else { addToContent(c); }
		} else if(status == STATUS_INSIDE_ESC) {
			if( c == KIND_DEFAULT_FIELD) { // new tag, markup started!
				enterTag(c);
			} else if( c >= 'A' && c <= 'Z') { // new tag, markup started!
				enterTag(c);
			} else if ( c >= 'a' && c <= 'z' ) { // end tag
				if(kind == KIND_DEFAULT_FIELD) {
					endTag(); endTag();
				} else if(Character.toLowerCase(kind) == c) {
					endTag();
				} else {
					System.err.println("addChar: STATUS_INSIDE_ESC: unexpected ESC char: " + c + "; within kind: " + kind);
					status = STATUS_INSIDE;
				}
			} else if( c == ',') {
				// if content of last field is null, make it empty string. 
				if(content == null) { content = ""; }
				addToFields(new PolyMarkup(null,content));
				content = null;
				status = STATUS_INSIDE;
			} else {
				System.err.println("addChar: STATUS_INSIDE_ESC: unexpected ESC char: " + c + "; within kind: " + kind);
				status = STATUS_INSIDE;
			}
		} else if(status == STATUS_COMPLETE) {
			System.err.println("addChar: called on PolyMarkup which is STATUS_COMPLETE");
		} else {
			System.err.println("addChar: called with undefined status: " + status);	
		}
	}

	/* 
	 * recursively changes location fields into HTML content
	 * */
	public void recChangeLocationFieldsToHTML() {
		if(fields != null) {
			Iterator<PolyMarkup> i = fields.iterator();
			if(kind == KIND_DESCRIPTION_TAG) { 
				// get local location details. 
				String filename = (i.next()).getContent();
				String startline = (i.next()).getContent();
				String startloc = (i.next()).getContent();
				String endloc = (i.next()).getContent();
				// the main sub-stuff
				PolyMarkup substuff = i.next();
				substuff.recChangeLocationFieldsToHTML();
				
				fields = new LinkedList<>();
				
				fields.add(new PolyMarkup(null,"<a href='pmjp://" + filename + "?line=" + startline + 
						"&start=" + startloc + "&end=" + endloc + "'>"));
				fields.add(substuff);
				fields.add(new PolyMarkup(null,"</a>"));
				
				kind = KIND_DEFAULT_FIELD;
			} else {
				while(i.hasNext()) {
					PolyMarkup m2 = i.next();
					m2.recChangeLocationFieldsToHTML();
				}
			}
		}
	}
	
	
	
	/* 
	 * flatten all recursive fields to XML markup. 
	 * */
	public void recFlattenAllFieldsToContent() {
		if(content == null) { content = ""; }
		if(fields != null) {
			String tag = null;
			Iterator<PolyMarkup> i = fields.iterator();
			if(kind != null && kind != KIND_DEFAULT_FIELD) {
				tag = "POLYML_" + kind + "";
				content += "<" + tag + ">";
			}
			while(i.hasNext()) {
				PolyMarkup m2 = i.next();
				m2.recFlattenAllFieldsToContent();
				content += m2.getContent();
				if(i.hasNext() && kind != KIND_DEFAULT_FIELD) {
					content += "<POLYML_NEXT/>";
				}
			}
			if(tag != null){
				content +=  "</" + tag + ">";
			}
			fields = null;
		}
	}
	
	/** 
	 * don't worry about this fields kind, recursively flatten everything else 
	 * */
	public void recFlattenAllSubFieldsToContent() {
		if(content == null) { content = ""; }
		if(fields != null) {
			for (PolyMarkup m2 : fields) {
				m2.recFlattenAllFieldsToContent();
				content += m2.getContent();
			}
			fields = null;
		}
	}
	
	/*
	 * Flattens everything within a default field - typically this was the 
	 * stuff after a ";" before the end markup. 
	 */
	public void recFlattenDefaultFieldsToContent() {
		if(kind != null && kind == KIND_DEFAULT_FIELD) {
			recFlattenAllFieldsToContent(); // to result in pure content.
		} else if(fields != null) {
			for(PolyMarkup m2 : fields) {
				m2.recFlattenDefaultFieldsToContent();
			}
		}
	}
	
	/*
	 * This will flatten the content, recursively of all sub-fields of kind "k"
	 * For example, you can use this to flatten the PolyML location tags which 
	 * in ESC D ... ESC d tags. (kind = D)
	 */
	public void recFlattenUnderTagToContent(char k) {
		if(kind != null && kind == k) {
			recFlattenAllFieldsToContent();
		} else if(fields != null) {
			for(PolyMarkup m2 : fields) {
				m2.recFlattenUnderTagToContent(k);
			}
		}
	}
	
	// NOTE: interesting case for generic inefficiency in imperative languages: 
	// end up with hasPrev set each time in the loop - but only needed once. 
	/**
	 * recreate the input from PolyML that would produce this markup
	 */
	public String toString() {
		String body = "";
		boolean hasPrev = false;
		char k;
		if(kind == null) {k = '_'; } else {k = kind;}
		if(content != null) { body += content; hasPrev = true; }
		if(fields != null) {
			for (PolyMarkup field : fields) {
				if (hasPrev) {
					body += (ESC + ",");
				}
				body += field.toString();
				hasPrev = true;
			}
		}
		
		return (ESC + Character.toUpperCase(k) + body + ESC + Character.toLowerCase(k));
	}
	
	/**
	 * Produces a string of this Markup object, as it would have 
	 * come from PolyML (for debugging, easier to read than toString)
	 */
	public String toPrettyString() {
		String body = "";
		boolean hasPrev = false;
		if(content != null) { body += content; hasPrev = true; }
		if(fields != null) {
			for (PolyMarkup field : fields) {
				if (hasPrev) {
					body += ("`,");
				}
				body += field.toPrettyString();
				hasPrev = true;
			}
		}
		
		String startKindString;
		String endKindString;

		if(kind != null){
			startKindString = Character.toString(Character.toUpperCase(kind));
			endKindString = Character.toString(Character.toLowerCase(kind));
		} else {
			endKindString = "_";
			startKindString = "_";
		}
		return ("`" + startKindString + body + "`" + endKindString + "\n");
	}
	
	/**
	 * Produces a XML string format of this Markup object, reflecting the 
	 * internal data structure. (mostly for debugging)
	 */
	public String toXMLString() {
		String body = "";
		//boolean hasPrev = false;
		
		if(content != null) {
			body += "<c>" + content + "</c>"; //hasPrev = true; 
		}
		if(fields != null) { 
			body += "\n<f>";
			for (PolyMarkup field : fields) {
				// if(hasPrev){ body += ("<POLYML_NEXT/>\n");}
				body += field.toXMLString();
				//hasPrev = true;
			}
			body += "\n</f>";
		}
		
		if(kind != null){
			String kindString = "" + Character.toUpperCase(kind);
			body = "\n<m k=" + kindString + ">" + body + "\n</m>";
		} else {
			body = "\n<m>" + body + "</m>";
		}
		
		return body;
		//return ("<POLYML_" + kindString + ">" + body + "</POLYML_" + kindString + ">\n");
	}
	
	/**
	 * Extracts location information from appropriate PolyMarkup for
	 * representation as HTML-style strings.
	 * </pre>
	 * @see StateViewDockable#uriof(errorlist.ErrorSource.Error)
	 */
	public String toHTMLString() {
		return toXMLString();
		//throw new UnsupportedOperationException("Not yet implemented.");
	}
	
	/**
	 * Replaces escape characters with the backquote character.
	 * @return a modified string.
	 */
	public static String explicitEscapes(String s) {
		return s.replace((char)ESC, '`');
	}
	
}


/*

// now read in to see what kind of special char it is
c = (char) r.read();
//System.err.println("ESC: " + c);
if (c == 'D') {
	if (content == null) { content = new String(); }
	content += readDescriptionMessageMarkup(r);
} else if (c == ',') { // if field break
	if (content == null) { content = new String(); }
	fields.add(new PolyMarkup(null, content));
	//System.err.println("content in field of '" + k + "' = '" + content + "'");
	content = new String();
} else if (c >= 'A' && c <= 'Z') {
	// if capital char, note new previous field if not empty, then
	// read sub-field. if not empty previous stuff, then add new
	// stuff as separate field.
	if (content != null && !content.isEmpty()) {
		fields.add(new PolyMarkup(null, content));
		//System.err.println("content in field of '" + k + "' = '" + content + "'");
	}

	// read subfield
	Character ch = Character.toLowerCase(c);
	try { // catch how far parsing had gone
		fields.add(readPolyMarkupWithin(ch, r));
	} catch (MarkupException e) {
		// add e's markup: this is the subfield already wrapped up.
		fields.add(e.markup);
		throw new MarkupException("Bad Markup", new PolyMarkup(k,
				fields), e.ch);
	}
	content = null; // might not have another field after this
} else if (k.charValue() == c) { // k char indicates end of this block
	if (content != null) { // avoid empty end of field fields.
		fields.add(new PolyMarkup(null, content));
		//System.err.println("content in '" + k + "' = '" + content + "'");
	}
	inBlock = false;
	//System.err.println("end tag: " + c + "");
} else { // includes bad EOT characters
	// badly formed, add content so far to error
	if (content != null) {
		fields.add(new PolyMarkup(null, content));
	}
	throw new MarkupException("Bad Markup", new PolyMarkup(k,
			fields), c);
}
}



}
if(isComplete) { action.act(this); }
}
*/