package net.thisptr.specialize.processor.internal.eclipse;

public class Hoge {
	
	private int hogehogeghoeg = 10;

	public int getHoge() {
		return hogehogeghoeg;
	}

	public void setHoge(int hoge) {
		this.hogehogeghoeg = hoge;
	}

	@Override
	public String toString() {
		return "Hoge [hoge=" + hogehogeghoeg + "]";
	}
}
