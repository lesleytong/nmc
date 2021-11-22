package edu.ustb.sei.mde.nmc.test;

import java.util.ArrayList;
import java.util.Collections;
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
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
import edu.ustb.sei.mde.nmc.nway.Conflict;
import edu.ustb.sei.mde.nmc.nway.ConflictKind;
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

		URI baseURI = URI.createFileURI("E:\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\bank.ecore");
		URI branch1URI = URI
				.createFileURI("E:\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\bank1.ecore");
		URI branch2URI = URI
				.createFileURI("E:\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\bank2.ecore");
		URI branch3URI = URI
				.createFileURI("E:\\nmc\\edu.ustb.sei.mde.nmc\\src\\edu\\ustb\\sei\\mde\\nmc\\ecore\\bank3.ecore");

		List<URI> uriList = new ArrayList<>();
		uriList.add(baseURI);
		uriList.add(branch1URI);
		uriList.add(branch2URI);
		uriList.add(branch3URI);

		int size = uriList.size();
		List<Resource> resourceList = new ArrayList<>(size);
		Map<Resource, Integer> resourceMap = new HashMap<>();
		for (int i = 0; i < size; i++) {
			Resource resource = resourceSet.getResource(uriList.get(i), true);
			resourceList.add(resource);
			resourceMap.put(resource, i-1); // 为了方便记录新加元素属于哪个分支模型
		}

		// never use identifiers
		IMatchEngine.Factory.Registry registry = MatchEngineFactoryRegistryImpl.createStandaloneInstance();
		final MatchEngineFactoryImpl matchEngineFactory = new MatchEngineFactoryImpl(UseIdentifiers.NEVER);
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
		// 还要记录非新加的匹配组，为了checkValid
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
						sum = 1; // sum不为0话，大于等于5；当sum为0时，让其为1，为了之后的除法
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
			List<EObject> matchGroup = findFirst.get().getKey();	
			maximalCliques.remove(matchGroup);
			
			List<EObject> nodeMatchGroup = new ArrayList<>(Collections.nCopies(size-1, null));
			matchGroup.forEach(e -> {
				Integer index =  resourceMap.get(e.eResource());
				nodeMatchGroup.set(index, e);	// 其它位置为null
				nodeMatchGroupList.add(nodeMatchGroup);				
				// 更新nodesIndexMap，need to improve?
				nodesIndexMap.put(e, nodeMatchGroup);
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
			List<EObject> sourceIndex = nodesIndexMap.get(baseEObject);	// e.sourceIndex; 
			
			EClass eClass = baseEObject.eClass();
			
			for(EAttribute a : eClass.getEAllAttributes()) {
				if(a.isMany() == false) {	// 单值属性
					Object target = baseEObject.eGet(a);	
					ValEdge valEdge = new ValEdge(a, sourceIndex, target);	// target maybe unset
					List<ValEdge> create = new ArrayList<>(Collections.nCopies(size-1, null));
					valEdgeMatchGroupMap.put(valEdge, create);
					
				} else {	// 多值属性
					List<Object> targets = (List<Object>) baseEObject.eGet(a);	// targets maybe unset
					ValEdgeMulti valEdgeMulti = new ValEdgeMulti(a, sourceIndex, targets);
					List<ValEdgeMulti> create = new ArrayList<>(Collections.nCopies(size-1, null));
					valEdgeMultiMatchGroupMap.put(valEdgeMulti, create);
					
				}
			}
			
			System.out.println("-----------------------------------------------------------------------");
			for(EReference r : eClass.getEAllReferences()) {
				if(r.isMany()==false) {	// 单值引用
					Object value = baseEObject.eGet(r);	
					List<EObject> targetIndex = nodesIndexMap.get(value);	
					RefEdge refEdge = new RefEdge(r, sourceIndex, targetIndex);	// targetIndex maybe unset
					List<RefEdge> create = new ArrayList<>(Collections.nCopies(size-1, null));
					refEdgeMatchGroupMap.put(refEdge, create);					
					
				} else {	// 多值引用
					List<EObject> values = (List<EObject>) baseEObject.eGet(r);
					List<List<EObject>> targetsIndex = new ArrayList<>();
					values.forEach(eObj -> {
						targetsIndex.add(nodesIndexMap.get(eObj));
					});
					
					RefEdgeMulti refEdgeMulti = new RefEdgeMulti(r, sourceIndex, targetsIndex);	// targetsIndex may unset
					List<RefEdgeMulti> create = new ArrayList<>(Collections.nCopies(size-1, null));
					refEdgeMultiMatchGroupMap.put(refEdgeMulti, create);
					
				}
			}
						
			
			for(int i=0; i<size-1; i++) {
				EObject branchEObject = list.get(i);
				System.out.println("\n\n***************************遍历list***************************");
				if(branchEObject != null) {
					// 因为在同一个matchGroup，sourceIndex相同
					System.out.println("branchEObject: " + branchEObject);
					
					EClass eClass2 = branchEObject.eClass();	// PENDING: may not the same as eClass.
					
					for(EAttribute a2: eClass2.getEAllAttributes()) {
						if(a2.isMany() == false) {	// 单值属性
							Object target2 = branchEObject.eGet(a2);							
							ValEdge valEdge2 = new ValEdge(a2, sourceIndex, target2);
							valEdgeMatchGroupMap.get(valEdge2).set(i, valEdge2);	// PENDING: what if get(valEdge2) is null?
													
						} else {	// 多值属性
							List<Object> targets2 = (List<Object>) branchEObject.eGet(a2);						
							ValEdgeMulti valEdgeMulti2 = new ValEdgeMulti(a2, sourceIndex, targets2);
							valEdgeMultiMatchGroupMap.get(valEdgeMulti2).set(i, valEdgeMulti2);
							
						}
					}
					
					System.out.println("-----------------------------------------------------------------------");
					for(EReference r2: eClass2.getEAllReferences()) {
						if(r2.isMany() == false) {	// 单值引用
							Object value2 = branchEObject.eGet(r2);
							List<EObject> targetIndex2 = nodesIndexMap.get(value2);	
							
							RefEdge refEdge2 = new RefEdge(r2, sourceIndex, targetIndex2);
							refEdgeMatchGroupMap.get(refEdge2).set(i, refEdge2);
											
						} else {	// 多值引用
							List<EObject> values2 = (List<EObject>) branchEObject.eGet(r2);
							
							List<List<EObject>> targetsIndex2 = new ArrayList<>();
							values2.forEach(eObj2 -> {
								targetsIndex2.add(nodesIndexMap.get(eObj2));
							});
							
							RefEdgeMulti refEdgeMulti2 = new RefEdgeMulti(r2, sourceIndex, targetsIndex2);	
							refEdgeMultiMatchGroupMap.get(refEdgeMulti2).set(i, refEdgeMulti2);
														
						}
					}
				} 
			}
		});
		
		// tmp
