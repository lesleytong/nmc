package edu.ustb.sei.mde.nmc.nway;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;

public class ValEdgeConflict {
	
	List<Tuple4<Integer, EObject, EAttribute, Object>> first;
	ConflictKind firstKind;
	List<Tuple4<Integer, EObject, EAttribute, Object>> second;
	ConflictKind secondKind;
	String information;
	
	public ValEdgeConflict(List<Tuple4<Integer, EObject, EAttribute, Object>> first, ConflictKind firstKind,
			List<Tuple4<Integer, EObject, EAttribute, Object>> second, ConflictKind secondKind, String information) {
		super();
		this.first = first;
		this.firstKind = firstKind;
		this.second = second;
		this.secondKind = secondKind;
		this.information = information;
	}

	@Override
	public String toString() {
		return "ValEdgeConflict "
				+ "\n[first=" + first + ", "
				+ "\n\nfirstKind=" + firstKind + ", "
				+ "\n\nsecond=" + second + ", "
				+ "\n\nsecondKind="+ secondKind + ", "
				+ "\n\ninformation=" + information + "]";
	}
	
}
