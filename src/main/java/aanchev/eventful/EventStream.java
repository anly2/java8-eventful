package aanchev.eventful;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

//TODO tests
//TODO javadoc
public interface EventStream<E> {

	/* State Getters */
	// may need overriding

	public Collection<Handler<E>> getHandlers();


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
		Iterable<Handler<E>> handlers = getHandlers();

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
		final static Map<Object, Collection<Handler<Object>>> eventStreams = new HashMap<>();

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public default Collection<Handler<E>> getHandlers() {
			return (Set) eventStreams.computeIfAbsent(this, k -> new HashSet<>());
		}
	}

	public interface Organizing<E> extends EventStream<E> {
		final static Map<Object, Map<Class<?>, Collection<Handler<Object>>>> eventCaches = new HashMap<>();

		@Override
		default Handler<? extends E> on(Handler<? extends E> handler) {
			//can potentially be done better, but not worth the effort considering the usual use case
			eventCaches.remove(this);

			return EventStream.super.on(handler);
		}

		@SuppressWarnings("unchecked")
		@Override
		default boolean fire(E data) {
			Map<Class<?>, Collection<Handler<Object>>> eventCache = eventCaches.computeIfAbsent(this, k -> new HashMap<>());

			Collection<Handler<Object>> cached = eventCache.get(data.getClass());

			if (cached == null) {
				cached = new LinkedList<>();
				eventCache.put(data.getClass(), cached);
				Collection<Handler<E>> handlers = getHandlers();

				if (handlers == null)
					return true;

				try {
					for (Handler<?> handler : handlers) {
						if (handler.tryHandle(data))
							cached.add((Handler<Object>) handler);
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
			else {
				try {
					for (Handler<?> handler : cached) {
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
		}


		public interface Default<E> extends Organizing<E>, EventStream.Default<E> {}
	}

	public interface Ranked<E> extends EventStream.Default<E> {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public default Set<Handler<E>> getHandlers() {
			return (Set) EventStream.Default.eventStreams.computeIfAbsent(this, k -> new PrioritySet<>());
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