//		System.out.println("\n\n\n=========================== valEdgeMatchGroupMap ==============================");
//		valEdgeMatchGroupMap.forEach((key, value) -> {
//			System.out.println("key.getType():        " + key.getType());
//			System.out.println("key.getSourceIndex(): " + key.getSourceIndex());
//			System.out.println("key.getTarget():      " + key.getTarget());
//			
//			value.forEach(v -> {
//				if(v != null) {
//					System.out.println("v.getType():        " + v.getType());
//					System.out.println("v.getSourceIndex(): " + v.getSourceIndex());
//					System.out.println("v.getTarget():      " + v.getTarget());
//				} else {
//					System.out.println(v);
//				}
//			});
//			System.out.println("-----------------------------------------------------");
//		});
//		System.out.println("\n\n\n");
//		
//		
//		System.out.println("\n\n\n=========================== valEdgeMultiMatchGroupMap ==============================");
//		valEdgeMultiMatchGroupMap.forEach((key, value) -> {
//			System.out.println("key.getType():         " + key.getType());
//			System.out.println("key.getSourceIndex():  " + key.getSourceIndex());
//			System.out.println("key.getTargets():      " + key.getTargets().size() + " "+ key.getTargets());
//			
//			value.forEach(v -> {
//				if(v != null) {
//					System.out.println("v.getType():         " + v.getType());
//					System.out.println("v.getSourceIndex():  " + v.getSourceIndex());
//					System.out.println("v.getTargets():      " + v.getTargets().size()+ " " + v.getTargets());
//				} else {
//					System.out.println(v);
//				}
//			});
//			System.out.println("-----------------------------------------------------");
//		});
//		System.out.println("\n\n\n");
//
//		
//		
//		System.out.println("\n\n\n=========================== refEdgeMatchGroupMap ==============================");
//		refEdgeMatchGroupMap.forEach((key, value) ->{
//			System.out.println("key.getType():        " + key.getType());
//			System.out.println("key.getSourceIndex(): " + key.getSourceIndex());
//			System.out.println("key.getTargetIndex(): " + key.getTargetIndex());
//			
//			value.forEach(v -> {
//				if(v != null) {
//					System.out.println("v.getType():        " + v.getType());
//					System.out.println("v.getSourceIndex(): " + v.getSourceIndex());
//					System.out.println("v.getTargetIndex(): " + v.getTargetIndex());
//				} else {
//					System.out.println(v);
//				}
//			});
//			System.out.println("-----------------------------------------------------");
//		});
//		System.out.println("\n\n\n");
//		
//		
//		System.out.println("\n\n\n=========================== refEdgeMultiMatchGroupMap ==============================");
//		refEdgeMultiMatchGroupMap.forEach((key, value) ->{
//			System.out.println("key.getType():         " + key.getType());
//			System.out.println("key.getSourceIndex():  " + key.getSourceIndex());
//			System.out.println("key.getTargetsIndex(): " + key.getTargetsIndex().size() + " " + key.getTargetsIndex());
//			
//			value.forEach(v -> {
//				if(v != null) {
//					System.out.println("v.getType():         " + v.getType());
//					System.out.println("v.getSourceIndex():  " + v.getSourceIndex());
//					System.out.println("v.getTargetsIndex(): " + v.getTargetsIndex().size() + " " + v.getTargetsIndex());
//				} else {
//					System.out.println(v);
//				}
//			});
//			System.out.println("-----------------------------------------------------");
//		});
//		System.out.println("\n\n\n");
		
		
		
		// 针对新加的点
		System.out.println("\n\n\n***********************针对新加的点**************************");
		Map<ValEdge, List<ValEdge>> valEdgeAddMatchGroupMap = new HashMap<>();
		Map<ValEdgeMulti, List<ValEdgeMulti>> valEdgeMultiAddMatchGroupMap = new HashMap<>();
		
		Map<RefEdge, List<RefEdge>> refEdgeAddMatchGroupMap = new HashMap<>();
		Map<RefEdgeMulti, List<RefEdgeMulti>> refEdgeMultiAddMatchGroupMap = new HashMap<>();
		
		for(List<EObject> list : nodeMatchGroupList) {
			for(int i=0; i<size-1; i++) {
				EObject addEObject = list.get(i);
				if(addEObject != null) {
					System.out.println("addEObject: " + addEObject);
					
					EClass eClass = addEObject.eClass();
					List<EObject> sourceIndex = nodesIndexMap.get(addEObject);
					
					for(EAttribute a : eClass.getEAllAttributes()) {
						if(a.isMany() == false) {	// 单值属性
							Object target = addEObject.eGet(a);
							ValEdge valEdge = new ValEdge(a, sourceIndex, target);
							List<ValEdge> exist = valEdgeAddMatchGroupMap.get(valEdge);
							if(exist == null) {
								List<ValEdge> create = new ArrayList<>(Collections.nCopies(size-1, null));
								create.set(i, valEdge);
								valEdgeAddMatchGroupMap.put(valEdge, create);	// 还是把新加匹配组中的第一个看作base的地位，但之后合并不需要考虑
							}else {
								exist.set(i, valEdge);
							}
							
						} else {	// 多值属性
							List<Object> targets = (List<Object>) addEObject.eGet(a);
							ValEdgeMulti valEdgeMulti = new ValEdgeMulti(a, sourceIndex, targets);
							List<ValEdgeMulti> exist = valEdgeMultiAddMatchGroupMap.get(valEdgeMulti);
							if(exist == null) {
								List<ValEdgeMulti> create = new ArrayList<>(Collections.nCopies(size-1, null));
								create.set(i, valEdgeMulti);
								valEdgeMultiAddMatchGroupMap.put(valEdgeMulti, create);
							} else {
								exist.set(i, valEdgeMulti);
							}
						}
					}
					
					for(EReference r : eClass.getEAllReferences()) {
						if(r.isMany() == false) {	// 单值引用
							Object value = addEObject.eGet(r);
							List<EObject> targetIndex = nodesIndexMap.get(value);
							RefEdge refEdge = new RefEdge(r, sourceIndex, targetIndex);
							List<RefEdge> exist = refEdgeAddMatchGroupMap.get(refEdge);
							if(exist == null) {
								List<RefEdge> create = new ArrayList<>(Collections.nCopies(size-1, null));
								create.set(i, refEdge);
								refEdgeAddMatchGroupMap.put(refEdge, create);
							} else {
								exist.set(i, refEdge);
							}
							
						} else {	// 多值引用
							List<Object> values = (List<Object>) addEObject.eGet(r);
							List<List<EObject>> targetsIndex = new ArrayList<>();
							values.forEach(v -> {
								targetsIndex.add(nodesIndexMap.get(v));
							});
							RefEdgeMulti refEdgeMulti = new RefEdgeMulti(r, sourceIndex, targetsIndex);
							List<RefEdgeMulti> exist = refEdgeMultiAddMatchGroupMap.get(refEdgeMulti);
							if(exist == null) {
								List<RefEdgeMulti> create = new ArrayList<>(Collections.nCopies(size-1, null));
								create.set(i, refEdgeMulti);
								refEdgeMultiAddMatchGroupMap.put(refEdgeMulti, create);
							} else {
								exist.set(i, refEdgeMulti);
							}
						}
					}
				}			
			}
			System.out.println("============================");
		}

		// tmp
