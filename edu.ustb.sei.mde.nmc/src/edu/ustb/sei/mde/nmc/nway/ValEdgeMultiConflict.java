package edu.ustb.sei.mde.nmc.nway;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;

public class ValEdgeMultiConflict {
	List<Tuple4<Integer, EObject, EAttribute, List<Object>>> first;
	ConflictKind firstKind;
	List<Tuple4<Integer, EObject, EAttribute, List<Object>>> second;
	ConflictKind secondKind;
	String information;
	
	public ValEdgeMultiConflict(List<Tuple4<Integer, EObject, EAttribute, List<Object>>> first, ConflictKind firstKind,
			List<Tuple4<Integer, EObject, EAttribute, List<Object>>> second, ConflictKind secondKind,
			String information) {
		super();
		this.first = first;
		this.firstKind = firstKind;
		this.second = second;
		this.secondKind = secondKind;
		this.information = information;
	}

	public List<Tuple4<Integer, EObject, EAttribute, List<Object>>> getFirst() {
		return first;
	}

	public void setFirst(List<Tuple4<Integer, EObject, EAttribute, List<Object>>> first) {
		this.first = first;
	}

	public ConflictKind getFirstKind() {
		return firstKind;
	}

	public void setFirstKind(ConflictKind firstKind) {
		this.firstKind = firstKind;
	}

	public List<Tuple4<Integer, EObject, EAttribute, List<Object>>> getSecond() {
		return second;
	}

	public void setSecond(List<Tuple4<Integer, EObject, EAttribute, List<Object>>> second) {
		this.second = second;
	}

	public ConflictKind getSecondKind() {
		return secondKind;
	}

	public void setSecondKind(ConflictKind secondKind) {
		this.secondKind = secondKind;
	}

	public String getInformation() {
		return information;
	}

	public void setInformation(String information) {
		this.information = information;
	}

	@Override
	public String toString() {
		return "ValEdgeMultiConflict "
				+ "\n[first=" + first + ", "
				+ "\n\nfirstKind=" + firstKind + ", "
				+ "\n\nsecond=" + second + ", "
				+ "\n\nsecondKind=" + secondKind + ", "
				+ "\n\ninformation=" + information + "]";
	}
	
}
