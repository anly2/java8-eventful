package aanchev.eventful;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import aanchev.eventful.Eventful.Handler;
import aanchev.eventful.Eventful.Ranked.RankedHandler;

public class EventfulTest {
	
	public static class Expectations<E> {
		private List<E> expected;
		private int i = 0;

		
		@SuppressWarnings("unchecked")
		public Expectations(E... expected) {
			this(Arrays.asList(expected));
		}
		
		public Expectations(List<E> expected) {
			this.expected = expected;
		}
		
		
		public void is(E value) {
			if (i >= expected.size())
				fail("Expected only "+expected.size()+" value(s), but got a "+(i+1)+"-th");
			
			E e = expected.get(i);

			assertEquals(e, value);
			i++;
		}
	
		public void allMet() {
			assertEquals("Not all Expectations were met.", expected.size(), i);
		}
	}


	@Test
	public void testBasic() {
		Eventful<Exception> eventful = new Eventful<Exception>() {
			private Map<String, Set<Handler<Exception>>> handlers = new HashMap<>();

			public Map<String, Set<Handler<Exception>>> getHandlers() {
				return this.handlers;
			}
			
		};

		Exception[] exceptions = {
			new IllegalArgumentException("first"),	
			new IllegalStateException("second")	
		};
		
		Expectations<Exception> expected = new Expectations<>(exceptions);
		
		eventful.on("myevent", e -> expected.is(e));
		
		for (Exception e : exceptions)
			eventful.fire("myevent", e);
		
		expected.allMet();
	}

	@Test
	public void testKeys() {
		Eventful<Exception> eventful = new Eventful<Exception>() {
			private Map<String, Set<Handler<Exception>>> handlers = new HashMap<>();

			public Map<String, Set<Handler<Exception>>> getHandlers() {
				return this.handlers;
			}
			
		};

		Exception[] exceptions = {
			new IllegalArgumentException("first"),	
			new IllegalStateException("second")	
		};
		
		Expectations<Exception> expected0 = new Expectations<>(exceptions[0]);
		Expectations<Exception> expected1 = new Expectations<>(exceptions[1]);
		
		eventful.on("myevent0", (IllegalArgumentException e) -> expected0.is(e));
		eventful.on("myevent1", (IllegalStateException e) -> expected1.is(e));
		
		eventful.fire("myevent0", exceptions[0]);
		eventful.fire("myevent1", exceptions[1]);
		
		expected0.allMet();
		expected1.allMet();
	}

	@Test
	public void testMorph() {
		Eventful<Exception> eventful = new Eventful<Exception>() {
			private Map<String, Set<Handler<Exception>>> handlers = new HashMap<>();

			public Map<String, Set<Handler<Exception>>> getHandlers() {
				return this.handlers;
			}
			
		};

		Exception[] exceptions = {
			new IllegalArgumentException("first"),	
			new IllegalStateException("second")	
		};
		
		Expectations<Exception> expected0 = new Expectations<>(exceptions[0]);
		Expectations<Exception> expected1 = new Expectations<>(exceptions[1]);
		
		eventful.on("myevent", (IllegalArgumentException e) -> expected0.is(e));
		eventful.on("myevent", (IllegalStateException e) -> expected1.is(e));
		
		for (Exception e : exceptions)
			eventful.fire("myevent", e);
		
		expected0.allMet();
		expected1.allMet();
	}

	@Test
	public void testWild() {
		Eventful<Object> eventful = new Eventful<Object>() {
			private Map<String, Set<Handler<Object>>> handlers = new HashMap<>();

			public Map<String, Set<Handler<Object>>> getHandlers() {
				return this.handlers;
			}
			
		};

		Exception[] exceptions = {
			new IllegalArgumentException("first"),	
			new IllegalStateException("second")	
		};
		
		Expectations<Exception> expected0 = new Expectations<>(exceptions[0]);
		Expectations<Exception> expected1 = new Expectations<>(exceptions[1]);
		
		eventful.on("myevent", (IllegalArgumentException e) -> expected0.is(e));
		eventful.on("myevent", (IllegalStateException e) -> expected1.is(e));
		
		for (Exception e : exceptions)
			eventful.fire("myevent", e);
		
		expected0.allMet();
		expected1.allMet();
	}

