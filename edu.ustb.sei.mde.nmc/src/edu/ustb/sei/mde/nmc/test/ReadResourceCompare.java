package edu.ustb.sei.mde.nmc.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import edu.ustb.sei.mde.nmc.compare.Comparison;
import edu.ustb.sei.mde.nmc.compare.IComparisonScope;
import edu.ustb.sei.mde.nmc.compare.IMatchEngine;
import edu.ustb.sei.mde.nmc.compare.match.UseIdentifiers;
import edu.ustb.sei.mde.nmc.compare.start.DefaultComparisonScope;
import edu.ustb.sei.mde.nmc.compare.start.EMFCompare;
import edu.ustb.sei.mde.nmc.compare.start.MatchEngineFactoryImpl;
import edu.ustb.sei.mde.nmc.compare.start.MatchEngineFactoryRegistryImpl;

public class ReadResourceCompare {

	public static void main(String[] args) {

		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		
		URI baseURI = URI.createFileURI("E:\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\person.ecore");
		URI branch1URI = URI
				.createFileURI("E:\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\person1.ecore");
		URI branch2URI = URI
				.createFileURI("E:\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\person2.ecore");
		
		List<URI> uriList = new ArrayList<>();
		uriList.add(baseURI);
		uriList.add(branch1URI);
		uriList.add(branch2URI);

		List<Resource> resourceList = new ArrayList<>();
		Map<Resource, Integer> resourceMap = new HashMap<>();
		for(int i=0; i<uriList.size(); i++) {
			Resource resource = resourceSet.getResource(uriList.get(i), true);
			resourceList.add(resource);
			resourceMap.put(resource, i);	// 为了方便记录新加元素属于哪个分支模型
		}
		
		// tmp
		Resource baseResource = resourceList.get(0);
		baseResource.getAllContents().forEachRemaining(e -> {
			System.out.println("\n\n\ne: " + e + "\n");
			
			EClass eClass = e.eClass();
			
			eClass.getEAllAttributes().forEach(a -> {
				System.out.println("a: " + a);
				if(a.isMany() == false) {
					System.out.println("单值：" + e.eGet(a));
				} else {
					List<Object> list =  (List<Object>) e.eGet(a);
					System.out.println("多值：");
					list.forEach(l -> {
						System.out.println(l);
					});
				}
			});
			
			System.out.println("-----------------------------");
			eClass.getEAllReferences().forEach(r -> {
				System.out.println("r: " + r);
				
				if(r.isMany() == false) {
					System.out.println("单值：" + e.eGet(r));
				} else {
					List<EObject> list = (List<EObject>)e.eGet(r);
					System.out.println("多值：");
					list.forEach(l -> {
						System.out.println(l);
					});
				}
			});
			
			System.out.println("line 88");
			
		});
		
		
		
		
		
		
		// never use identifiers
		IMatchEngine.Factory.Registry registry = MatchEngineFactoryRegistryImpl.createStandaloneInstance();
		final MatchEngineFactoryImpl matchEngineFactory = new MatchEngineFactoryImpl(UseIdentifiers.ONLY);
		matchEngineFactory.setRanking(20);
		registry.add(matchEngineFactory);
					
		Resource originResource = baseResource;
		Resource leftResource = resourceList.get(1);
		Resource rightResource = resourceList.get(2);
		IComparisonScope scope = null;

//		scope = new DefaultComparisonScope(leftResource, rightResource, originResource);
		scope = new DefaultComparisonScope(originResource, leftResource, null);
		Comparison comparison = EMFCompare.builder().setMatchEngineFactoryRegistry(registry).build().compare(scope);

		comparison.getMatches().forEach(m -> {
			System.out.println("left   :" + m.getLeft());
			System.out.println("right  :" + m.getRight());
			System.out.println("origin :" + m.getOrigin());
			System.out.println("________________________________________");
//			m.getAllSubmatches().forEach(sub -> {
//				System.out.println("left   :" + sub.getLeft());
//				System.out.println("right  :" + sub.getRight());
//				System.out.println("origin :" + sub.getOrigin());
//				System.out.println("----------------------------------------");
//			});
		});
		System.out.println("");
				
	}
}
