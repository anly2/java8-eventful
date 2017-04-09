package aanchev.eventful;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;

import aanchev.eventful.Eventful.Ranked.PrioritySet;


/**
 * An interface that provides an Event Bus/System implementation through default methods.
 * 
 * <p>
 * The main methods of interest are:
 * <ul>
 *  <li>{@link #on(String, Handler)} which associates the event handler with an event name (string)</li>
 *  <li>{@link #fire(String, Object)} which triggers the event with the specified name, passing the object as event data</li>
 *  <li>{@link #off(String, Handler)} which de-associates a handler from an event name</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The most important feature is that handlers are only called IFF the Event Data Object is assignable as a parameter.
 * In other words, if a handler accepts {@link java.io.IOException IOExceptions}
 * but an event is fired with a {@link java.lang.RuntimeException RuntimeException}
 * then that handler will be silently ignored and not called.
 * <br />
 * This allow for a convenient use of the Type Hierarchy without arduous casts.
 * </p>
 * 
 * <p>
 * The functionality was designed with Java8's Lambdas in mind.
 * Attaching Lambda handlers is as easy as possible.
 * In addition to the {@link Handler} interface being a Functional Interface,
 * the instances are also returned so that references to the lambda objects are not lost.
 * This allows:
 * <pre>{@code
 * Handler<?> handler = eventData -> consumer.accept(eventData);
 *eventful.on("some-event", handler);
 * }</pre>
 * to be rewritten as:
 * <pre>{@code
 * Handler<?> handler = eventful.on("some-event", eventData -> consumer.accept(eventData));
 * }</pre>
 * </p>
 * 
 * <br />
 * 
 * <p>
 * There are several interfaces that can actually be used, depending on the situation:
 * <ul>
 * 	<li>{@link Eventful}</li>
 * 	<li>{@link Eventful.Default}</li>
 * 	<li>{@link Eventful.Ranked}</li>
 * 	<li>{@link Eventful.Ranked.Default}</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The {@link Eventful} interface provides the main functionality,
 * but delegates the state handling to the actual implementing class.
 * There is still only one method that needs to be overriden - {@link Eventful#getHandlers()}.
 * </p>
 * 
 * <p>
 * The {@link Eventful.Default} interface manages state through the use of a global static hash map.
 * This makes it possible to just declare that the interface is implemented
 * and get all the functionality for free, without needing to override anything.
 * However, if the instances (the `this` objects) are not suitable for keys in a Hash Map,
 * then do not use this convenience sub-interface, but implement the {@link Eventful} one.
 * </p>
 * 
 * <p>
 * The {@link Eventful.Ranked} interface extends the {@link Eventful} functionality,
 * by allowing handlers to have priorities.
 * This way, an ordering can be imposed that is independent of the order of addition.
 * The ordering is maintained by a {@link PrioritySet} (== {@link PriorityQueue}&{@link Set}),
 * so to allow duplicates (same priority, but different handlers).
 * <br/>
 * The additional method of interest is {@link Eventful.Ranked#on(String, Handler, int)}.
 * An overload for {@link #off(String, Handler) off()} is provided, but no special attention is needed for handler removal.
 * <br/>
 * <b>NOTE:</b> Handlers with the same priority have an undefined order of invocation!
 * </p>
 * 
 * <p>
 * The {@link Eventful.Ranked.Default} interface simply reuses the {@link Eventful.Default} state management,
 * but for the {@link Eventful.Ranked} functionality.
 * </p>
 * 
 * @author Anko Anchev
 *
 * @param <E> the type of events that will be firing. Can easily be Object!
 * 
 * @see Eventful.Default
 * @see Eventful.Ranked
 * @see Eventful.Ranked.Default
 */
public interface Eventful<E> {
	
	/* State Getters */
	// may need overriding
	
	public Map<String, Set<Handler<E>>> getHandlers();
	
	public default Set<Handler<E>> getHandlers(String event) {
		return getHandlers().computeIfAbsent(event, k -> new HashSet<Handler<E>>());
	}

	
	/* Handler/Listener Attaching */
	
	@SuppressWarnings("unchecked")
	public default Handler<? extends E> on(String event, Handler<? extends E> handler) {
		getHandlers(event).add((Handler<E>) handler);
		return handler;
	}
	
	public default boolean off(String event, Handler<? extends E> handler) {
		return getHandlers(event).remove(handler);
	}
	
	
	/* Firing of Events */
	
	public default boolean fire(String event) {
		return fire(event, null);
	}
	
