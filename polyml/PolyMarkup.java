package polyml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import pushstream.PushStream;


public class PolyMarkup implements PushStream<Character> {
	public final static int ESC = 0x1b;
	public final static int EOT = -1; // end of transmition
	
	// exlusive possible values of status
	public final static int STATUS_OUTSIDE = 1;
	public final static int STATUS_OUTSIDE_ESC = 2;
	public final static int STATUS_OUTSIDE_IN_D = 3;
	
	public final static int STATUS_INSIDE = 4;
	public final static int STATUS_INSIDE_ESC = 5;
	public final static int STATUS_INSIDE_IN_D = 6;
	public final static int STATUS_INSIDE_ESC_IN_D = 7;
	//public final static int STATUS_INSIDE_IN_D2 = 8;
	//public final static int STATUS_INSIDE_ESC_IN_D2 = 9;
	
	public final static int STATUS_COMPLETE = 10;
	
	public final static char INKIND_COMPILE = 'R';
	public final static char INKIND_PROPERTIES = 'O';
	public final static char INKIND_TYPE_INFO = 'T';
	public final static char INKIND_DEC_LOCATION = 'I';
	public final static char INKIND_MOVE_TO_PARENT = 'U';
	public final static char INKIND_MOVE_TO_FIRST_CHILD = 'C';
	public final static char INKIND_MOVE_TO_NEXT = 'N';
	public final static char INKIND_MOVE_TO_PREVIOUS = 'P';
	public final static char INKIND_LOC_WHERE_OPENED = 'J';
	public final static char INKIND_LOC_OF_PARENT_STRUCT = 'J';
	public final static char OUTKIND_CANCEL = 'K';
	
	// status of lazy markup
	int status;

	// data for lazy markup
	Character kind; // when Markup a field this is null
	List<PolyMarkup> fields; // when Markup is only content, this is null
	String content; // when Markup is only fields, this is null
	Queue<PolyMarkup> parents; // 

	PushStream<PolyMarkup> markupStream;

	// create a new empty markup, that, when we get a completed markup, sends it onto the given stream;
	public PolyMarkup(PushStream<PolyMarkup> a) {
		markupStream = a;
		resetMarkup();
	}

	// start again from having seen no markup. 
	public void resetMarkup() {
		kind = null;
		fields = null;
		content = null;
		parents = null;
		status = STATUS_OUTSIDE;
	}
	
	// create element with content and no sub-elements. 
	public PolyMarkup(Character c, String s) {
		markupStream = null;
		kind = c;
		content = s;
		status = STATUS_COMPLETE;
		parents = null;
	}
	
