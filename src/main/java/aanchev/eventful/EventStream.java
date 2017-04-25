package aanchev.eventful;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//TODO tests
//TODO javadoc
public interface EventStream<E> {

	/* State Getters */
	// may need overriding
	
	public Set<Handler<E>> getHandlers();

	
	/* Handler/Listener Attaching */
	
	@SuppressWarnings("unchecked")
	public default Handler<? extends E> on(Handler<? extends E> handler) {
		getHandlers().add((Handler<E>) handler);
		return handler;
	}
	
	public default boolean off(Handler<? extends E> handler) {
		return getHandlers().remove(handler);
	}

	
	/* Firing of Events */
	
	public default boolean fire(E data) {
		Set<Handler<E>> handlers = getHandlers();
		
		if (handlers == null)
			return true;
		
		try {
			for (Handler<?> handler : handlers) {
				handler.tryHandle(data);
			}
			
			return true;
		}
		catch (VetoEventException e) {
			return false;
		}
		catch (ConsumeEventException e) {
			return true;
		}
	}
	
	
	/* Specialized Implementations */
	
	public interface Default<E> extends EventStream<E> {
		final static Map<Object, Set<Handler<Object>>> eventStreams = new HashMap<>();
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public default Set<Handler<E>> getHandlers() {
			return (Set<Handler<E>>) (Set) eventStreams.computeIfAbsent(this, k -> new HashSet<>());
		}
	}
	
	public interface Ranked<E> extends EventStream.Default<E> {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public default Set<Handler<E>> getHandlers() {
			return (Set<Handler<E>>) (Set) EventStream.Default.eventStreams.computeIfAbsent(this, k -> new PrioritySet<>());
		}
		
		
		@Override
		default Handler<? extends E> on(Handler<? extends E> handler) {
			return this.on(handler, 0);
		}

		default RankedHandler<? extends E> on(Handler<? extends E> handler, int priority) {
			RankedHandler<? extends E> rankedHandler = new RankedHandler<>(handler, priority);
			
			EventStream.Default.super.on(rankedHandler);
			
			return rankedHandler;
		}
		

		@Override
		default boolean off(Handler<? extends E> handler) {
			return this.off(new RankedHandler<>(handler, 0));
		}
		
		default boolean off(RankedHandler<? extends E> handler) {
			return getHandlers().remove(handler);
		}
	}
}
