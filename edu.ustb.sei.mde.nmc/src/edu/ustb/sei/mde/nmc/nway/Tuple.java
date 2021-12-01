package edu.ustb.sei.mde.nmc.nway;

//二元组
public class Tuple<F, S> {
	
	public final F first;	
	// lyt-为了检测强制序的环冲突，暂时去掉final修饰符
	public S second;
	
	public Tuple(F first, S second) {
		super();
		this.first = first;
		this.second = second;
	}
	
	static public <F,S> Tuple<F,S> make(F f, S s) {
		return new Tuple<F, S>(f, s);
	}
	
	@Override
	public int hashCode() {
		int firstCode = first==null ? 0 : first.hashCode();
		int secondCode = second==null ? 0 : second.hashCode();
		return ((firstCode&0xFFFF)<<16) & (secondCode&0xFFFF);
	}
	
	//判断两个元组相等的方法<a,b>=<c, d>当且仅当a=c且b=d
	@SuppressWarnings("rawtypes")
	public boolean equals(Object o) {
		if(o==null || ! (o instanceof Tuple))
			return false;
		else return ((first!=null && first.equals(((Tuple)o).first)) || (first==null && ((Tuple)o).first==null))
				&& ((second!=null && second.equals(((Tuple)o).second)) || (second==null && ((Tuple)o).second==null));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static private final Tuple EMPTY = new Tuple(null, null);
	
	@SuppressWarnings("unchecked")
	static public <X,Y> Tuple<X,Y> emptyTuple() {
		return (Tuple<X,Y>)EMPTY;
	}

	
	public String toString() {
		return "<"+first+","+second+">";
	}
	
	public Tuple<F,S> replaceFirst(F f) {
		return Tuple.make(f, second);
	}
	
	// 注意replaceSecond是返回一个新对象
	public Tuple<F,S> replaceSecond(S s) {
		return Tuple.make(first, s);
	}
}
