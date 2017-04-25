package aanchev.eventful;

import java.util.PriorityQueue;
import java.util.SortedSet;

class PrioritySet<E> extends PriorityQueue<E> implements SortedSet<E> {

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