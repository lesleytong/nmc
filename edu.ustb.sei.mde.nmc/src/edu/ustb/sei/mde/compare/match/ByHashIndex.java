package edu.ustb.sei.mde.compare.match;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.ustb.sei.mde.compare.EObjectIndex;

public class ByHashIndex implements EObjectIndex{

	private Map<String, EObjectIndex> allIndexes;

	// tlt
	public ByHashIndex() {
		this.allIndexes = Maps.newHashMap();
	}
	
	@Override
	public Iterable<EObject> getValuesStillThere(Side side) {
		List<Iterable<EObject>> allLists = Lists.newArrayList();
		for (EObjectIndex typeSpecificIndex : allIndexes.values()) {
			allLists.add(typeSpecificIndex.getValuesStillThere(side));
		}
		return Iterables.concat(allLists);
	}

	@Override
	public void remove(EObject eObj, Side side) {
		EObjectIndex typeSpecificIndex = getOrCreate(eObj);
		typeSpecificIndex.remove(eObj, side);
		
	}

	private EObjectIndex getOrCreate(EObject eObj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void index(EObject eObj, Side side) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<EObject> getLefts(EObject obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<EObject> getRights(EObject obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<EObject> getOrigins(EObject obj) {
		// TODO Auto-generated method stub
		return null;
	}

}
