package edu.ustb.sei.mde.nmc.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import edu.ustb.sei.mde.nmc.compare.Comparison;
import edu.ustb.sei.mde.nmc.compare.IComparisonScope;
import edu.ustb.sei.mde.nmc.compare.IMatchEngine;
import edu.ustb.sei.mde.nmc.compare.Match;
import edu.ustb.sei.mde.nmc.compare.internal.ComparisonSpec;
import edu.ustb.sei.mde.nmc.compare.internal.MatchSpec;
import edu.ustb.sei.mde.nmc.compare.match.UseIdentifiers;
import edu.ustb.sei.mde.nmc.compare.start.DefaultComparisonScope;
import edu.ustb.sei.mde.nmc.compare.start.EMFCompare;
import edu.ustb.sei.mde.nmc.compare.start.MatchEngineFactoryImpl;
import edu.ustb.sei.mde.nmc.compare.start.MatchEngineFactoryRegistryImpl;
import edu.ustb.sei.mde.nmc.nway.MaximalCliquesWithPivot;
import edu.ustb.sei.mde.nmc.nway.RefEdge;
import edu.ustb.sei.mde.nmc.nway.RefEdgeMulti;
import edu.ustb.sei.mde.nmc.nway.ValEdge;
import edu.ustb.sei.mde.nmc.nway.ValEdgeMulti;

public class TestNWay {

