package edu.ustb.sei.mde.nmc.compare;

import java.math.BigInteger;

import org.eclipse.emf.ecore.EObject;

public interface HashFunction {

	boolean areIdentic(BigInteger one, BigInteger two);
	//to be implement in MatchComputationByHash

	double distance(BigInteger one, BigInteger two);
	//to be implement in MatchComputationByHash
}
