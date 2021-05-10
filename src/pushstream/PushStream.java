package pushstream;

/**
 * 
 * A stream which gets called to deal with new elements being added; 
 * push rather than pull protocol. Abstracted over objects being pushed. 
 * 
 * @author ldixon
 *
 * @param <C>
 */
public interface PushStream<C> {

	void close();
	void add(C c);
	void add(C c, boolean isMore);
}
