package polyml;

import java.util.Iterator;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;

import pushstream.PushStream;

public class PolyMarkupPushStream implements PushStream<PolyMarkup> {

	CompileInfos compileInfos;
	Object helloLock;
	private BufferMLStatusMap compileMap;
	
	public PolyMarkupPushStream(BufferMLStatusMap map, CompileInfos compileInfos, Object helloLock) {
		this.compileMap = map;
		this.compileInfos = compileInfos;
		this.helloLock = helloLock;
	}
	
	/**
	 * create initial location response from PolyMarkup
	 */
	class LocationResponse {
		String parseID;
		String reqID;
		int start;
		int end;
		Iterator<PolyMarkup> markup;
		
		public LocationResponse(PolyMarkup m){
			markup = m.getSubs().iterator();
			reqID = markup.next().getContent();
			parseID = markup.next().getContent();
			start = Integer.parseInt(markup.next().getContent());
			end = Integer.parseInt(markup.next().getContent());
		}
		
		public String getSrcLocTextOfBuffer(Buffer srcBuffer) {
			return srcBuffer.getText(start, end - start);
		}
		
		public String getSrcLocText() {
			CompileRequest pInfo = compileInfos.getFromParseID(parseID);
			Buffer buffer = jEdit.getBuffer(pInfo.fileName);
			return getSrcLocTextOfBuffer(buffer);
		}
	}
	
	/**
	 * initial location followed by a file and a location in that file. 
	 */
	class FullLocationResponse extends LocationResponse {
		
		String filenameLoc;
		int lineLoc; 
		int startLoc;
		int endLoc;
		
		public FullLocationResponse(PolyMarkup m){
			super(m);
			if(markup.hasNext()) {
				filenameLoc = markup.next().getContent();
				lineLoc = Integer.parseInt(markup.next().getContent());
				startLoc = Integer.parseInt(markup.next().getContent());
				endLoc = Integer.parseInt(markup.next().getContent());
			} else { // we get back null filename if not such remote location
				filenameLoc = null;
				lineLoc = 0;
				startLoc = 0;
				endLoc = 0;
			}
		}
		
		public boolean locGiven() {
			return (filenameLoc != null);
		}
	}
	
