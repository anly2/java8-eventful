package aanchev.eventful;


public class RankedHandler<DATA> implements Handler<DATA>, Comparable<RankedHandler<DATA>> {
	private final Handler<DATA> handler;
	public final int priority;
	
	
	/* Constructors */
	
	public RankedHandler(Handler<DATA> handler, int priority) {
		this.handler = handler;
		this.priority = priority;
	}

	
	/* Proxy Handler */
	
	public void handle(DATA event) throws VetoEventException, ConsumeEventException {
		handler.handle(event);
	}
	
	@Override
	public boolean tryHandle(Object data) throws VetoEventException, ConsumeEventException {
		return handler.tryHandle(data);
	}
	
	
	/* Comparable Contract  */
	
	@Override
	public int compareTo(RankedHandler<DATA> other) {
		if (this.handler == other.handler)
			return 0;
		
		return Integer.compare(this.priority, other.priority);
	}
	
	
	/* Proxy Object */
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RankedHandler) {
			@SuppressWarnings("rawtypes")
			RankedHandler other = (RankedHandler) obj;
			return ((this.handler.equals(other.handler)) );//&& (this.priority == other.priority));	
		}

		if (obj instanceof Handler)
			return this.handler.equals(obj);

		return false;
	}
	
	@Override
	public int hashCode() {
		return handler.hashCode(); // + priority;
	}
}