	public default boolean fire(String event, E data) {
		Set<Handler<E>> handlers = getHandlers(event);
		
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
	
	
	/* Inner Types and Exceptions */
	
	/**
	 * A Functional Interface (like {@link java.util.function.Consumer Consumer})
	 * that can potentially throw {@link VetoEventException} or {@link ConsumeEventException}.
	 */
	public interface Handler<DATA> {
		public void handle(DATA event) throws VetoEventException, ConsumeEventException;
		
		@SuppressWarnings("unchecked")
		public default boolean tryHandle(Object data) throws VetoEventException, ConsumeEventException {
			try {
				handle((DATA) data);
				return true;
			}
			catch (ClassCastException e) {
				return false;
			}
		}
	}

	
	/**
	 * An exception that allows a handler to stop the invocation of subsequent handlers for that event.
	 * This is very similar to {@link VetoEventException},
	 * except the triggering call to {@link Eventful#fire(String, Object)} will return with <code>true</code>,
	 * indicating a successful, albeit premature completion.
	 */
	public static class ConsumeEventException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * An exception that allows a handler to stop the invocation of subsequent handlers for that event.
	 * This is very similar to {@link ConsumeEventException},
	 * except the triggering call to {@link Eventful#fire(String, Object)} will return with <code>false</code>,
	 * indicating a failure.
	 * For more specific error indications, custom runtime exceptions can be thrown.
	 */
	public static class VetoEventException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	
	/* Specialized Implementations */
	
	/**
	 * A sub-interface of {@link Eventful} that also manages state!
	 * Use this as a convenient whole implementation of {@link Eventful}.
	 * 
	 * <p>
	 * Be aware that the implementation makes use of a {@link java.util.HashMap HashMap},
	 * with the {@link Eventful} object instances as keys!
	 * So if the instances (the `this` objects) are not suitable for keys in a Hash Map,
	 * do not use this convenience sub-interface, but implement the {@link Eventful} one!
	 * </p>
	 * 
	 * @see Eventful
	 */
	public interface Default<E> extends Eventful<E> {
		final static Map<Object, Map<String, Set<Handler<Object>>>> eventMaps = new HashMap<>();
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public default Map<String, Set<Handler<E>>> getHandlers() {
			return (Map<String, Set<Handler<E>>>) (Map) eventMaps.computeIfAbsent(this, k -> new HashMap<>());
		}
	}


	/**
	 * A sub-interface of {@link Eventful} that extends the functionality
	 * by allowing handlers to have priorities.
	 * 
	 * <p>
	 * Using this, an ordering can be imposed that is independent of the order of addition.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> Handlers with the same priority have an undefined order of invocation!
	 * </p>
	 * 
	 * <p>
	 * The ordering is maintained by a {@link PrioritySet} (== {@link PriorityQueue}&{@link Set}),
	 * so to allow duplicates (same priority, but different handlers).
	 * </p>
	 * 
	 * <p>
	 * The additional method of interest is {@link Eventful.Ranked#on(String, Handler, int)}.
	 * An overload for {@link #off(String, Handler) off()} is provided, but no special attention is needed for handler removal.
	 * </p>
	 * 
	 * @see Eventful
	 * @see Eventful.Ranked.Default
	 */
	public interface Ranked<E> extends Eventful<E> {

		@Override
		default Set<Handler<E>> getHandlers(String event) {
			return getHandlers().computeIfAbsent(event, k -> new PrioritySet<Handler<E>>());
		}
		
		
		@Override
		default Handler<? extends E> on(String event, Handler<? extends E> handler) {
			return this.on(event, handler, 0);
		}
		
		default RankedHandler<? extends E> on(String event, Handler<? extends E> handler, int priority) {
			RankedHandler<? extends E> rankedHandler = new RankedHandler<>(handler, priority);
			
			Eventful.super.on(event, rankedHandler);
			
			return rankedHandler;
		}
		
		
		@Override
		default boolean off(String event, Handler<? extends E> handler) {
			return this.off(event, new RankedHandler<>(handler, 0));
		}
		
		default boolean off(String event, RankedHandler<? extends E> handler) {
			return getHandlers(event).remove(handler);
		}
		

		public static class RankedHandler<DATA> implements Handler<DATA>, Comparable<RankedHandler<DATA>> {
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
		
		public static class PrioritySet<E> extends PriorityQueue<E> implements SortedSet<E> {

			@Override
			public E first() {
				return super.peek();
			}

			@Override
			public E last() {
				throw new UnsupportedOperationException();
			}

			
			@Override
			public SortedSet<E> headSet(E pivot) {
				throw new UnsupportedOperationException();
			}

			@Override
			public SortedSet<E> tailSet(E pivot) {
				throw new UnsupportedOperationException();
			}

			
			@Override
			public SortedSet<E> subSet(E start, E end) {
				throw new UnsupportedOperationException();
			}
		}

		
		/**
		 * A sub-interface of {@link Eventful.Ranked} that also adds the convenience of {@link Eventful.Default}.
		 * 
		 * <p>
		 * The {@link Eventful.Ranked.Default} interface simply reuses the {@link Eventful.Default} state management,
		 * but for the {@link Eventful.Ranked} functionality.
		 * </p>
		 * 
		 * @see Eventful
		 * @see Eventful.Ranked
		 * @see Eventful.Default
		 */
		public interface Default<E> extends Ranked<E>, Eventful.Default<E> {}
	}
}