	/**
	 * handle location result by adding it to errorList
	 * @param l
	 */
	void noteLocation(FullLocationResponse l) {
		CompileRequest cr = compileInfos.getFromParseID(l.parseID);
		Buffer srcBuffer = jEdit.getBuffer(cr.fileName);
		//Buffer srcBuffer = pInfo.editPane.getBuffer();
		
		synchronized (PolyMLPlugin.jEditGUILock) {
			if(l.locGiven()) {
				Buffer locBuffer = jEdit.getBuffer(l.filenameLoc);
				if(locBuffer != null) {
					int line = locBuffer.getLineOfOffset(l.startLoc);
					int line_offset = l.startLoc - locBuffer.getLineStartOffset(line);
					int end_line = locBuffer.getLineOfOffset(l.endLoc);
					int end_offset = 0;
					if (end_line == line) {
						end_offset = l.endLoc - locBuffer.getLineStartOffset(end_line);
					}
					
					new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
							locBuffer.getPath()+":"+line+":"+line_offset+"--"+end_offset+":  "+
							"Location of: `" + l.getSrcLocTextOfBuffer(srcBuffer) + "`" ).send();
				} else {
					int line = srcBuffer.getLineOfOffset(l.start);
					int line_offset = l.start - srcBuffer.getLineStartOffset(line);
					int end_line = srcBuffer.getLineOfOffset(l.end);
					int end_offset = 0;
					if (end_line == line) {
						end_offset = l.end - srcBuffer.getLineStartOffset(end_line);
					}
					new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
							cr.fileName+":"+line+":"+line_offset+"--"+end_offset+":  "+"No such file: `" + l.filenameLoc + "` : " 
							+ l.startLoc + ":" + l.endLoc).send();
				}
			} else {
				int line = srcBuffer.getLineOfOffset(l.start);
				int line_offset = l.start - srcBuffer.getLineStartOffset(line);
				int end_line = srcBuffer.getLineOfOffset(l.end);
				int end_offset = 0;
				if (end_line == line) {
					end_offset = l.end - srcBuffer.getLineStartOffset(end_line);
				}
				
				new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
						srcBuffer.getPath()+":"+line+":"+line_offset+"--"+end_offset+":  "+
						"No declaration for: `" + l.getSrcLocTextOfBuffer(srcBuffer) + "`" ).send();
			}
		}
	}
	
	
	/**
	 * Given some markup, what to we do with it...
	 */
	public synchronized void add(PolyMarkup m) {
		System.err.println("PolyMarkupPushStream.add: " + m.toXMLString());
		
		//PolyMLPlugin.debugMessage("\n\n"); 
		//PolyMLPlugin.debugMessage(m.toPrettyString()); 
		PolyMLPlugin.debugMessage("\n\n"); 
		PolyMLPlugin.debugMessage(m.toXMLString());
		PolyMLPlugin.debugMessage("\n\n"); 
		// to make output buffer more readable; add new lines after each bit of markup is successfully added. 
		if(m.kind == PolyMarkup.KIND_HELLO) {
			//System.err.println("got hello!");
			synchronized(helloLock) {
				helloLock.notifyAll(); // any threads waiting on compile to be completed can wake up
			}
		} else if(m.kind == PolyMarkup.KIND_COMPILE) {
			// Parse the markup into a compile result
			CompileResult r = new CompileResult(m);
			// no longer compiling TODO: find a more appropriate place to put this
			new PolyEBMessage(this, PolyMsgType.POLY_WORKING, false).send();
			
			// compile completed and here is the result
			CompileRequest cr = compileInfos.compileCompleted(r);
			// have to use strange long name because errorSOurce indexes by object not string.
			
			synchronized(cr) {
				cr.notifyAll(); // any threads waiting on compile to be completed can wake up
			}
			
			// now do the GUI stuff to display the errors...
			//System.err.println("PolyMarkupPushStream.add: about to enter GUI lock");
			synchronized (PolyMLPlugin.jEditGUILock) {
				String fileName = cr.fileName;
				//System.err.println("PolyMarkupPushStream.add: in GUI lock");

				Buffer buffer = jEdit.getBuffer(cr.fileName);
				compileMap.setResultFor(buffer, r); // TODO: put this somewhere better
				
				// COMMENTED OUT: auto-open of compiled files: causes AWT thread lockup - I don't know why!?
//				System.err.println("PolyMarkupPushStream.add: in GUI lock2");
//
//				//String fileName = i.buffer.getPath();
//				//Buffer buffer = i.buffer;
//				//System.err.println("Completed compile for file: " + cr.fileName);
//				if(buffer == null) {
//					//System.err.println("buffer for: " + cr.fileName + " is null! opening it ");
//					buffer = jEdit.openFile((View) null, cr.fileName);
//					if(buffer == null) {
//						System.err.println("cannot open: " +  cr.fileName);
//						fileName = cr.fileName;
//						System.err.println("PolyMarkupPushStream.add: in GUI lock2.1");
//					} else {
//						fileName = buffer.getPath();
//						System.err.println("PolyMarkupPushStream.add: in GUI lock2.2");
//					}
//				} else {
//					fileName = buffer.getPath();
//					System.err.println("PolyMarkupPushStream.add: in GUI lock2.3");
//				}
//				
//				System.err.println("PolyMarkupPushStream.add: in GUI lock3");

				// TODO: clear compilation status for "filename"?
				// compileMap.setResultFor(fileName, null);
	
//				(r.isBug() || buffer == null) 
				if(r.isBug()) {
					new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
							fileName+": "+"BUG: Failed to check, or have null buffer.").send();
				} else {
					// General Status: success/not success
					
					if(r.isSuccess()){
						new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
								fileName+": "+"Compiled Successfully! (parse id: " + r.parseID + ")").send();
					} else {
						new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
								fileName+": "+"Compilation found errors of kind: " + r.status + " (parse id: " + r.parseID + ")").send();
					}
					
					if(buffer != null) {
						// can still have errors even is success: e.g. warnings. 
						for (PolyMLError e : r.errors) {
							
							//System.err.println("PolyMarkupPushStream: error at: " + e.startPos + ":" + e.endPos);
							//System.err.println("going thruogh the error list...");
							
							try {
								int line = buffer.getLineOfOffset(e.startPos);
								int line_offset = e.startPos - buffer.getLineStartOffset(line);
								int end_line = buffer.getLineOfOffset(e.endPos);
								int end_offset = 0;
								if (end_line == line) {
									end_offset = e.endPos - buffer.getLineStartOffset(end_line);
								}
							
								// TODO: add an error?
								new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
										fileName+":"+line+":"+line_offset+"--"+end_offset+":  ("+e.kind+") "+e.message).send();
								
							} catch(java.lang.ArrayIndexOutOfBoundsException ex) {
								ex.printStackTrace();
							}
						} // for errors
					}// if buffer != null
				} // no bug
			} // sync
			System.err.println("PolyMarkupPushStream.add: out of GUI lock");
		} else if(m.kind == PolyMarkup.KIND_PROPERTIES) {
			LocationResponse l = new LocationResponse(m);
			CompileRequest lastCompile = compileInfos.getFromParseID(l.parseID);
			
			// FIXME: deal with returned list of properties
			// FIXME: synchronisation issue between getting buffer path and selecting right area in it?
			TextArea a = jEdit.getActiveView().getTextArea();
			Buffer b = jEdit.getActiveView().getBuffer();
			
			if(b.getPath().equals(lastCompile.fileName)) {
				a.setSelection(new Selection.Range(l.start,l.end));
			}
		} else if(m.kind == PolyMarkup.KIND_TYPE_INFO) {
			LocationResponse l = new LocationResponse(m);
			CompileRequest cr = compileInfos.getFromParseID(l.parseID);
			
			if(jEdit.getActiveView().getBuffer().getPath().equals(cr.fileName)) {
				jEdit.getActiveView().getTextArea().setSelection(new Selection.Range(l.start,l.end));
				Buffer srcBuffer = jEdit.getBuffer(cr.fileName); 
				// jEdit.getActiveView().getBuffer();
				int line = srcBuffer.getLineOfOffset(l.start);
				int line_offset = l.start - srcBuffer.getLineStartOffset(line);
				int end_line = srcBuffer.getLineOfOffset(l.end);
				int end_offset = 0;
				if (end_line == line) {
					end_offset = l.end - srcBuffer.getLineStartOffset(end_line);
				}
				
				synchronized (PolyMLPlugin.jEditGUILock) { // hack; should be in error src
					if(l.markup.hasNext()) {
						String type_string = l.markup.next().getContent();
						new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
								srcBuffer.getPath()+":"+line+":"+line_offset+"--"+end_offset+":  "+
								"`" + type_string + "` is the type of: `"+ l.getSrcLocTextOfBuffer(srcBuffer) + "`").send();
					} else {
						new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
								srcBuffer.getPath()+":"+line+":"+line_offset+"--"+end_offset+":  "+
								"Not a value, so no type: `"+ l.getSrcLocTextOfBuffer(srcBuffer) + "`").send();
					}
				}
			} else {
				System.err.println("different buffer shown than one which asked for type.");
			}
		} 
		// Location responses
		else if(m.kind == PolyMarkup.KIND_LOC) {
			FullLocationResponse l = new FullLocationResponse(m);
			noteLocation(l);
		} 
		// movement responses (all the same)
		else if(m.kind == PolyMarkup.KIND_MOVE) {
			LocationResponse l = new LocationResponse(m);
			CompileRequest cr = compileInfos.getFromParseID(l.parseID);
			if(jEdit.getActiveView().getBuffer().getPath() == cr.fileName) {
				jEdit.getActiveView().getTextArea().setSelection(new Selection.Range(l.start,l.end));
			}
		} else {
			System.err.println("Unkown message kind ignored: " + m.kind);
		}
		
		System.err.println("PolyMarkupPushStream.add: end.");
	}

	public void add(PolyMarkup c, boolean isMore) { add(c); }

	public void close() { 
		// nothing special to do 
	}
		
}

