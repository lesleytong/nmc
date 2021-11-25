package edu.ustb.sei.mde.nmc.compare;

import org.eclipse.emf.ecore.EObject;

public interface HashFunction {

	boolean areIdentic(Comparison inProgress, EObject eObj, EObject fastCheck);
	//to be implement in MatchComputationByHash

	double distance(Comparison inProgress, EObject eObj, EObject potentialClosest);
	//to be implement in MatchComputationByHash
}
