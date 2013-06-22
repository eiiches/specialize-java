package net.thisptr.specialize.example;

// these imports shall be removed
import net.thisptr.specialize.Specialize;
import net.thisptr.specialize.*;

@Specialize({"Q: int, ?"})
public class Example<Q> {

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

	@Specialize("Score: *, ?")
	public static class BasicScoredItem<Item, Score> {
		public final Item item;
		public final Score score;

		public BasicScoredItem(final Item item, final Score score) {
			this.item = item;
			this.score = score;
		}
	}

	public static class ScoredItem<Item> extends BasicScoredItem<Item, $double> {
		public ScoredItem(final Item item, final double score) {
			super(item, score);
		}
	}

	public static class IntDoublePair extends net.thisptr.specialize.example.Example.Pair<$int, $double> {
		public IntDoublePair(final int first, final double second) {
			super(first, second);
		}
	}

	@Specialize("T: int, float, boolean, ?")
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

	public static AtomicValue<$boolean> b = new AtomicValue<$boolean>(true);

	@Specialize("T: int, float")
	public static <T> T someProcess(final AtomicValue<T> i, final AtomicValue<$int> p) {
		return i.getValue() + p.getValue();
	}

	@Specialize("T: int, float")
	public static <T> T sum(final T[] values) {
		T result = 0;
		for (final T value : values)
			result += value;
		return result;
	}

	// done
	public static void main1() {
		AtomicValue<$int> hoge = new AtomicValue<$int>(10);
		hoge = new AtomicValue<$int>(20);
		hoge.setValue(20);
		final int value = hoge.getValue();
		System.out.println(value);
		final int value2 = someProcess(hoge, hoge);
	}

	// done
	public static void main2() {
		final BasicScoredItem<String, $double> a = new BasicScoredItem<String, $double>("hoge", 2.0);
		final double aScore = a.score;
		System.out.println(aScore);

		final ScoredItem<String> b = new ScoredItem<String>("hoge", 2.0);
		final double bScore = b.score;
		System.out.println(bScore);

		new BasicScoredItem<String, $double>("hoge", 2.0) {
			{
				final double a = this.score;
			}
		};

		final BasicScoredItem<BasicScoredItem<String, $double>, $double> c = new BasicScoredItem<BasicScoredItem<String, $double>, $double>(a, 10.0);
	}

	public static void candidate5() {
		AtomicValue</* @int */Integer> hoge = new AtomicValue</* @int */Integer>(10);
		hoge = new AtomicValue</* @int */Integer>(20);

		AtomicValue</* @int */Integer> fuga = new AtomicValue<>(10);
		fuga = new AtomicValue<>(20);
	}

//	public static void candidate4() {
//		@Specialized("<int>") AtomicValue<Integer> hoge = new AtomicValue<Integer>(10);
//		hoge = new AtomicValue<Integer>(20);
//
//		@Specialized("<int>") AtomicValue<Integer> fuga = new AtomicValue<>(10);
//		fuga = new AtomicValue<>(20);
//	}

	public static void candidate3() {
		AtomicValue<$int> hoge = new AtomicValue<$int>(10);
		hoge = new AtomicValue<$int>(20);

		hoge.setValue(20);
		final int value = hoge.getValue();
		System.out.println(value);
		final int value2 = someProcess(hoge, hoge);

//		AtomicValue<int> fuga = new AtomicValue<>(10);
//		fuga = new AtomicValue<>(20);
	}

//	public static void candidate2() {
//		AtomicValue<_int> hoge = new AtomicValue<_int>(10);
//		hoge = new AtomicValue<_int>(20);
//
//		hoge.setValue(20);
//		final int value = hoge.getValue();
//		System.out.println(value);
//		final int value2 = someProcess(hoge, hoge);
//
////		AtomicValue<int> fuga = new AtomicValue<>(10);
////		fuga = new AtomicValue<>(20);
//	}

//	public static void candidate1() {
//		AtomicValue<int> hoge = new AtomicValue<int>(10);
//		hoge = new AtomicValue<int>(20);
//
////		AtomicValue<int> fuga = new AtomicValue<>(10);
////		fuga = new AtomicValue<>(20);
//	}

	public static int test(final int a) {
		int b = 10;
		Class<?> p = int.class;

		for (int i = 0; i < 10; ++i) {

		}

		return a;
	}

}

@Specialize("R: float, ?")
class Hoge<R> {

}