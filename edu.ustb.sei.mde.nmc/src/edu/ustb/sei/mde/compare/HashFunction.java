package edu.ustb.sei.mde.compare;

import org.eclipse.emf.ecore.EObject;

public interface HashFunction {
	double distance(Comparison inProgress, EObject a, EObject b);
	
	boolean areIdentic(Comparison inProgress, EObject a, EObject b);
}
