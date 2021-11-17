package edu.ustb.sei.mde.compare.match;

import java.util.Iterator;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.compare.Comparison;
import edu.ustb.sei.mde.compare.IEObjectMatcher;

public class HashEObjectMatcher implements IEObjectMatcher	{

	@Override
	public void createMatches(Comparison comparison, Iterator<? extends EObject> leftEObjects,
			Iterator<? extends EObject> rightEObjects, Iterator<? extends EObject> originEObjects, MultiKeyMap<EObject, Double> distanceMap) {
		
		
		
	}
	
	// Constructor
	public HashEObjectMatcher() {
		
	}
	
}
