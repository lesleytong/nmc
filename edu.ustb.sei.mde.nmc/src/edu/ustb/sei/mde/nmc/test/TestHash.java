package edu.ustb.sei.mde.nmc.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import edu.ustb.sei.mde.nmc.compare.Comparison;
import edu.ustb.sei.mde.nmc.compare.IComparisonScope;
import edu.ustb.sei.mde.nmc.compare.IMatchEngine;
import edu.ustb.sei.mde.nmc.compare.match.ByHashIndex;
import edu.ustb.sei.mde.nmc.compare.match.UseIdentifiers;
import edu.ustb.sei.mde.nmc.compare.start.DefaultComparisonScope;
import edu.ustb.sei.mde.nmc.compare.start.EMFCompare;
import edu.ustb.sei.mde.nmc.compare.start.MatchEngineFactoryImpl;
import edu.ustb.sei.mde.nmc.compare.start.MatchEngineFactoryRegistryImpl;

public class TestHash {

	public static void main(String[] args) {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		
		resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		
		URI baseURI = URI.createFileURI("D:\\src\\test\\person.ecore");
		URI branch1URI = URI
			.createFileURI("D:\\src\\test\\person1.ecore");
		URI branch2URI = URI
			.createFileURI("D:\\src\\test\\person2.ecore");
		URI branch3URI = URI
				.createFileURI("D:\\src\\test\\person3.ecore");
		URI branch4URI = URI
				.createFileURI("D:\\src\\test\\person4.ecore");
		URI branch5URI = URI
				.createFileURI("D:\\src\\test\\person5.ecore");
		
		List<URI> uriList = new ArrayList<>();
		uriList.add(baseURI);
		uriList.add(branch1URI);
		uriList.add(branch2URI);
		uriList.add(branch3URI);
		uriList.add(branch4URI);
		uriList.add(branch5URI);
		
		List<Resource> resourceList = new ArrayList<>();
		Map<Resource, Integer> resourceMap = new HashMap<>();
		for(int i=0; i<uriList.size(); i++) {
		Resource resource = resourceSet.getResource(uriList.get(i), true);
		resourceList.add(resource);
		resourceMap.put(resource, i);	// 为了方便记录新加元素属于哪个分支模型
		}
		
		
		// never use identifiers
		IMatchEngine.Factory.Registry registry = MatchEngineFactoryRegistryImpl.createStandaloneInstance();
		final MatchEngineFactoryImpl matchEngineFactory = new MatchEngineFactoryImpl(UseIdentifiers.HASH);
		matchEngineFactory.setRanking(20);
		registry.add(matchEngineFactory);
				
		Resource person0 = resourceList.get(0);
		Resource person1 = resourceList.get(1);
		Resource person2 = resourceList.get(2);
		Resource person3 = resourceList.get(3);
		Resource person4 = resourceList.get(4);
		Resource person5 = resourceList.get(5);
		IComparisonScope scope = null;
		
		//scope = new DefaultComparisonScope(originResource, leftResource, null);
		scope = new DefaultComparisonScope(person4, person5, null);
		Comparison comparison = EMFCompare.builder().setMatchEngineFactoryRegistry(registry).build().compare(scope);
		
		comparison.getMatches().forEach(m -> {
		System.out.println("left   :" + m.getLeft());
		System.out.println("right  :" + m.getRight());
		System.out.println("origin :" + m.getOrigin());
		System.out.println("________________________________________");
		});
		System.out.println("");
		
	}

}
