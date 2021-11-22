package edu.ustb.sei.mde.nmc.compare;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.eclipse.emf.ecore.EObject;

public interface IDFunction extends HashFunction{

	void compute(MultiKeyMap<EObject, Double> distanceMap);
}
