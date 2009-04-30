package polyml;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * PolyMarkup is something like a simplified XML structure. Each block consists
 * of a string content and a list of sub elements. Each block is of a kind
 * defined by a character. When written, a block starts with ESC then the
 * character in upper-case. A block is ended by the kind character in
 * lower-case. ESC then ',' indicates the next sub-element in a block.
 * 
 * @author ldixon
 * 
 */
public class PolyMarkup {
	public static int ESC = 0x1b;
	public static int EOT = -1; // end of transmition

	/**
	 * 
	 */
	Character kind; // when Markup a field this is null
	List<PolyMarkup> fields; // when Markup is only content, this is null
	String content; // when Markup is only fields, this is null

	/**
	 * Make a new bit of markup with subs
	 */
	public PolyMarkup(Character k, List<PolyMarkup> s) {
		kind = k;
		content = null;
		fields = s;
	}

	/**
	 * Make a new bit of markup with content
	 */
	public PolyMarkup(Character k, String c) {
		kind = k;
		content = c;
		fields = null;
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


	public static String readDescriptionMessageMarkup(BufferedReader r) throws IOException {
		String content = new String();
		boolean inBlock = true;
		// start off with description meta-data, so don't output it
		boolean output_text = false; 
		int c;
		String loctag = new String();
		
		// System.err.println("D Tag!");
		while (inBlock) {
			c = r.read();
			while (c != ESC) {
				// add to content only if we are in output_text part
				if (output_text) { content += ((char)c); }
				else { loctag += c; }
				c = r.read();
			}
			// now read in to see what kind of special char it is
			c = r.read();
			// System.err.println("ESC: " + c);
			if (((char)c) == 'd') { // end of description markup
				inBlock = false;
			} else if( ((char)c) == ';') { // indicates that we are in output text part
				output_text = true;
			} else if(c == EOT) {
				throw new IOException("readDescriptionMessageMarkup: got EOT");
				// ("readDescriptionMessageMarkup: got EOT", null, c);
			} else {
				loctag += ";";
			}
		}
		return content + "<" + loctag + ">";
	}
	
	/**
	 * read until ESC-k which to end a block from BufferedReader r.
	 * 
	 * @param k : end of block character
	 * @param r : reader
	 * @return : list of fields until k, if just content then one field of
	 *         content
	 * @throws MarkupException
	 * @throws IOException
	 */
	public static PolyMarkup readPolyMarkupWithin(Character k, BufferedReader r)
			throws MarkupException, IOException {
		char c;
		List<PolyMarkup> fields = new LinkedList<PolyMarkup>();
		String content = new String();
		boolean inBlock = true;

		// System.err.println("makePolyMarkupList: In Tag: " + k);
		
		// until the block is ended by ESC then k
		while (inBlock) {
			c = (char) r.read();
			while (c != ESC) {
				// if not in a field, start one
				if (content == null) { content = new String(); }
				content += c;
				c = (char) r.read();
			}
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

		return new PolyMarkup(k, fields);
	}

	// read up to and including next PolyMarkup element, return its markup. 
	public static PolyMarkup readPolyMarkup(BufferedReader r) throws MarkupException, IOException {
		char c;
		Character ch;
		// ignore until first escape
		c = (char) r.read();
		while (c != ESC) {
			// System.err.println("makePolyMarkup: read before ESC: " + c);
			c = (char) r.read();
		}
		c = (char) r.read();
		if (c >= 'A' && c <= 'Z') {
			ch = Character.toLowerCase(c);
			return readPolyMarkupWithin(ch, r);
		} else {
			throw new MarkupException("makePolyMarkup: bad start kind", null, c);
		}
	}
	
	// NOTE: interesting case for generic inefficiency in imperative languages: 
	// end up with hasPrev set each time in the loop - but only needed once. 
	public String toString() {
		String body = new String();
		boolean hasPrev = false;
		if(content != null) { body += content; hasPrev = true; }
		if(fields != null) { 
			Iterator<PolyMarkup> i = fields.iterator();
			while(i.hasNext()) {
				if(hasPrev){ body += (ESC + ",");} 
				body += i.next().toString();
				hasPrev = true;
			}
		}
		
		return (ESC + Character.toUpperCase(kind) + body + ESC + Character.toLowerCase(kind));
	}
	
	
	public String toXMLString() {
		String body = new String();
		boolean hasPrev = false;
		if(content != null) { body += content; hasPrev = true; }
		if(fields != null) { 
			Iterator<PolyMarkup> i = fields.iterator();
			while(i.hasNext()) {
				if(hasPrev){ body += ("<,/>\n  ");} 
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
	
}