	// create an element with no content, but with subelements. 
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
		if(content == null) {content = new String();}
		content += c;
	}
	
	public void addToFields(PolyMarkup f) {
		if(fields == null) {fields = new LinkedList<PolyMarkup>(); }
		fields.add(f);
	}
	
	// nothing to do when the stream is closed. 
	public void close() {
	}
	
	public void add(Character c, boolean isMore) { add(c); }
	
	// add a character to the markup seen so far
	public void add(Character c) {
		if(status == STATUS_OUTSIDE_ESC) {
			if(c != 'D' && c >= 'A' && c <= 'Z') { // new tag, markup started!
				parents = new LinkedList<PolyMarkup>();
				kind = new Character(c);
				status = STATUS_INSIDE;
				fields = null;
				content = null;
			} else {
				status = STATUS_OUTSIDE;
			}
			//else if(c == 'D') { // translate extra info markup, put into 
			//	status = STATUS_OUTSIDE_IN_D;
			//}
		} else if(status == STATUS_OUTSIDE) {
			if(c == ESC) {
				status = STATUS_OUTSIDE_ESC;
			}
		} else if(status == STATUS_INSIDE) {
			if(c == ESC) {
				status = STATUS_INSIDE_ESC;
			} else {
				addToContent(c);
			}
		} else if(status == STATUS_INSIDE_ESC) {
			if(c == 'D') {
				status = STATUS_INSIDE_IN_D;
				addToContent('[');
			} else if( c >= 'A' && c <= 'Z') { // new tag, markup started!
				if(content != null) { // create new subtag with content
					addToFields(new PolyMarkup(null,content));
					content = null;
				}
				PolyMarkup parent = new PolyMarkup(kind,fields);
				parents.add(parent);
				fields = null;
				kind = new Character(c);
				status = STATUS_INSIDE;
			} else if ( c >= 'a' && c <= 'z' ) { // end tag
				if(Character.toLowerCase(kind) == c) {					
					PolyMarkup cur;
					if(fields == null) {
						if(content == null) { content = new String(); }
						cur = new PolyMarkup(kind,content);
					} else {
						if(content != null) {
							fields.add(new PolyMarkup(null,content));
						}
						cur = new PolyMarkup(kind,fields);
					}
					
					if(parents.isEmpty()) {
						status = STATUS_COMPLETE;
						// push on completed markup to markup stream
						//System.err.println("addChar: status complete!: " + cur.toXMLString());
						PolyMLPlugin.debugBuffer.append(cur.toXMLString());
						markupStream.add(cur);
						// if we close the last tag, we have completed parsing!
						resetMarkup();
					} else {
						PolyMarkup oldParent = parents.remove();
						fields = oldParent.fields;
						kind = oldParent.kind;
						content = null;
						addToFields(cur);
						status = STATUS_INSIDE;
					}
				} else {
					System.err.println("addChar: STATUS_INSIDE_ESC: unexpected ESC char: " + c);
					status = STATUS_INSIDE;
				}
			} else if( c == ',') {
				if(content == null) { content = new String(); }
				addToFields(new PolyMarkup(null,content));
				content = null;
				status = STATUS_INSIDE;
			} else {
				System.err.println("addChar: STATUS_INSIDE_ESC: unexpected ESC char: " + c);
				status = STATUS_INSIDE;
			}
		
		} else if(status == STATUS_INSIDE_IN_D ) {
			if(c == ESC) {
				status = STATUS_INSIDE_ESC_IN_D;
			} else {
				addToContent(c);
			}
		} else if(status == STATUS_INSIDE_ESC_IN_D) {
			if(content == null) { content = new String(); }
			if(c == ',') {
				status = STATUS_INSIDE_IN_D;
				content += c;
			} else if (c == ';'){
				addToContent(']');
				status = STATUS_INSIDE_IN_D;
			} else if (c == 'd'){
				status = STATUS_INSIDE;
			} else {
				System.err.println("addChar: STATUS_INSIDE_ESC_IN_D: unexpected ESC char: " + c);
			}
		} else if(status == STATUS_COMPLETE) {
			System.err.println("addChar: called on PolyMarkup which is STATUS_COMPLETE");
		} else {
			System.err.println("addChar: called with undefined status: " + status);	
		}
	}
	
	
	// NOTE: interesting case for generic inefficiency in imperative languages: 
	// end up with hasPrev set each time in the loop - but only needed once. 
	public String toString() {
		String body = new String();
		boolean hasPrev = false;
		Character k; 
		if(kind == null) {k = '_'; } else {k = kind;}
		if(content != null) { body += content; hasPrev = true; }
		if(fields != null) { 
			Iterator<PolyMarkup> i = fields.iterator();
			while(i.hasNext()) {
				if(hasPrev){ body += (ESC + ",");} 
				body += i.next().toString();
				hasPrev = true;
			}
		}
		
		return (ESC + Character.toUpperCase(k) + body + ESC + Character.toLowerCase(k));
	}
	
	
	public String toXMLString() {
		String body = new String();
		boolean hasPrev = false;
		if(content != null) { body += content; hasPrev = true; }
		if(fields != null) { 
			Iterator<PolyMarkup> i = fields.iterator();
			while(i.hasNext()) {
				if(hasPrev){ body += ("<,/>\n");} 
				body += i.next().toXMLString();
				hasPrev = true;
			}
		}
		
		String kindString;
		if(kind != null){
			kindString = "" + Character.toUpperCase(kind);
		} else {
			kindString = "_";
		}
		return ("<" + kindString + ">" + body + "</" + kindString + ">\n");
	}
	

	
	public static String explicitEscapes(String s) {
		return s.replace((char)ESC, '`');
	}
	
}


/*

// now read in to see what kind of special char it is
c = (char) r.read();
// System.err.println("ESC: " + c);
if (c == 'D') {
	if (content == null) { content = new String(); }
	content += readDescriptionMessageMarkup(r);
} else if (c == ',') { // if field break
	if (content == null) { content = new String(); }
	fields.add(new PolyMarkup(null, content));
	// System.err.println("content in field of '" + k + "' = '" + content + "'");
	content = new String();
} else if (c >= 'A' && c <= 'Z') {
	// if capital char, note new previous field if not empty, then
	// read sub-field. if not empty previous stuff, then add new
	// stuff as separate field.
	if (content != null && !content.isEmpty()) {
		fields.add(new PolyMarkup(null, content));
		// System.err.println("content in field of '" + k + "' = '" + content + "'");
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
		// System.err.println("content in '" + k + "' = '" + content + "'");
	}
	inBlock = false;
	// System.err.println("end tag: " + c + "");
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