	@Test
	public void testRemoval() {
		Eventful<Exception> eventful = new Eventful<Exception>() {
			private Map<String, Set<Handler<Exception>>> handlers = new HashMap<>();

			public Map<String, Set<Handler<Exception>>> getHandlers() {
				return this.handlers;
			}
			
		};

		Exception[] exceptions = {
			new IllegalArgumentException("first"),	
			new IllegalStateException("second")	
		};
		
		Expectations<Exception> expected = new Expectations<>(exceptions[0]);
		
		Handler<? extends Exception> handler = eventful.on("myevent", e -> expected.is(e));
		eventful.fire("myevent", exceptions[0]);
		eventful.off("myevent", handler);
		eventful.fire("myevent", exceptions[1]);
		
		expected.allMet();
	}
	
	
	@Test
	public void testRanked() {
		Eventful.Ranked<Exception> eventful = new Eventful.Ranked<Exception>() {
			private Map<String, Set<Handler<Exception>>> handlers = new HashMap<>();

			public Map<String, Set<Handler<Exception>>> getHandlers() {
				return this.handlers;
			}
		};
		
		Exception exc = new Exception("A");
		
		Expectations<Integer> expected = new Expectations<>(1, 23, 23, 4, 5);
		
		eventful.on("myevent", e -> expected.is(4), 40);
		eventful.on("myevent", e -> expected.is(23), 20); // 2 or 3
		eventful.on("myevent", e -> expected.is(1), 10);
		eventful.on("myevent", e -> expected.is(23), 20); // 2 or 3
		eventful.on("myevent", e -> expected.is(5), 100);
		
		eventful.fire("myevent", exc);
		
		expected.allMet();
	}

	@Test
	public void testRankedRemoval() {
		Eventful.Ranked<Exception> eventful = new Eventful.Ranked<Exception>() {
			private Map<String, Set<Handler<Exception>>> handlers = new HashMap<>();

			public Map<String, Set<Handler<Exception>>> getHandlers() {
				return this.handlers;
			}
		};

		Exception[] exceptions = {
			new IllegalArgumentException("first"),	
			new IllegalStateException("second"),
			new IllegalAccessException("third"),
			new IllegalMonitorStateException("fourth"),
			new IOException("fifth"),
			new FileNotFoundException("sixth")
		};
		
		Expectations<Exception> expected = new Expectations<>(exceptions[0], exceptions[2], exceptions[4]);
		
		Handler<? extends Exception> handler0 = eventful.on("myevent", e -> expected.is(e));
		eventful.fire("myevent", exceptions[0]);
		eventful.off("myevent", handler0);
		eventful.fire("myevent", exceptions[1]);
		
		RankedHandler<? extends Exception> handler1 = eventful.on("myevent", e -> expected.is(e), 0);
		eventful.fire("myevent", exceptions[2]);
		eventful.off("myevent", handler1);
		eventful.fire("myevent", exceptions[3]);
		
		Handler<? extends Exception> handler2 = e -> expected.is(e);
		eventful.on("myevent", handler2);
		eventful.fire("myevent", exceptions[4]);
		eventful.off("myevent", handler2);
		eventful.fire("myevent", exceptions[5]);
		
		expected.allMet();
	}
	

	@Test
	public void testDefault() {
		Eventful<Exception> eventful = new Eventful.Default<Exception>() {};

		Exception[] exceptions = {
			new IllegalArgumentException("first"),	
			new IllegalStateException("second")	
		};
		
		Expectations<Exception> expected = new Expectations<>(exceptions);
		
		eventful.on("myevent", (Exception e) -> expected.is(e));
		
		for (Exception e : exceptions)
			eventful.fire("myevent", e);
		
		expected.allMet();
	}

	@Test
	public void testRankedDefault() {
		Eventful.Ranked<Exception> eventful = new Eventful.Ranked.Default<Exception>() {};
		
		Exception exc = new Exception("A");
		
		Expectations<Integer> expected = new Expectations<>(1, 23, 23, 4, 5);
		
		eventful.on("myevent", e -> expected.is(4), 40);
		eventful.on("myevent", e -> expected.is(23), 20); // 2 or 3
		eventful.on("myevent", e -> expected.is(1), 10);
		eventful.on("myevent", e -> expected.is(23), 20); // 2 or 3
		eventful.on("myevent", e -> expected.is(5), 100);
		
		eventful.fire("myevent", exc);
		
		expected.allMet();
	}

}
