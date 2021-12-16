package edu.ustb.sei.mde.nmc.nway;

import java.util.List;
import org.eclipse.emf.ecore.EObject;

import conflict.ConflictKind;

public class NodeConflict {
	List<Tuple2<Integer, EObject>> first;
	ConflictKind firstKind;
	List<Tuple2<Integer, EObject>> second;
	ConflictKind secondKind;
	String information;
	
	public NodeConflict(List<Tuple2<Integer, EObject>> first, ConflictKind firstKind, List<Tuple2<Integer, EObject>> second,
			ConflictKind secondKind, String information) {
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