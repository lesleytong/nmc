package edu.ustb.sei.mde.nmc.nway;

public class Tuple4<F, S, T, O> {
	
	public final F first;	
	public final S second;
	public final T third;
	public final O fourth;
	
	public Tuple4(F first, S second, T third, O fourth) {
		super();
		this.first = first;
		this.second = second;
		this.third = third;
		this.fourth = fourth;
	}

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	public T getThird() {
		return third;
	}

	public O getFourth() {
		return fourth;
	}

	@Override
	public String toString() {
		return "Tuple4[ "
				+ "\n分支=" + first + ", "
				+ "\n对象=" + second + ", "
				+ "\n属性或引用=" + third + ", "
				+ "\n对象=" + fourth + "]";
	}
	
}
