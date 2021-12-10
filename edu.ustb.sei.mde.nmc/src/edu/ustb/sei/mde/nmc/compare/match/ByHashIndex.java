package edu.ustb.sei.mde.nmc.compare.match;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.ustb.sei.mde.nmc.compare.EObjectIndex;
import edu.ustb.sei.mde.nmc.compare.EObjectIndex.Side;
/*HashIndex
 *1.compute EObeject HashValue
 *2.return the nearest top-k Value
*/
public class ByHashIndex implements EObjectIndex{
	/**
	 * hashIndexex recording EObject HashValue
	 */
	private Map<EObject, BigInteger> hashIndexes;
	private Map<String, EObject> invertedIndexes;
	/**
	 * the left objects still present in the index.
	 */
	private Set<EObject> lefts;

	/**
	 * the right objects still present in the index.
	 */
	private Set<EObject> rights;

	/**
	 * the origin objects still present in the index.
	 */
	private Set<EObject> origins;
	
	private MatchComputationByHash matchComputationByHash;
	/*
	 * HashKey Generate
	 * get all references and attribute
	 */
	//public for testing , later change this
	public void HashKey(EObject obj) {
		
		obj.eAllContents().forEachRemaining(e ->{
			//System.out.println("-------------------------------");
			Map<String, Integer> wordCount = new HashMap<String, Integer>();
			EClass eClass = e.eClass();
			//System.out.println(eClass.toString());
			eClass.getEAllAttributes().forEach(a ->{
				Object eGet = e.eGet(a);
				if(eGet != null && eGet.toString()!="null" && eGet.toString()!="false") {
					//System.out.println("a:" + " " + a.getName() + " " + eGet);
					//String str = a.getName()+eGet;
					wordCount.put(eGet.toString(),wordCount.getOrDefault(a.getName(),0)+1);
					//System.out.println(str);
				}
			});
			eClass.getEAllReferences().forEach(r ->{
				Object eGet = e.eGet(r);
				if(eGet != null && eGet.toString().compareTo("[]") != 0 ) {
					//System.out.println("r:" + r.getName().equals("name") + "  " + str.substring(0,str.length()-1));
					//System.out.println("r:" + r.getName() + "  " + str.substring(0,str.length()-1));
					//str = r.getName() + str.substring(0,str.length()-1);
					String str = r.getName();
					wordCount.put(str,wordCount.getOrDefault(str,0)+1);
					//System.out.println(str);
				}
			});
			BigInteger hashCode = matchComputationByHash.simHash(wordCount);
			//hashIndexes.put(e, hashCode);
			String addStr = "000000000000000";
			String code = hashCode.toString(2);
			String str = (addStr + code).substring((addStr + code).length()-64,(addStr + code).length());
			invertedIndexes.put(str.substring(0,15), e);
			invertedIndexes.put(str.substring(16,31), e);
			invertedIndexes.put(str.substring(32,47), e);
			invertedIndexes.put(str.substring(48,63), e);
			hashIndexes.put(e, new BigInteger(str,2));
		});
		
	}
	
	public BigInteger getObjHashKey(EObject obj) {
		HashKey(obj);
		return hashIndexes.get(obj);
	}
	
	public ByHashIndex() {
		this.lefts = Sets.newLinkedHashSet();
		this.rights = Sets.newLinkedHashSet();
		this.origins = Sets.newLinkedHashSet();
		this.hashIndexes = Maps.newHashMap();
		this.invertedIndexes = Maps.newHashMap();
		this.matchComputationByHash = new MatchComputationByHash();
	}

	/**
	 * {@inheritDoc}
	 */
	public void remove(EObject obj, Side side) {
		switch (side) {
			case RIGHT:
				rights.remove(obj);
				break;
			case LEFT:
				lefts.remove(obj);
				break;
			case ORIGIN:
				origins.remove(obj);
				break;
			default:
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void index(EObject eObject, Side side) {
		switch (side) {
			case RIGHT:
				rights.add(eObject);
				break;
			case LEFT:
				lefts.add(eObject);
				break;
			case ORIGIN:
				origins.add(eObject);
				break;

			default:
				break;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Iterable<EObject> getValuesStillThere(final Side side) {
		Collection<EObject> result = Collections.emptyList();
		switch (side) {
			case RIGHT:
				result = ImmutableList.copyOf(rights);
				break;
			case LEFT:
				result = ImmutableList.copyOf(lefts);
				break;
			case ORIGIN:
				result = ImmutableList.copyOf(origins);
				break;
			default:
				break;
		}
		return result;
	}

	@Override
	public Set<EObject> getLefts(EObject obj) {
		return lefts;
	}

	@Override
	public Set<EObject> getRights(EObject obj) {
		return rights;
	}

	@Override
	public Set<EObject> getOrigins(EObject obj) {
		return origins;
	}

}
