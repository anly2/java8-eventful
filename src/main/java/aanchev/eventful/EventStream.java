package aanchev.eventful;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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


		@Deprecated
		public interface Organizing<E> extends Default<E>, EventStream.Organizing<E> {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public default Queue<Handler<E>> getHandlers() {
				return (Queue) eventStreams.computeIfAbsent(this, k -> new LinkedList<>());
			}
		}
	}

	@Deprecated
	public interface Organizing<E> extends EventStream<E> {
		@Override
		default boolean fire(E data) {
			Queue<Handler<E>> handlers;

			try {
				handlers = (Queue<Handler<E>>) getHandlers();
			}
			catch (ClassCastException e) {
				return EventStream.super.fire(data);
			}

			if (handlers == null)
				return true;

			Collection<Handler<E>> requeued = new LinkedList<>();
			try {
				Iterator<Handler<E>> it = handlers.iterator();
				Handler<E> handler;
				while (it.hasNext()) {
					handler = it.next();
					if (!handler.tryHandle(data)) {
						it.remove();
						requeued.add(handler);
					}
				}

				return true;
			}
			catch (VetoEventException e) {
				return false;
			}
			catch (ConsumeEventException e) {
				return true;
			}
			finally {
				handlers.addAll(requeued);
			}
		}
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
