package polyml;

import java.util.Iterator;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;

import pushstream.PushStream;

public class PolyMarkupPushStream implements PushStream<PolyMarkup> {

	final CompileInfos compileInfos;
	final Object helloLock;
	
	public PolyMarkupPushStream(CompileInfos compileInfos, Object helloLock) {
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
	 */
	void noteLocation(FullLocationResponse l) {
		synchronized (PolyMLPlugin.jEditGUILock) {
			CompileRequest cr = compileInfos.getFromParseID(l.parseID);
			FlexibleLocationReponse flr = new FlexibleLocationReponse(l, cr);
			// srcBuffer.getPath()+":"+line+":"+line_offset+"--"+end_offset+":  "+
			// "No declaration for: `" + l.getSrcLocTextOfBuffer(srcBuffer) + "`" ).send();
			new PolyEBMessage(this, PolyMsgType.POLY_LOCATION, flr);
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
		
		// no longer compiling TODO: find a more appropriate place to put this
		new PolyEBMessage(this, PolyMsgType.POLY_WORKING, false).send();

		// to make output buffer more readable; add new lines after each bit of markup is successfully added. 
		if(m.kind == PolyMarkup.KIND_HELLO) {
			//System.err.println("got hello!");
			synchronized(helloLock) {
				helloLock.notifyAll(); // any threads waiting on compile to be completed can wake up
			}
			
		} else if(m.kind == PolyMarkup.KIND_COMPILE) {
			// Parse the markup into a compile result
			CompileResult res = new CompileResult(m);
			// compile completed and here is the result
			CompileRequest req = compileInfos.compileCompleted(res);
			// have to use strange long name because errorSOurce indexes by object not string.
			
			synchronized(req) {
				req.notifyAll(); // any threads waiting on compile to be completed can wake up
			}
			new PolyEBMessage(this, PolyMsgType.COMPILE_RESULT, req).send();

			
			// now do the GUI stuff to display the errors...
			//System.err.println("PolyMarkupPushStream.add: about to enter GUI lock");
//			synchronized (PolyMLPlugin.jEditGUILock) {
//				String fileName = req.fileName;
//				//System.err.println("PolyMarkupPushStream.add: in GUI lock");
//				Buffer buffer = jEdit.getBuffer(fileName);
				//compileMap.setResultFor(buffer, r); // TODO: put this somewhere better
				
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
				//compileMap.setResultFor(fileName, null);
	
//				if(res.isBug()) {
//					new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
//							fileName+": "+"BUG: Failed to check, or have null buffer.").send();
//					new PolyEBMessage(this, PolyMsgType.COMPILE_RESULT, req).send();
//				} else {
//					
//					// General Status: success/not success
//					if(res.isSuccess()){
//						new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
//								fileName+": "+"Compiled Successfully! (parse id: " + res.parseID + ")").send();
//						new PolyEBMessage(this, PolyMsgType.COMPILE_RESULT, req).send();
//					} else {
//						new PolyEBMessage(this, PolyMsgType.TRANSITIONAL, 
//								fileName+": "+"Compilation found errors of kind: " + res.status + " (parse id: " + res.parseID + ")").send();
//						new PolyEBMessage(this, PolyMsgType.COMPILE_RESULT, req).send();
//					}
//				} // no bug
//				new PolyEBMessage(this, PolyMsgType.COMPILE_RESULT, req).send();
//			} // sync
//			System.err.println("PolyMarkupPushStream.add: out of GUI lock");
			
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
			// No need for this since the position will be a one-off, I guess.
			// Ultimately it might be nice to cache these.  However... in the
			// meantime it is useful to use these objects to calculate lines, etc.
			FlexibleLocationReponse fl = new FlexibleLocationReponse(l, cr);
			
			if(jEdit.getActiveView().getBuffer().getPath().equals(cr.fileName)) {
				jEdit.getActiveView().getTextArea().setSelection(new Selection.Range(l.start,l.end));
				Buffer srcBuffer = jEdit.getBuffer(cr.fileName);
				
				synchronized (PolyMLPlugin.jEditGUILock) { // hack; should be in error src
					// TODO: add this information to the relevant CompileResult.
					if(l.markup.hasNext()) {
						String type_string = l.markup.next().getContent();
						new PolyEBMessage(this, PolyMsgType.INFORMATION,
								srcBuffer.getPath()+":"+fl.getLineNumber()+":"+fl.getLineOffset()+"--"+fl.getEndLineOffset()+":  "+
								"`" + type_string + "` is the type of: `"+ l.getSrcLocTextOfBuffer(srcBuffer) + "`").send();
					} else {
						new PolyEBMessage(this, PolyMsgType.INFORMATION, 
								srcBuffer.getPath()+":"+fl.getLineNumber()+":"+fl.getLineOffset()+"--"+fl.getEndLineOffset()+":  "+
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
			if(jEdit.getActiveView().getBuffer().getPath().equals(cr.fileName)) {
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

