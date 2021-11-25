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

import edu.ustb.sei.mde.nmc.compare.match.ByHashIndex;

public class TestHashIndex {

	public static void main(String[] args) {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		
		URI baseURI = URI.createFileURI("C:\\Users\\Administrator\\Documents\\code\\eclipse dsl\\src\\test_ecore\\person.ecore");
		URI branch1URI = URI
				.createFileURI("C:\\Users\\Administrator\\Documents\\code\\eclipse dsl\\src\\test_ecore\\person1.ecore");
		URI branch2URI = URI
				.createFileURI("C:\\Users\\Administrator\\Documents\\code\\eclipse dsl\\src\\test_ecore\\person2.ecore");
		
		List<URI> uriList = new ArrayList<>();
		uriList.add(baseURI);
		uriList.add(branch1URI);
		uriList.add(branch2URI);


		Resource resource = resourceSet.getResource(uriList.get(0), true);
		EObject root  = resource.getContents().get(0);
		ByHashIndex byHashIndex = new ByHashIndex();
		byHashIndex.HashKey(root);
		
	}

}