	public static void main(String[] args) {

		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

		URI baseURI = URI.createFileURI("D:\\eclipse-dsl-workspace\\nmc-main2\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\person.ecore");
		URI branch1URI = URI
				.createFileURI("D:\\eclipse-dsl-workspace\\nmc-main2\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\person1.ecore");
		URI branch2URI = URI
				.createFileURI("D:\\eclipse-dsl-workspace\\nmc-main2\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\person2.ecore");

		List<URI> uriList = new ArrayList<>();
		uriList.add(baseURI);
		uriList.add(branch1URI);
		uriList.add(branch2URI);
		//uriList.add(branch3URI);

		int size = uriList.size();
		List<Resource> resourceList = new ArrayList<>(size);
		Map<Resource, Integer> resourceMap = new HashMap<>();
		for (int i = 0; i < size; i++) {
			Resource resource = resourceSet.getResource(uriList.get(i), true);
			resourceList.add(resource);
			resourceMap.put(resource, i); // 为了方便记录新加元素属于哪个分支模型
		}

		// never use identifiers
		IMatchEngine.Factory.Registry registry = MatchEngineFactoryRegistryImpl.createStandaloneInstance();
		final MatchEngineFactoryImpl matchEngineFactory = new MatchEngineFactoryImpl(UseIdentifiers.ONLY);
		matchEngineFactory.setRanking(20);
		registry.add(matchEngineFactory);

		// 只使用一个EMFCompare
		EMFCompare build = EMFCompare.builder().setMatchEngineFactoryRegistry(registry).build();

		Comparison comparison = null;
		IComparisonScope scope = null;
		Resource baseResource = resourceList.get(0);
		Resource branchResource = null;

		// 将原始模型与各个分支模型进行二向匹配
		// 匹配上同一个原始元素的，划分到相同的匹配组
		// 还要记录新加的元素
		// 还要记录预匹配信息
		Map<EObject, List<EObject>> nodeMatchGroupMap = new HashMap<>();
		Map<Integer, List<EObject>> addMap = new HashMap<>();
		// 还要记录非新加的匹配组
		Map<EObject, List<EObject>> nodesIndexMap = new HashMap<>();

		for (int i = 1; i < size; i++) { // resourceList下标为0时，对应baseResource
			branchResource = resourceList.get(i);
			scope = new DefaultComparisonScope(baseResource, branchResource, null);
			long start = System.currentTimeMillis();
			comparison = build.compare(scope);
			long end = System.currentTimeMillis();
			System.out.println("MATCH TIME: " + (end - start) + " ms.\n");

			for (Match match : comparison.getMatches()) {
				EObject baseEObject = match.getLeft();
				EObject branchEObject = match.getRight();
				if (baseEObject != null) {
					List<EObject> list = nodeMatchGroupMap.get(baseEObject);
					if (list == null) {
						list = new ArrayList<>();
						nodeMatchGroupMap.put(baseEObject, list);
						// 注意baseEObject没在list中（暂时不影响）
						nodesIndexMap.put(baseEObject, list);
					}
					list.add(branchEObject);	// 例如<eb, {e1, null, e3, ..., en}>

					// need to improve?
					if (branchEObject != null) {
						nodesIndexMap.put(branchEObject, list);
					}

				} else {
					List<EObject> addList = addMap.get(i);
					if (addList == null) {
						addList = new ArrayList<>();
						addMap.put(i, addList); // 将相同分支模型的所有新加元素放到一起
					}
					addList.add(branchEObject);
				}
			}

		}
		
		
		
		
		// tmp
		System.out.println("\n\n\n----------------------matchGroupMap: ");
		nodeMatchGroupMap.forEach((key, value) -> {
			System.out.println("key: " + key);
			value.forEach(e -> {
				System.out.println(e);
			});
			System.out.println();
		});
		System.out.println("\n\n\n");
		
		
		
		

		// 预匹配信息：将分支i，分支j作为键
		MultiKeyMap<Integer, List<Match>> preMatchMap = new MultiKeyMap<>();
		nodeMatchGroupMap.values().forEach(value -> {
			for (int i = 1; i < size - 1; i++) {
				EObject leftEObject = value.get(i - 1); // 注意：value下标为0时，对应的是分支1
				for (int j = i + 1; j < size; j++) {
					EObject rightEObject = value.get(j - 1);
					if (leftEObject != null || rightEObject != null) {
						List<Match> list = preMatchMap.get(i, j);
						if (list == null) {
							list = new ArrayList<>();
							preMatchMap.put(i, j, list);
						}
						Match match = new MatchSpec();
						match.setLeft(leftEObject);
						match.setRight(rightEObject);
						list.add(match);
					}
				}
			}
		});

		// 对于新加元素，两两比较分支模型
		Set<EObject> vertices = new HashSet<>();
		Set<Match> edges = new HashSet<>();
		// 还要记录编辑距离
		MultiKeyMap<EObject, Double> distanceMap = new MultiKeyMap<>();

		for (int i = 1; i < size - 1; i++) {
			List<EObject> leftEObjects = addMap.get(i);
			if (leftEObjects != null) {
				vertices.addAll(leftEObjects);
				for (int j = i + 1; j < size; j++) {
					List<EObject> rightEObjects = addMap.get(j);
					if (rightEObjects != null) {
						vertices.addAll(rightEObjects);
						// 加入预匹配信息
						Comparison comparisonADD = new ComparisonSpec();
						List<Match> preMatchList = preMatchMap.get(i, j);
						comparisonADD.getMatches().addAll(preMatchList);

						long start = System.currentTimeMillis();
						build.compareADD(comparisonADD, leftEObjects, rightEObjects, distanceMap);
						long end = System.currentTimeMillis();
						System.out.println("\n\n\nADD MATCH TIME: " + (end - start) + " ms.");

						// 过滤出新加元素的匹配
						int num = preMatchList.size();
						List<Match> collect = comparisonADD.getMatches().stream().skip(num)
								.filter(m -> m.getLeft() != null && m.getRight() != null).collect(Collectors.toList());
						edges.addAll(collect);
					}
				}
			}
		}

		// tmp
		System.out.println("\n\n\n----------------------edges: ");
		edges.forEach(e -> {
			System.out.println(e.getLeft());
			System.out.println(e.getRight());
			System.out.println();
		});
		System.out.println("\n\n\n");

		// 成团、排序、选择
		List<List<EObject>> nodeMatchGroupList = new ArrayList<>();

		MaximalCliquesWithPivot ff = new MaximalCliquesWithPivot();
		ff.initGraph(vertices, edges);
		List<List<EObject>> maximalCliques = new ArrayList<>();
		if (vertices.size() > 0 && edges.size() > 0) {
			ff.Bron_KerboschPivotExecute(maximalCliques);
		}

		// tmp
		System.out.println("\n\n\n----------------------maximalCliques: ");
		maximalCliques.forEach(m -> {
			m.forEach(e -> {
				System.out.println("e: " + e);
			});
			System.out.println();
		});
		System.out.println("\n\n\n");

		while (maximalCliques.size() > 0) {

			Map<List<EObject>, Double> map = new HashMap<>();

			for (List<EObject> clique : maximalCliques) {
				// 筛选出container为空，或者container位于同一个匹配组的
				if (checkValid(clique, nodesIndexMap)) {
					int sum = 0;
					for (int i = 0; i < clique.size() - 1; i++) {
						EObject eObjectI = clique.get(i);
						for (int j = i + 1; j < clique.size(); j++) {
							EObject eObjectJ = clique.get(j);
							Double distance = distanceMap.get(eObjectI, eObjectJ);
							if (distance == null) {
								distance = distanceMap.get(eObjectJ, eObjectI);
							}
							sum += distance;
						}
					}

					if (sum == 0) {
						sum = 1; // sum不为0话，大于等于5；为了之后的除法
					}
					int cSize = clique.size();
					if (clique.size() == 1) {
						cSize = 0;	// 当团只有一个元素时，让cSize为0，这样选择它的优先级最低
					}

					map.put(clique, Double.valueOf(cSize) / sum);
				}
			}

			// tmp
			map.forEach((key, value) -> {
				key.forEach(e -> {
					System.out.println(e);
				});

				System.out.println(value);
				System.out.println();
			});

			// 按照value进行排序（降序），取第一个作为匹配组
			Optional<Entry<List<EObject>, Double>> findFirst = map.entrySet().stream()
					.sorted(Map.Entry.<List<EObject>, Double>comparingByValue().reversed()).findFirst();
			// 暂时没将其它位置设置为null，对应分支位置设置为EObject
			// 应该还是需要记录，因为出现新加-新加冲突时，需要精确定位？
			List<EObject> matchGroup = findFirst.get().getKey();	
			nodeMatchGroupList.add(matchGroup);						
			maximalCliques.remove(matchGroup);

			// 更新nodesIndexMap，need to improve?
			matchGroup.forEach(e -> {
				nodesIndexMap.put(e, matchGroup);
			});

			// tmp
			System.out.println("**************挑选的团***************");
			matchGroup.forEach(e -> {
				System.out.println(e);
			});
			System.out.println("\n\n\n\n\n");

			// 若其它团包含匹配组中的EObject，则clique.remove(EObejct)
			// 当clique不包含任何对象，则maximalCliques.remove(clique)
			for (EObject eObject : matchGroup) {
				Iterator<List<EObject>> iterator = maximalCliques.iterator();
				while (iterator.hasNext()) {
					List<EObject> clique = iterator.next();
					if (clique.contains(eObject)) {
						clique.remove(eObject);
						if (clique.size() == 0) {
							iterator.remove();
						}
					}
				}
			}
		}

		
		
		
		
		// tmp
		System.out.println("\n\n\n----------------------matchGroupList: ");
		nodeMatchGroupList.forEach(list -> {
			list.forEach(e -> {
				System.out.println(e);
			});
			System.out.println();
		});
		System.out.println("\n\n\n");
		
		
		
		
		// PENDING: 边的ID　-> 边的匹配组
		Map<ValEdge, List<ValEdge>> valEdgeMatchGroupMap = new HashMap<>();
		Map<ValEdgeMulti, List<ValEdgeMulti>> valEdgeMultiMatchGroupMap = new HashMap<>();		
		
		Map<RefEdge, List<RefEdge>> refEdgeMatchGroupMap = new HashMap<>();
		Map<RefEdgeMulti, List<RefEdgeMulti>> refEdgeMultiMatchGroupMap = new HashMap<>();
				
		nodeMatchGroupMap.forEach((baseEObject, list) -> {
			
			System.out.println("\n\n***************************baseEObject***************************");
			System.out.println("baseEObject: " + baseEObject);
			List<EObject> t = nodesIndexMap.get(baseEObject);	// e.sourceIndex = t; 
			System.out.println("t: " + t +"\n\n");
			
			EClass eClass = baseEObject.eClass();
			
			for(EAttribute a : eClass.getEAllAttributes()) {
				if(a.isMany() == false) {	// 单值属性
					System.out.println("单值a: " + a);
					Object eGet = baseEObject.eGet(a);	
					System.out.println("eGet: " + eGet);
					
					ValEdge valEdge = new ValEdge(a, t, eGet);	// eGet maybe null
					List<ValEdge> create = new ArrayList<>(size-1);
					valEdgeMatchGroupMap.put(valEdge, create);
					
				} else {	// 多值属性
					System.out.println("多值a: " + a);
					List<Object> targets = (List<Object>) baseEObject.eGet(a);	// targets maybe null
					
					ValEdgeMulti valEdgeMulti = new ValEdgeMulti(a, t, targets);
					List<ValEdgeMulti> create = new ArrayList<>(size-1);
					valEdgeMultiMatchGroupMap.put(valEdgeMulti, create);
					
				}
			}
			
			System.out.println("-----------------------------------------------------------------------");
			for(EReference r : eClass.getEAllReferences()) {
				if(r.isMany()==false) {	// 单值引用
					System.out.println("单值r: " + r);
					Object eGet = baseEObject.eGet(r);	
					List<EObject> g = nodesIndexMap.get(eGet);	
					System.out.println("g: " + g);
					
					RefEdge refEdge = new RefEdge(r, t, g);	// g maybe null
					List<RefEdge> create = new ArrayList<>(size-1);
					refEdgeMatchGroupMap.put(refEdge, create);					
					
				} else {	// 多值引用
					System.out.println("多值r: " + r);
					List<EObject> targets = (List<EObject>) baseEObject.eGet(r);
					
					List<List<EObject>> targetsIndex = new ArrayList<>();
					targets.forEach(eObj -> {
						targetsIndex.add(nodesIndexMap.get(eObj));
					});
					
					RefEdgeMulti refEdgeMulti = new RefEdgeMulti(r, t, targetsIndex);	// targetsIndex maybe null
					List<RefEdgeMulti> create = new ArrayList<>(size-1);
					refEdgeMultiMatchGroupMap.put(refEdgeMulti, create);
					
				}
			}
						
			
			for(EObject branchEObject : list) {
				System.out.println("\n\n***************************遍历list***************************");
				if(branchEObject != null) {
					// 这里没必要nodesIndexMap.get(branchEObject)，因为在同一个matchGroup，肯定为t
					System.out.println("branchEObject: " + branchEObject);
					
					EClass eClass2 = branchEObject.eClass();
					
					for(EAttribute a2: eClass2.getEAllAttributes()) {
						if(a2.isMany() == false) {	// 单值属性
							System.out.println("单值a2: " + a2);
							Object eGet2 = branchEObject.eGet(a2);
							System.out.println("eGet2: " + eGet2);
							
							ValEdge valEdge2 = new ValEdge(a2, t, eGet2);
							valEdgeMatchGroupMap.get(valEdge2).add(valEdge2);
							
						} else {	// 多值属性
							System.out.println("多值a2: " + a2);
							List<Object> targets2 = (List<Object>) baseEObject.eGet(a2);
							
							ValEdgeMulti valEdgeMulti2 = new ValEdgeMulti(a2, t, targets2);
							valEdgeMultiMatchGroupMap.get(valEdgeMulti2).add(valEdgeMulti2);
							
						}
					}
					
					System.out.println("-----------------------------------------------------------------------");
					for(EReference r2: eClass2.getEAllReferences()) {
						if(r2.isMany() == false) {	// 单值引用
							System.out.println("单值r2: " + r2);
							Object eGet2 = branchEObject.eGet(r2);
							List<EObject> g2 = nodesIndexMap.get(eGet2);	
							System.out.println("g2: " + g2);
							
							RefEdge refEdge2 = new RefEdge(r2, t, g2);
							refEdgeMatchGroupMap.get(refEdge2).add(refEdge2);
											
						} else {	// 多值引用
							System.out.println("多值r2: " + r2);
							List<EObject> targets2 = (List<EObject>) baseEObject.eGet(r2);
							
							List<List<EObject>> targetsIndex2 = new ArrayList<>();
							targets2.forEach(eObj2 -> {
								targetsIndex2.add(nodesIndexMap.get(eObj2));
							});
							
							RefEdgeMulti refEdgeMulti2 = new RefEdgeMulti(r2, t, targetsIndex2);	
							refEdgeMultiMatchGroupMap.get(refEdgeMulti2).add(refEdgeMulti2);
							
							
						}
					}
				}
			}
			
			
			// tmp
			System.out.println("\n\n\n=========================== valEdgeMatchGroupMap ==============================");
			valEdgeMatchGroupMap.forEach((key, value) -> {
				System.out.println("key.getType():        " + key.getType());
				System.out.println("key.getSourceIndex(): " + key.getSourceIndex());
				System.out.println("key.getTarget():      " + key.getTarget());
				
				value.forEach(v -> {
					System.out.println("v.getType():        " + v.getType());
					System.out.println("v.getSourceIndex(): " + v.getSourceIndex());
					System.out.println("v.getTarget():      " + v.getTarget());
				});
				System.out.println("-----------------------------------------------------");
			});
			System.out.println("\n\n\n");
			
			
			System.out.println("\n\n\n=========================== valEdgeMultiMatchGroupMap ==============================");
			valEdgeMultiMatchGroupMap.forEach((key, value) -> {
				System.out.println("key.getType():         " + key.getType());
				System.out.println("key.getSourceIndex():  " + key.getSourceIndex());
				System.out.println("key.getTargets():      " + key.getTargets().size() + " "+ key.getTargets());
				
				value.forEach(v -> {
					System.out.println("v.getType():         " + v.getType());
					System.out.println("v.getSourceIndex():  " + v.getSourceIndex());
					System.out.println("v.getTargets():      " + v.getTargets().size()+ " " + v.getTargets());
				});
				System.out.println("-----------------------------------------------------");
			});
			System.out.println("\n\n\n");

			
			
			System.out.println("\n\n\n=========================== refEdgeMatchGroupMap ==============================");
			refEdgeMatchGroupMap.forEach((key, value) ->{
				System.out.println("key.getType():        " + key.getType());
				System.out.println("key.getSourceIndex(): " + key.getSourceIndex());
				System.out.println("key.getTargetIndex(): " + key.getTargetIndex());
				
				value.forEach(v -> {
					System.out.println("v.getType():        " + v.getType());
					System.out.println("v.getSourceIndex(): " + v.getSourceIndex());
					System.out.println("v.getTargetIndex(): " + v.getTargetIndex());
				});
				System.out.println("-----------------------------------------------------");
			});
			System.out.println("\n\n\n");
			
			
			System.out.println("\n\n\n=========================== refEdgeMultiMatchGroupMap ==============================");
			refEdgeMultiMatchGroupMap.forEach((key, value) ->{
				System.out.println("key.getType():         " + key.getType());
				System.out.println("key.getSourceIndex():  " + key.getSourceIndex());
				System.out.println("key.getTargetsIndex(): " + key.getTargetsIndex().size() + " " + key.getTargetsIndex());
				
				value.forEach(v -> {
					System.out.println("v.getType():         " + v.getType());
					System.out.println("v.getSourceIndex():  " + v.getSourceIndex());
					System.out.println("v.getTargetsIndex(): " + v.getTargetsIndex().size() + " " + v.getTargetsIndex());
				});
				System.out.println("-----------------------------------------------------");
			});
			System.out.println("\n\n\n");
			
			
			System.out.println("line 462");
			
			
		});
		
		
		
		
		
		
		
//		matchGroupList.forEach(list -> {
//			list.forEach(addEObject -> {
//				
//			});
//		});

	}

	/**
	 * 选择团时，先选container为空，或者container位于同一个匹配组的。
	 */
	private static boolean checkValid(List<EObject> clique, Map<EObject, List<EObject>> nodesIndexMap) {
		List<EObject> origin = nodesIndexMap.get(clique.get(0).eContainer());
		for (EObject e : clique) {
			EObject eContainer = e.eContainer();
			List<EObject> current = nodesIndexMap.get(eContainer);
			if (eContainer == null || (current != null && origin != null && current == origin)) {
				current = origin;
			} else {
				return false;
			}

		}
		return true;
	}

}
