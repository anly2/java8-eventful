package aanchev.eventful;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class OrganizingEventStreamTest {


	private static class Elem {
		private static class A extends Elem {}
		private static class B extends Elem {}
	}


	protected static class Avg {
		private double avg = 0;
		private int cnt = 0;

		public double get() {
			return avg;
		}

		public void add(long i) {
			avg = ((avg * cnt) + i) / (cnt+1);
			cnt++;
		}
	}


	@Test
	public void reportSpeedSame() {
		System.out.println("Report with Same streams...");
		reportSpeedComparison(
				new EventStream.Default<Elem>(){},
				new EventStream.Default<Elem>(){});
	}

	@Test
	public void reportSpeedDiff() {
		System.out.println("Report with Different streams...");
		reportSpeedComparison(
				new EventStream.Default<Elem>(){},
				new EventStream.Organizing.Default<Elem>(){});
	}

	public static void reportSpeedComparison(EventStream<Elem> evn0, EventStream<Elem> evn1) {
		int Nf = 10, Nt = 5;
		int Nc = 10_000_000;

		List<Handler<? extends Elem>> handlers = new ArrayList<>(Nf+Nt);
		for (int i=0; i<Nf+Nt; i++)
			handlers.add(i<Nt? ((Elem.A e) -> act(e)) : ((Elem.B e) -> act(e)));
		Collections.shuffle(handlers);

		for (Handler<? extends Elem> handler : handlers) {
			evn0.on(handler);
			evn1.on(handler);
		}


		long t0;
		Avg avg0 = new Avg();
		Avg avg1 = new Avg();

		for (int i=0; i<Nc; i++) {
			final Elem data = new Elem.A();

			t0 = System.nanoTime();
			evn0.fire(data);
			avg0.add(System.nanoTime() - t0);

			t0 = System.nanoTime();
			evn1.fire(data);
			avg1.add(System.nanoTime() - t0);
		}

		System.out.println("Average firing times for "+Nc+" prcs:");
		System.out.format("\tEventStream 0:  %6f%n", avg0.get());
		System.out.format("\tEventStream 1:  %6f%n", avg1.get());
	}

	private static <E> void act(E e) {
//		System.out.println("Caught fire: " + e);
	}
}
