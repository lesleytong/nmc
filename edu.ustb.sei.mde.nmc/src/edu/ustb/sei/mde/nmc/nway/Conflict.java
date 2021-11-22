package edu.ustb.sei.mde.nmc.nway;

import java.util.List;

public class Conflict {
	List<Integer> first;
	ConflictKind firstKind;
	List<Integer> second;
	ConflictKind secondKind;
	String information;
	
	public Conflict(List<Integer> first, ConflictKind firstKind, List<Integer> second, ConflictKind secondKind,
			String information) {
		super();
		this.first = first;
		this.firstKind = firstKind;
		this.second = second;
		this.secondKind = secondKind;
		this.information = information;
	}

	@Override
	public String toString() {
		return "Conflict [first=" + first + ", firstKind=" + firstKind + ", second=" + second + ", secondKind="
				+ secondKind + ", information=" + information + "]";
	}
	
}