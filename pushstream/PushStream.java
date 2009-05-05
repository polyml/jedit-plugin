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

	public abstract void close();
	public abstract void add(C c);
	public abstract void add(C c, boolean isMore);
}