//		System.out.println("*******************************valEdgeAddMatchGroupMap********************************");
//		valEdgeAddMatchGroupMap.forEach((key, value) -> {
//			System.out.println("key.getType():        " + key.getType());
//			System.out.println("key.getSourceIndex(): " + key.getSourceIndex());
//			System.out.println("key.getTarget():      " + key.getTarget());
//			
//			value.forEach(v -> {
//				if(v != null) {
//					System.out.println("v.getType():        " + v.getType());
//					System.out.println("v.getSourceIndex(): " + v.getSourceIndex());
//					System.out.println("v.getTarget():      " + v.getTarget());
//				} else {
//					System.out.println(v);
//				}
//			});
//			System.out.println("-----------------------------------------------------");
//		});
//		System.out.println("\n\n\n");
//		
//		
//		System.out.println("*******************************valEdgeAddMatchGroupMap********************************");
//		valEdgeMultiAddMatchGroupMap.forEach((key, value) -> {
//			System.out.println("key.getType():         " + key.getType());
//			System.out.println("key.getSourceIndex():  " + key.getSourceIndex());
//			System.out.println("key.getTargets():      " + key.getTargets().size() + " "+ key.getTargets());
//			
//			value.forEach(v -> {
//				if(v != null) {
//					System.out.println("v.getType():         " + v.getType());
//					System.out.println("v.getSourceIndex():  " + v.getSourceIndex());
//					System.out.println("v.getTargets():      " + v.getTargets().size()+ " " + v.getTargets());
//				} else {
//					System.out.println(v);
//				}
//			});
//			System.out.println("-----------------------------------------------------");
//		});
//		System.out.println("\n\n\n");
//		
//		
//		System.out.println("*******************************refEdgeAddMatchGroupMap********************************");
//		refEdgeAddMatchGroupMap.forEach((key, value) ->{
//			System.out.println("key.getType():        " + key.getType());
//			System.out.println("key.getSourceIndex(): " + key.getSourceIndex());
//			System.out.println("key.getTargetIndex(): " + key.getTargetIndex());
//			
//			value.forEach(v -> {
//				if(v != null) {
//					System.out.println("v.getType():        " + v.getType());
//					System.out.println("v.getSourceIndex(): " + v.getSourceIndex());
//					System.out.println("v.getTargetIndex(): " + v.getTargetIndex());
//				} else {
//					System.out.println(v);
//				}
//			});
//			System.out.println("-----------------------------------------------------");
//		});
//		System.out.println("\n\n\n");
//		
//		
//		System.out.println("*******************************refEdgeMultiAddMatchGroupMap********************************");
//		refEdgeMultiAddMatchGroupMap.forEach((key, value) ->{
//			System.out.println("key.getType():         " + key.getType());
//			System.out.println("key.getSourceIndex():  " + key.getSourceIndex());
//			System.out.println("key.getTargetsIndex(): " + key.getTargetsIndex().size() + " " + key.getTargetsIndex());
//			
//			value.forEach(v -> {
//				if(v != null) {
//					System.out.println("v.getType():         " + v.getType());
//					System.out.println("v.getSourceIndex():  " + v.getSourceIndex());
//					System.out.println("v.getTargetsIndex(): " + v.getTargetsIndex().size() + " " + v.getTargetsIndex());
//				} else {
//					System.out.println(v);
//				}
//			});
//			System.out.println("-----------------------------------------------------");
//		});
//		System.out.println("\n\n\n");
		

		// 先计算点的合并结果
		URI m1URI = URI.createFileURI("E:\\eclipse-dsl202012\\edu.ustb.sei.mde.college\\src\\edu\\ustb\\sei\\mde\\college\\xmi\\college_m1.xmi");
		Resource m1Resource = new ResourceImpl(m1URI);
		List<Conflict> conflicts = new ArrayList<>();
		
		nodeMatchGroupMap.forEach((baseEObject, list) -> {
			// 先计算点的合并类型
			EClass baseEClass = baseEObject.eClass();
			EClass finalEClass = baseEClass;
			// 记录可能产生的冲突
			List<Integer> firstMayConflict = new ArrayList<>();
			List<Integer> secondMayConfclit = new ArrayList<>();
			for(int i=0; i<size-1; i++) {
				EObject branchEObject = list.get(i);
				if(branchEObject == null) {
					firstMayConflict.add(i);
				} else {
					EClass branchEClass = branchEObject.eClass();
					if(branchEClass != baseEClass) {
						secondMayConfclit.add(i);
					}
					finalEClass = computeSubType(finalEClass, branchEClass);	
				}
			}
			
			if(finalEClass == null) {
				// 冲突：类型不兼容
				
				
			} else if(firstMayConflict.size()> 0 && finalEClass!=baseEClass) {
				// 冲突：删除节点-修改节点类型
				Conflict conflict = new Conflict(firstMayConflict, ConflictKind.Delete, secondMayConfclit, ConflictKind.Update, "删除节点-修改节点类型");
				System.out.println("*****" + conflict.toString());
				conflicts.add(conflict);
				
			} else if(firstMayConflict.size() == 0){
				EObject finalEObject = EcoreUtil.create(finalEClass);
				m1Resource.getContents().add(finalEObject);	// may not correct?
				// 更新nodexIndexMap
				List<EObject> sourceIndex = nodesIndexMap.get(baseEObject);
				nodesIndexMap.put(finalEObject, sourceIndex);
			}
			// 其它应该就是删除
		});
		
		EClass finalType = null;
		
		for(List<EObject> list : nodeMatchGroupList) {
			for(int i=0; i<size-1; i++) {
				EObject addEObject = list.get(i);
				if(addEObject != null) {
					EClass addEClass = addEObject.eClass();
					if(finalType == null) {
						finalType = addEClass;
					} else {
						
					}
				}
				
			}
		}		
		
		// tmp
		System.out.println("\n\n\n*****************baseEResource");
		baseResource.getAllContents().forEachRemaining(e -> {
			System.out.println(e);
		});
		
		System.out.println("\n\n\n*****************m1Resource");
		m1Resource.getAllContents().forEachRemaining(e -> {
			System.out.println(e);
		});
		
		System.out.println("line 733");
		
		
	}

	private static EClass computeSubType(EClass leftEClass, EClass rightEClass) {
		if(leftEClass == rightEClass) {
			return leftEClass;
		}
		if(leftEClass.isSuperTypeOf(rightEClass)) {
			return rightEClass;
		}
		if(rightEClass.isSuperTypeOf(leftEClass)) {
			return leftEClass;
		}		
		return null;
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