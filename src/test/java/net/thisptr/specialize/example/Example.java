package net.thisptr.specialize.example;

import net.thisptr.specialize.annotation.InjectPrimitive;
import net.thisptr.specialize.annotation.Specialize;

public class Example {

	@Specialize({
		"T: int, double, ?",
		"U: int, double, ?"
	})
	public static class Pair<T, U> {
		public final T first;
		public final U second;

		public Pair(final T first, final U second) {
			this.first = first;
			this.second = second;
		}
	}

	@Specialize("Score: int, double")
	public static class BasicScoredItem<Item, Score> {
		public final Item item;
		public final Score score;

		public BasicScoredItem(final Item item, final Score score) {
			this.item = item;
			this.score = score;
		}
	}

	@InjectPrimitive("T: double")
	public static class ScoredItem<Item> extends BasicScoredItem<Item, T> {
		public ScoredItem(final Item item, final T score) {
			super(item, score);
		}
	}

	@InjectPrimitive("T: int")
	public static class IntIntPair extends Pair<T, T> {
		public IntIntPair(final T first, final T second) {
			super(first, second);
		}
	}

	@InjectPrimitive({
		"T: int",
		"U: double"
	})
	public static class IntDoublePair extends Pair<T, U> {
		public IntDoublePair(final T first, final U second) {
			super(first, second);
		}
	}

	@Specialize("T: int, float, ?")
	public static class AtomicValue<T> {
		private T value;

		public AtomicValue(final T value) {
			this.value = value;
		}

		public synchronized T getValue() {
			return value;
		}

		public synchronized void setValue(final T value) {
			this.value = value;
		}
	}

	@Specialize("T: int, float")
	@InjectPrimitive("U: int")
	public static <T> T someProcess(final AtomicValue<T> i, final AtomicValue<U> p) {
		return i.getValue() + p.getValue();
	}

	@Specialize("T: int, float")
	public static <T> T sum(final T[] values) {
		T result = 0;
		for (final T value : values)
			result += value;
		return result;
	}

	public void main() {
		AtomicValue<int> hoge =  new AtomicValue<int>(10);
		hoge = new AtomicValue<int>(20);
		hoge.setValue(20);
		final int value = hoge.getValue();
		System.out.println(value);

		final BasicScoredItem<String, double> a = new BasicScoredItem<String, double>("hoge", 2.0);
		final double aScore = a.score;
		System.out.println(aScore);

		final ScoredItem<String> b = new ScoredItem<String>("hoge", 2.0);
		final double bScore = b.score;
		System.out.println(bScore);

		final int value2 = someProcess(hoge, hoge);
	}
}
