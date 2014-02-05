package net.thisptr.specialize.example;

// these imports will be removed after specialization
import net.thisptr.specialize.Specialize;
import net.thisptr.specialize.*;

public class EclipseDebug {

	@Specialize("Score: double, ?")
	public static class BasicScoredItem<Item, Score> {
		public final Item item;
		public final Score score;

		public BasicScoredItem(final Item item, final Score score) {
			this.item = item;
			this.score = score;
		}
	}
	
	public static class BasicScoredItem$specialized$_$double<Item> {
		public final Item item;
		public final double score;
		
		public BasicScoredItem$specialized$_$double(final Item item, final double score) {
			this.item = item;
			this.score = score;
		}
	}

	public static class ScoredItem<Item> extends BasicScoredItem<Item, $double> {
		public ScoredItem(final Item item, final double score) {
			super(item, score);
		}
	}
	
	@Specialize("T: int, float")
	public static <T> T sum(final T[] values) {
		T result = 0;
		for (final T value : values)
			result += value;
		return result;
	}

	// done
	public static void main2() {
		final BasicScoredItem<String, $double> a = new BasicScoredItem<String, $double>("hoge", 2.0);
		final double aScore = a.score;
		System.out.println(aScore);

		final BasicScoredItem<String, Integer> c = new BasicScoredItem<String, Integer>("fuga", 2);

//		final ScoredItem<String> b = new ScoredItem<String>("hoge", 2.0);
//		final double bScore = b.score;
//		System.out.println(bScore);
	}
}