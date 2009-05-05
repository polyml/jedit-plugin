package pushstream;

/**
 * 
 * Copies everything given to a pair of push streams;
 * 
 * @author ldixon
 *
 * @param <C>
 */
public class CopyPushStream<C> implements PushStream<C> {

	PushStream<C> stream1;
	PushStream<C> stream2;
	
	public CopyPushStream(PushStream<C> s1, PushStream<C> s2) {
		stream1 = s1;
		stream2 = s2;
	}
	
	public void close() {
		stream1.close();
		stream2.close();
	}

	public void add(C c) {
		stream1.add(c);
		stream2.add(c);
	}
	
	public void add(C c, boolean isMore) {
		stream1.add(c, isMore);
		stream2.add(c, isMore);
	}
	
}
