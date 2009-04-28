package polyml;

import java.util.LinkedList;

/** History of commands sent (by user's input) in a ShellBuffer */
public class History {
	
	LinkedList<String> mHistoryLL;
	Integer mMaxSize;

	public History(Integer size) {
		mHistoryLL = new LinkedList<String>();
		mMaxSize = size;
	}
	
	/** add a new string to the history */
	public void add(String s) {
		mHistoryLL.addFirst(s);
		if(mHistoryLL.size() > mMaxSize) {
			mHistoryLL.removeLast();
		}
	}
	
	/** cycle back an element, adding cur to end and removing the 
	 * first element and returning it. 
	 * 
	 * next(prev(c)) = c and leaves history unchanged. 
	 * Note: interesting idea for formal verification !
	 * */
	public String prev(String cur) {
		String s = mHistoryLL.getFirst();
		mHistoryLL.removeFirst();
		mHistoryLL.addLast(cur);
		return s;
	}
	
	/** cycle forward an element, adding cur to start and removing the 
	 * last element and returning it. 
	 * 
	 * 	prev(next(c)) = c and leaves history unchanged. 
	 *  Note: interesting idea for formal verification !
	 * */
	public String next(String cur) {
		String s = mHistoryLL.getLast();
		mHistoryLL.removeLast();
		mHistoryLL.addFirst(cur);
		return s;
	}
}
