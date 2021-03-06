package edu.ustb.sei.mde.nmc.nway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EDataTypeImpl;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.impl.ETypedElementImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.InteractionOperand;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.internal.impl.BehaviorExecutionSpecificationImpl;
import org.eclipse.uml2.uml.internal.impl.GeneralOrderingImpl;
import org.eclipse.uml2.uml.internal.impl.GeneralizationImpl;
import org.eclipse.uml2.uml.internal.impl.InteractionConstraintImpl;
import org.eclipse.uml2.uml.internal.impl.InteractionFragmentImpl;
import org.eclipse.uml2.uml.internal.impl.InteractionOperandImpl;
import org.eclipse.uml2.uml.internal.impl.LiteralSpecificationImpl;
import org.eclipse.uml2.uml.internal.impl.MessageOccurrenceSpecificationImpl;
import org.eclipse.uml2.uml.internal.impl.ModelImpl;
import org.eclipse.uml2.uml.internal.impl.ParameterImpl;
import org.eclipse.uml2.uml.internal.impl.PropertyImpl;

import edu.ustb.sei.mde.conflict.Conflict;
import edu.ustb.sei.mde.conflict.ConflictFactory;
import edu.ustb.sei.mde.conflict.Conflicts;
import edu.ustb.sei.mde.conflict.Tuple;
import edu.ustb.sei.mde.nmc.compare.Comparison;
import edu.ustb.sei.mde.nmc.compare.IComparisonScope;
import edu.ustb.sei.mde.nmc.compare.Match;
import edu.ustb.sei.mde.nmc.compare.internal.ComparisonSpec;
import edu.ustb.sei.mde.nmc.compare.internal.MatchSpec;
import edu.ustb.sei.mde.nmc.compare.start.DefaultComparisonScope;
import edu.ustb.sei.mde.nmc.compare.start.EMFCompare;

public class NWay {

	static int size;

	public static void nWay(List<Resource> resourceList, Map<Resource, Integer> resourceMap, EMFCompare build,
			Set<String> needOrderSet, Resource m1Resource, Resource conflictResource) {
		
		long currentTime = System.currentTimeMillis();
		
		size = resourceList.size();
		Comparison comparison = null;
		IComparisonScope scope = null;
		Resource baseResource = resourceList.get(0);
		Resource branchResource = null;

		/**
		 * ???????????????????????????????????? ??????????????????????????????????????????
		 */
		Map<EObject, List<EObject>> nodeMatchGroupMap = new HashMap<>();	// ????<eb, {e1, null, e3, ..., en}>
		Map<EObject, List<EObject>> nodeAddMatchGroupMap = new HashMap<>(); // ????<e3, {null, null, e3, e4, ..., null}>
		
		Map<Integer, List<EObject>> addMap = new HashMap<>();
		
		// ????????????????????????????checkValid
		Map<EObject, List<EObject>> nodesIndexMap = new HashMap<>();
		// ????????????????
		Map<EObject, List<EObject>> proxyHelperMap = new HashMap<>();

		for (int i = 1; i < size; i++) { // resourceList??????0????????baseResource
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
						// ????baseEObject????list????????????????
						nodesIndexMap.put(baseEObject, list);
										
					}
					
					list.add(branchEObject); // ????<eb, {e1, null, e3, ..., en}>
					// need to improve?
					if (branchEObject != null) {
						nodesIndexMap.put(branchEObject, list);
					}
					
					// lyt: ????????????????????????????id????????EInt????EInt??????eIsProxy()??true
					if(baseEObject instanceof ETypedElementImpl) {
						EStructuralFeature eType = baseEObject.eClass().getEStructuralFeature("eType");
						
						EObject baseProxyEObject = (EObject) baseEObject.eGet(eType, false);	// ????????????false??????????eProxyURI						
						
						if(baseProxyEObject != null && baseProxyEObject.eIsProxy()) {
																				
							List<EObject> listProxy = nodeMatchGroupMap.get(baseProxyEObject);
							if(listProxy == null) {
								listProxy = new ArrayList<>(size-1);
								nodeMatchGroupMap.put(baseProxyEObject, listProxy);	
								nodesIndexMap.put(baseProxyEObject, listProxy);
								
							} 
							
							if(branchEObject != null) {
								EObject branchProxyEObject = (EObject) branchEObject.eGet(eType, false);
								if(branchProxyEObject != null) {
									listProxy.add(branchProxyEObject);
									nodesIndexMap.put(branchProxyEObject, listProxy);
								} else {
									listProxy.add(null);
								}
								
							} else {
								listProxy.add(null);
							}
														
						} else {
							if(branchEObject != null) {																						
								EObject branchProxyEObject = (EObject) branchEObject.eGet(eType, false);
								
								if(branchProxyEObject != null && branchProxyEObject.eIsProxy()) {
									List<EObject> proxyAddMatchGroup = proxyHelperMap.get(baseEObject);
									if(proxyAddMatchGroup == null) {
										proxyAddMatchGroup = new ArrayList<>(Collections.nCopies(size-1, null));
										proxyHelperMap.put(baseEObject, proxyAddMatchGroup);
										nodeAddMatchGroupMap.put(branchProxyEObject, proxyAddMatchGroup);
									}
																		
									Integer index = resourceMap.get(branchEObject.eResource());	// ??????resourceMap????????0????????????1
									proxyAddMatchGroup.set(index, branchProxyEObject);
									nodesIndexMap.put(branchProxyEObject, proxyAddMatchGroup);

								}
							}												
						}						
					}

				} else {
					List<EObject> addList = addMap.get(i);
					if (addList == null) {
						addList = new ArrayList<>();
						addMap.put(i, addList); // ????????????????????????????????????addList
					}
					addList.add(branchEObject);
														
				}
								
			}

		}

		// tmp
//		System.out.println("\n\n\n------------------------matchGroupMap------------------------------- ");
//		nodeMatchGroupMap.forEach((key, value) -> {
//			System.out.println("key: " + key);
//			value.forEach(e -> {
//				System.out.println(e);
//			});
//			System.out.println();
//		});
//		System.out.println("\n\n\n");

		/** ??????????????????i??????j?????? */
		MultiKeyMap<Integer, List<Match>> preMatchMap = new MultiKeyMap<>();
		
		for(List<EObject> value : nodeMatchGroupMap.values()) {
			for (int i = 1; i < size - 1; i++) {
				EObject leftEObject = value.get(i - 1); // ??????value??????0????????????????1
				boolean flag = false;				
				
				for (int j = i + 1; j < size; j++) {
					EObject rightEObject = value.get(j - 1);
					
					// lyt??????????????????????????????????match????????????????EGenericType??????
					if(leftEObject != null && leftEObject.eIsProxy()) {
						flag = true;
						break;
					}					
					if(rightEObject != null && rightEObject.eIsProxy()) {
						flag = true;
						break;
					}
					
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
				
				if(flag == true) {
					break;
				}
				
			}
		}
		

		/** ?????????????????????????????? */
		Set<EObject> vertices = new HashSet<>();
		Set<Match> edges = new HashSet<>();
		MultiKeyMap<EObject, Double> distanceMap = new MultiKeyMap<>(); // ????????????????
		for (int i = 1; i <= size - 1; i++) {
			List<EObject> leftEObjects = addMap.get(i);
			if (leftEObjects != null) {
				vertices.addAll(leftEObjects);
				for (int j = i + 1; j < size; j++) {
					List<EObject> rightEObjects = addMap.get(j);
					if (rightEObjects != null) {
						vertices.addAll(rightEObjects);
						// ??????????????
						Comparison comparisonADD = new ComparisonSpec();
						List<Match> preMatchList = preMatchMap.get(i, j);
						comparisonADD.getMatches().addAll(preMatchList);

						long start = System.currentTimeMillis();
						build.compareADD(comparisonADD, leftEObjects, rightEObjects, distanceMap);
						long end = System.currentTimeMillis();
						System.out.println("\n\n\nADD MATCH TIME: " + (end - start) + " ms.");

						// ????????????????????
						int num = preMatchList.size();
						List<Match> collect = comparisonADD.getMatches().stream().skip(num)
								.filter(m -> m.getLeft() != null && m.getRight() != null).collect(Collectors.toList());
						edges.addAll(collect);
					}
				}
			}
		}

		// tmp
//		System.out.println("\n\n\n----------------------vertices: ");
//		vertices.forEach(v -> {
//			System.out.println(v);
//		});
//
//		System.out.println("\n\n\n----------------------edges: ");
//		edges.forEach(e -> {
//			System.out.println(e.getLeft());
//			System.out.println(e.getRight());
//			System.out.println();
//		});
//		System.out.println("\n\n\n");

		/** ???????????????? */
		
		// ????????????????????????????BKPivot??????????????
		MaximalCliquesWithPivot ff = new MaximalCliquesWithPivot();
		ff.initGraph(vertices, edges);
		List<List<EObject>> maximalCliques = new ArrayList<>();
		if (vertices.size() > 0) {
			ff.Bron_KerboschPivotExecute(maximalCliques);
		}

		// tmp
//		System.out.println("\n\n\n----------------------maximalCliques: ");
//		maximalCliques.forEach(m -> {
//			m.forEach(e -> {
//				System.out.println("e: " + e);
//			});
//			System.out.println();
//		});
//		System.out.println("\n\n\n");

		// ????????????????????????????????????????

		while (maximalCliques.size() > 0) {
			Map<List<EObject>, Double> map = new HashMap<>();
			for (List<EObject> clique : maximalCliques) {
				// ??????container??????????container??????????????????
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
						sum = 1; // sum????0????????????5????sum??0??????????1????????????????
					}
					int cSize = clique.size();
					if (clique.size() == 1) {
						cSize = 0; // ??????????????????????cSize??0????????????????????????
					}

					map.put(clique, Double.valueOf(cSize) / sum);
				}
			}

			// tmp
//			map.forEach((key, value) -> {
//				key.forEach(e -> {
//					System.out.println(e);
//				});
//
//				System.out.println(value);
//				System.out.println();
//			});

			// ????value????????????????????????????????????
			Optional<Entry<List<EObject>, Double>> findFirst = map.entrySet().stream()
					.sorted(Map.Entry.<List<EObject>, Double>comparingByValue().reversed()).findFirst();
			List<EObject> matchGroup = findFirst.get().getKey();
			maximalCliques.remove(matchGroup);

			
			List<EObject> nodeAddMatchGroup = new ArrayList<>(Collections.nCopies(size - 1, null));			
			// lyt: ????eProxy??????
			List<EObject> proxyAddMatchGroup = new ArrayList<>(Collections.nCopies(size - 1, null));
			EObject firstProxyEObject = null;
			boolean flag = true;
			
			for(EObject e : matchGroup) {
				Integer index = resourceMap.get(e.eResource());
				nodeAddMatchGroup.set(index, e); // ??????????null??????{e1, e2, null, ..., en}
				// ????nodesIndexMap??need to improve?
				nodesIndexMap.put(e, nodeAddMatchGroup);
				
				// lyt: ??????????????????
				if(e instanceof ETypedElementImpl) {	
					EStructuralFeature eType = e.eClass().getEStructuralFeature("eType");
					EObject proxyEObject = (EObject) e.eGet(eType, false);					
					
					if(proxyEObject != null && proxyEObject.eIsProxy()) {
						proxyAddMatchGroup.set(index, proxyEObject);										
						nodesIndexMap.put(proxyEObject, proxyAddMatchGroup);
						// ????nodeAddMatchGroupMap????put
						if(flag == true) {
							firstProxyEObject = proxyEObject;
							flag = false;
						}
					}
					
				}
			}

			// ??????????????????????key????????????????????
			nodeAddMatchGroupMap.put(matchGroup.get(0), nodeAddMatchGroup);
			
			// lyt: ??????????????????????????????????????key????????????????????
			if(firstProxyEObject != null) {				
				nodeAddMatchGroupMap.put(firstProxyEObject, proxyAddMatchGroup);
			}
			
			// tmp
//			System.out.println("**************????????***************");
//			matchGroup.forEach(e -> {
//				System.out.println(e);
//			});
//			System.out.println("\n\n\n\n\n");

			// ??????????????????????EObject????clique.remove(EObejct)
			// ??clique??????????????????maximalCliques.remove(clique)
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
//		System.out.println("\n\n\n----------------------nodeAddMatchGroupMap: ");
//		nodeAddMatchGroupMap.values().forEach(list -> {
//			list.forEach(e -> {
//				System.out.println(e);
//			});
//			System.out.println();
//		});
//		System.out.println("\n\n\n");

		/** ?????????????? */
		// ????????????????????????4??Map
		Map<ValEdge, List<ValEdge>> valEdgeMatchGroupMap = new HashMap<>();
		Map<ValEdgeMulti, List<ValEdgeMulti>> valEdgeMultiMatchGroupMap = new HashMap<>();

		Map<RefEdge, List<RefEdge>> refEdgeMatchGroupMap = new HashMap<>();
		Map<RefEdgeMulti, List<RefEdgeMulti>> refEdgeMultiMatchGroupMap = new HashMap<>();

		// ??????????????????????????????????????
		MultiKeyMap<Object, ValEdgeMulti> valEdgeMultiHelper = new MultiKeyMap<>();
		MultiKeyMap<Object, RefEdgeMulti> refEdgeMultiHelper = new MultiKeyMap<>();
				
		for (Entry<EObject, List<EObject>> entry : nodeMatchGroupMap.entrySet()) {
			EObject baseEObject = entry.getKey();
			List<EObject> list = entry.getValue();
			
			System.out.println("\n\n***************************baseEObject***************************");
			System.out.println("baseEObject: " + baseEObject);
			List<EObject> sourceIndex = nodesIndexMap.get(baseEObject); // e.sourceIndex;

			EClass eClass = baseEObject.eClass();

//			System.out.println("-----------------------------??????????????????????????????????------------------------------------------");	
			for (EAttribute a : eClass.getEAllAttributes()) {

				if (a.isChangeable() == false) {
					continue;
				}

				if (a.isMany() == false) { // ????????
					Object target = baseEObject.eGet(a);
					ValEdge valEdge = new ValEdge(a, sourceIndex, target); // target maybe unset
					List<ValEdge> create = new ArrayList<>(Collections.nCopies(size - 1, null));
					valEdgeMatchGroupMap.put(valEdge, create);

				} else { // ????????
					List<Object> targets = (List<Object>) baseEObject.eGet(a); // targets maybe unset
					ValEdgeMulti valEdgeMulti = new ValEdgeMulti(a, sourceIndex, targets);
					List<ValEdgeMulti> create = new ArrayList<>(Collections.nCopies(size - 1, null));
					valEdgeMultiMatchGroupMap.put(valEdgeMulti, create);

					// ??????????????????????????????
					if (needOrderSet.contains(a.getName())) {
						valEdgeMultiHelper.put(a, sourceIndex, valEdgeMulti);
					}

				}
			}
			
//			System.out.println("----------------------------??????????????????????????????????-------------------------------------------");				
			for (EReference r : eClass.getEAllReferences()) {

				if (r.isChangeable() == false || r.getName().equals("eGenericType")
						|| r.getName().equals("eGenericSuperTypes")) {										
					continue;
				}

				if (r.isMany() == false) { // ????????							
					// lyt: ????????false??????eType????value????????eProxyURI
					EObject value = (EObject) baseEObject.eGet(r, false);
															
					List<EObject> targetIndex = nodesIndexMap.get(value);
					RefEdge refEdge = new RefEdge(r, sourceIndex, targetIndex); // targetIndex maybe unset
					List<RefEdge> create = new ArrayList<>(Collections.nCopies(size - 1, null));
					refEdgeMatchGroupMap.put(refEdge, create);
										
				} else { // ????????
					List<EObject> values = (List<EObject>) baseEObject.eGet(r);
					List<List<EObject>> targetsIndex = new ArrayList<>();
					values.forEach(eObj -> {
						targetsIndex.add(nodesIndexMap.get(eObj));
					});

					RefEdgeMulti refEdgeMulti = new RefEdgeMulti(r, sourceIndex, targetsIndex); // targetsIndex may
																								// unset
					List<RefEdgeMulti> create = new ArrayList<>(Collections.nCopies(size - 1, null));
					refEdgeMultiMatchGroupMap.put(refEdgeMulti, create);

					// ??????????????????????????????
					if (needOrderSet.contains(r.getName())) {
						refEdgeMultiHelper.put(r, sourceIndex, refEdgeMulti);
					}
				}
			}

			for (int i = 0; i < size - 1; i++) {
				EObject branchEObject = list.get(i);
				System.out.println("\n\n------------------????????list--------------------");
				if (branchEObject != null) {
					// ????????????matchGroup??sourceIndex????
					System.out.println("branchEObject: " + branchEObject);

					EClass eClass2 = branchEObject.eClass(); // PENDING: may not the same as eClass.

//					System.out.println("---------------------------??????????????????????????????????--------------------------------------------");
					for (EAttribute a2 : eClass2.getEAllAttributes()) {

						if (a2.isChangeable() == false) {
							continue;
						}

						if (a2.isMany() == false) { // ????????
							Object target2 = branchEObject.eGet(a2);
							ValEdge valEdge2 = new ValEdge(a2, sourceIndex, target2);
							valEdgeMatchGroupMap.get(valEdge2).set(i, valEdge2); // PENDING: what if get(valEdge2) is
																					// null?

						} else { // ????????
							List<Object> targets2 = (List<Object>) branchEObject.eGet(a2);
							ValEdgeMulti valEdgeMulti2 = new ValEdgeMulti(a2, sourceIndex, targets2);
							valEdgeMultiMatchGroupMap.get(valEdgeMulti2).set(i, valEdgeMulti2);

						}
					}

//					System.out.println("----------------------------??????????????????????????????????-------------------------------------------");
					for (EReference r2 : eClass2.getEAllReferences()) {

						if (r2.isChangeable() == false || r2.getName().equals("eGenericType")
								|| r2.getName().equals("eGenericSuperTypes")) {										
							continue;
						}

						if (r2.isMany() == false) { // ????????
							// lyt: ????false??????eType????value2????????eProxyURI
							EObject value2 = (EObject) branchEObject.eGet(r2, false);
																																		
							List<EObject> targetIndex2 = nodesIndexMap.get(value2);
							RefEdge refEdge2 = new RefEdge(r2, sourceIndex, targetIndex2);
							refEdgeMatchGroupMap.get(refEdge2).set(i, refEdge2);												
							
						} else { // ????????
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
		}



		// ????????????????
		Map<ValEdge, List<ValEdge>> valEdgeAddMatchGroupMap = new HashMap<>();
		Map<ValEdgeMulti, List<ValEdgeMulti>> valEdgeMultiAddMatchGroupMap = new HashMap<>();

		Map<RefEdge, List<RefEdge>> refEdgeAddMatchGroupMap = new HashMap<>();
		Map<RefEdgeMulti, List<RefEdgeMulti>> refEdgeMultiAddMatchGroupMap = new HashMap<>();

		// ????????????
		MultiKeyMap<Object, ValEdgeMulti> valEdgeMultiAddHelper = new MultiKeyMap<>();
		MultiKeyMap<Object, RefEdgeMulti> refEdgeMultiAddHelper = new MultiKeyMap<>();

		for (List<EObject> list : nodeAddMatchGroupMap.values()) {
			for (int i = 0; i < size - 1; i++) {
				EObject addEObject = list.get(i);
				if (addEObject != null) {
					EClass eClass = addEObject.eClass();
					List<EObject> sourceIndex = nodesIndexMap.get(addEObject);

//					System.out.println("-----------------------------????????????????????------------------------------------------");	
					for (EAttribute a : eClass.getEAllAttributes()) {

						if (a.isChangeable() == false) {
							continue;
						}

						if (a.isMany() == false) { // ????????
							Object target = addEObject.eGet(a);
							ValEdge valEdge = new ValEdge(a, sourceIndex, target);
							List<ValEdge> exist = valEdgeAddMatchGroupMap.get(valEdge);
							if (exist == null) {
								List<ValEdge> create = new ArrayList<>(Collections.nCopies(size - 1, null));
								create.set(i, valEdge);
								valEdgeAddMatchGroupMap.put(valEdge, create); // ??????????????????????????????base????????????????????????base
							} else {
								exist.set(i, valEdge);
							}

						} else { // ????????
							List<Object> targets = (List<Object>) addEObject.eGet(a);
							ValEdgeMulti valEdgeMulti = new ValEdgeMulti(a, sourceIndex, targets);
							List<ValEdgeMulti> exist = valEdgeMultiAddMatchGroupMap.get(valEdgeMulti);
							if (exist == null) {
								List<ValEdgeMulti> create = new ArrayList<>(Collections.nCopies(size - 1, null));
								create.set(i, valEdgeMulti);
								valEdgeMultiAddMatchGroupMap.put(valEdgeMulti, create);
							} else {
								exist.set(i, valEdgeMulti);
							}

							// ??????????????????????????????
							if (needOrderSet.contains(a.getName())) {
								valEdgeMultiAddHelper.put(a, sourceIndex, valEdgeMulti);
							}
						}
					}

//					System.out.println("-----------------------------????????????????????------------------------------------------");	
					for (EReference r : eClass.getEAllReferences()) {

						if (r.isChangeable() == false || r.getName().equals("eGenericType")
								|| r.getName().equals("eGenericSuperTypes")) {										
							continue;
						}

						if (r.isMany() == false) { // ????????
							// lyt: ????????false??????eType????value????????eProxyURI
							EObject value = (EObject) addEObject.eGet(r, false);
														
							List<EObject> targetIndex = nodesIndexMap.get(value);
							RefEdge refEdge = new RefEdge(r, sourceIndex, targetIndex);
							List<RefEdge> exist = refEdgeAddMatchGroupMap.get(refEdge);
							if (exist == null) {
								List<RefEdge> create = new ArrayList<>(Collections.nCopies(size - 1, null));
								create.set(i, refEdge);
								refEdgeAddMatchGroupMap.put(refEdge, create);
							} else {
								exist.set(i, refEdge);
							}

						} else { // ????????
							List<EObject> values = (List<EObject>) addEObject.eGet(r);
							List<List<EObject>> targetsIndex = new ArrayList<>();
							values.forEach(v -> {
								targetsIndex.add(nodesIndexMap.get(v));
							});
							RefEdgeMulti refEdgeMulti = new RefEdgeMulti(r, sourceIndex, targetsIndex);
							List<RefEdgeMulti> exist = refEdgeMultiAddMatchGroupMap.get(refEdgeMulti);
							if (exist == null) {
								List<RefEdgeMulti> create = new ArrayList<>(Collections.nCopies(size - 1, null));
								create.set(i, refEdgeMulti);
								refEdgeMultiAddMatchGroupMap.put(refEdgeMulti, create);
							} else {
								exist.set(i, refEdgeMulti);
							}

							// ??????????????????????????????
							if (needOrderSet.contains(r.getName())) {
								refEdgeMultiAddHelper.put(r, sourceIndex, refEdgeMulti);
							}

						}
					}
				}
			}
		}

		/** ?????????????????? */
		Map<List<EObject>, EObject> nodesReverseIndexMap = new HashMap<>();
		List<EObject> m1ResourceEObjects = new ArrayList<>(); // ??????????????????
		List<EObject> conflictResourceEObjects = new ArrayList<>();// ??????????????????

		List<Conflict> conflictList = new ArrayList<>(); // ????conflicts??????eSet??????????conflictResourceEObjects??
		List<Tuple> tupleList = new ArrayList<>(); // ????????conflictResourceEObjects??

		// ??????????????
		for (Entry<EObject, List<EObject>> entry : nodeMatchGroupMap.entrySet()) {
			EObject baseEObject = entry.getKey();
			List<EObject> list = entry.getValue();
			
			if(baseEObject.eIsProxy()) {
				boolean flag = true;
				boolean updateConflictFlag = false;
				
				// ??????????????????
				List<Tuple> deleteMayConflict = new ArrayList<>();
				List<Tuple> updateMayConflict = new ArrayList<>();
				
				// ??fragment????URI.toString()????
				String baseFragment = null;
				if(baseEObject instanceof EDataTypeImpl) {
					baseFragment = ((EDataTypeImpl) baseEObject).eProxyURI().fragment();					
				}
				String finalFragment = baseFragment;
				
				EObject finalEObject = baseEObject;		

				for(int i=0; i<size-1; i++) {
					EObject branchEObject = list.get(i);
					if(branchEObject == null) {
						// ??????????????????????1
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						deleteMayConflict.add(tuple);

					} else {
						String branchFragment = ((EDataTypeImpl) branchEObject).eProxyURI().fragment();
						if(branchFragment.equals(baseFragment) == false) {
							// ??????1
							Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
							tuple.setBranch(i + 1);
							tuple.setEObject(branchEObject);
							updateMayConflict.add(tuple);
							
							if (flag == true) {
								finalEObject = branchEObject;
								finalFragment = branchFragment;
								flag = false;
							} else {
								if(branchFragment.equals(finalFragment) == false) {
									if (updateConflictFlag == false) {
										updateConflictFlag = true;
									}
								}
							}
						} 
					}
				}
				
				// PNEDING: ??????????????????????????????id: EInt????????id??
				if (updateConflictFlag == true) {
					Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
					conflict.getTuples().addAll(updateMayConflict);
					conflict.setInformation("????????????????????");	
					conflictList.add(conflict);
					tupleList.addAll(updateMayConflict);
				}

				if (deleteMayConflict.size() > 0 && updateMayConflict.size() > 0) { // ????????????????????
					Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
					deleteMayConflict.addAll(updateMayConflict); // ??????
					conflict.getTuples().addAll(deleteMayConflict);
					conflict.setInformation("??????????????-??????????????");
					conflictList.add(conflict);
					tupleList.addAll(deleteMayConflict);
				}

				if (deleteMayConflict.size() == 0) {
					// lyt: ????????????????????????????????????????nodesReverseIndexMap????????????m1ResourceEObjects??
					List<EObject> sourceIndex = nodesIndexMap.get(baseEObject);
					// ????nodexReverseIndexMap
					nodesReverseIndexMap.put(sourceIndex, finalEObject);
				}
				
			} else {	// ??????????
				boolean flag = true;
				boolean updateConflictFlag = false;
				
				// ??????????????????
				List<Tuple> deleteMayConflict = new ArrayList<>();
				List<Tuple> updateMayConflict = new ArrayList<>();
				
				// ??????????????????
				EClass baseEClass = baseEObject.eClass();
				EClass finalEClass = baseEClass;
				
				for (int i = 0; i < size - 1; i++) {
					EObject branchEObject = list.get(i);
					if (branchEObject == null) {
						// ??????????????????????1
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						deleteMayConflict.add(tuple);

					} else {
						EClass branchEClass = branchEObject.eClass();
						if (branchEClass != baseEClass) { // EClass??????????
							// ??????1
							Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
							tuple.setBranch(i + 1);
							tuple.setEObject(branchEObject);
							updateMayConflict.add(tuple);

							if (flag == true) {
								finalEClass = branchEClass;
								flag = false;
							} else {
								finalEClass = computeSubType(finalEClass, branchEClass);
								if (updateConflictFlag == false && finalEClass == null) {
									updateConflictFlag = true;
								}
							}
						}
					}
				}
				
				if (updateConflictFlag == true) {
					// ????????????????????????
					Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
					conflict.getTuples().addAll(updateMayConflict);
					conflict.setInformation("??????????????????");
					conflictList.add(conflict);
					tupleList.addAll(updateMayConflict);
				}

				if (deleteMayConflict.size() > 0 && updateMayConflict.size() > 0) { // ????????????????????
					// ??????????????-??????????????
					Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
					deleteMayConflict.addAll(updateMayConflict); // ??????
					conflict.getTuples().addAll(deleteMayConflict);
					conflict.setInformation("????????-??????????????");
					conflictList.add(conflict);
					tupleList.addAll(deleteMayConflict);
				}

				if (deleteMayConflict.size() == 0) {	
					EObject create = EcoreUtil.create(finalEClass);							
					// ????nodesIndexMap
					List<EObject> sourceIndex = nodesIndexMap.get(baseEObject);
					nodesIndexMap.put(create, sourceIndex);
					// ????nodexReverseIndexMap
					nodesReverseIndexMap.put(sourceIndex, create);
					// ????resourceNodes
					m1ResourceEObjects.add(create);
				}
			}
			
		}
		

	
		// ????????????
		for (Entry<EObject, List<EObject>> entry : nodeAddMatchGroupMap.entrySet()) {
			EObject firstEObject = entry.getKey();
			List<EObject> list = entry.getValue();
			
			if(firstEObject.eIsProxy()) {
				boolean flag = true;			
				boolean addConflictFlag = false;
				
				// ??????????????????
				List<Tuple> addMayConflict = new ArrayList<>();
				
				String finalFragment = null;
				
				EObject finalEObject = null;
				
				for (int i = 0; i < size - 1; i++) {
					EObject addEObject = list.get(i);	// ??????????firstEObject
					if (addEObject != null) {
						String addFragment = ((EDataTypeImpl) addEObject).eProxyURI().fragment();
						// ????????????1
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						tuple.setEObject(addEObject);
						addMayConflict.add(tuple);
						
						if(flag == true) {
							finalEObject = addEObject;
							finalFragment = addFragment;
							flag = false;
						} else {
							if(finalFragment.equals(addFragment) == false) {
								if(addConflictFlag == false) {
									addConflictFlag = true;
								}
							}
						}
					}
				}
				
				if (addConflictFlag == true) {
					Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
					conflict.getTuples().addAll(addMayConflict);
					conflict.setInformation("??????????????????????");
					conflictList.add(conflict);
					tupleList.addAll(addMayConflict);
				}
				
				// lyt: ??????????????????????????nodesReverseIndexMap????????????m1ResourceEObjects
				List<EObject> sourceIndex = nodesIndexMap.get(firstEObject);
				// ????nodesReverseIndexMap
				nodesReverseIndexMap.put(sourceIndex, finalEObject);
				
			} else {	// ??????????
				boolean flag = true;			
				boolean addConflictFlag = false;
				
				// ??????????????????
				List<Tuple> addMayConflict = new ArrayList<>();

				EClass finalType = null;
				
				for (int i = 0; i < size - 1; i++) {
					EObject addEObject = list.get(i);
					if (addEObject != null) {
						EClass addEClass = addEObject.eClass();
						// ????????????1
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						tuple.setEObject(addEObject);
						addMayConflict.add(tuple);

						if (flag == true) {
							finalType = addEClass;
							flag = false;
						} else {
							finalType = computeSubType(finalType, addEClass);
							if (addConflictFlag == false && finalType == null) {
								addConflictFlag = true;
							}
						}
					}
				}

				if (addConflictFlag == true) {
					// ????????????????????????????????????????????????????
					Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
					conflict.getTuples().addAll(addMayConflict);
					conflict.setInformation("??????????????????");
					conflictList.add(conflict);
					tupleList.addAll(addMayConflict);
				}
				
				EObject create = EcoreUtil.create(finalType);			
				// ????nodesIndexMap
				List<EObject> sourceIndex = nodesIndexMap.get(firstEObject);
				nodesIndexMap.put(create, sourceIndex);
				// ????nodesReverseIndexMap
				nodesReverseIndexMap.put(sourceIndex, create);
				// ????m1ResourceEObjects
				m1ResourceEObjects.add(create);
				
			}
			
		}
		
		
		
		
		
		
		// PENDING: Diff
		
		
		
		
		

		/** ValEdge?????????? **/
		System.out.println("\n\n\n");
		MultiKeyMap<Object, Object> valEdge_MultiKeyMap = new MultiKeyMap<>();
		for (Entry<ValEdge, List<ValEdge>> entry : valEdgeMatchGroupMap.entrySet()) {
			ValEdge key = entry.getKey();

			EAttribute a = key.getType();
			if (a.isChangeable() == false) {
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();
			Object baseTarget = key.getTarget();
			List<ValEdge> list = entry.getValue();

			List<Tuple> deleteMayConflict = new ArrayList<>();
			List<Tuple> updateMayConflict = new ArrayList<>();

			Object finalTarget = baseTarget;
			boolean flag = true;
			boolean updateConflictFlag = false;

			for (int i = 0; i < size - 1; i++) {
				ValEdge valEdge = list.get(i);
				if (valEdge == null) {
					// ??????????????????????????????
					Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
					tuple.setBranch(i + 1);
					deleteMayConflict.add(tuple);

				} else {
					Object branchTarget = valEdge.getTarget();
					if (branchTarget == null && baseTarget == null) { // null??null??equals??????????
						continue;
					}
					if (branchTarget.equals(baseTarget) == false) { // Object??equals????
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						tuple.setEObject(sourceIndex.get(i));
						tuple.setEStructuralFeature(a);
						// ??????????position??null??~
						updateMayConflict.add(tuple);

						if (flag == true) {
							finalTarget = branchTarget;
							flag = false;
						} else {
							if (updateConflictFlag == false && branchTarget.equals(finalTarget) == false) { // Object??equals????
								updateConflictFlag = true;
							}
						}
					}
				}
			}

			if (updateConflictFlag == true) {
				// ????????????????????????
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				conflict.getTuples().addAll(updateMayConflict);
				conflict.setInformation("??????????????????");
				conflictList.add(conflict);
				tupleList.addAll(updateMayConflict);
			}

			if (deleteMayConflict.size() > 0 && updateMayConflict.size() > 0) {
				// ????????????-??????????????????
				// ????????????????????????????sourceIndex??????????????????????
				// ??????????????????????????????
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				deleteMayConflict.addAll(updateMayConflict); // ??????
				conflict.getTuples().addAll(deleteMayConflict);
				conflict.setInformation("????????-??????????????????");
				conflictList.add(conflict);
				tupleList.addAll(deleteMayConflict);
			}

			// ??????conflictsSize == valEdgeConflicts.size()
			// ??????eSet??????NullPointer????
			if (deleteMayConflict.size() == 0) {
				valEdge_MultiKeyMap.put(a, sourceIndex, finalTarget);
			}
		}

		/** ValEdgeMulti?????????? */
		System.out.println("\n\n\n");
		MultiKeyMap<Object, Object> valEdgeMulti_MultiKeyMap = new MultiKeyMap<>();
		for (Entry<ValEdgeMulti, List<ValEdgeMulti>> entry : valEdgeMultiMatchGroupMap.entrySet()) {
			ValEdgeMulti key = entry.getKey();

			EAttribute a = key.getType();
			if (a.isChangeable() == false) {
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();
			List<Object> baseTargets = key.getTargets();

			List<ValEdgeMulti> list = entry.getValue();
			List<Tuple> deleteMayConflict = new ArrayList<>();
			List<Tuple> addValueMayConflict = new ArrayList<>();

			List<Object> remain = new ArrayList<>(baseTargets); // remain????????baseTargets??????
			Set<Object> addition = new HashSet<>(); // ??????????????

			for (int i = 0; i < size - 1; i++) {
				ValEdgeMulti valEdgeMulti = list.get(i);
				if (valEdgeMulti == null) {
					// ??????????????????????????????
					Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
					tuple.setBranch(i + 1);
					deleteMayConflict.add(tuple);

				} else { // ??????????
					List<Object> valEdgeMultiTargets = valEdgeMulti.getTargets();
					List<Object> branchTargets = new ArrayList<>(valEdgeMultiTargets); // ??????????????????
					// ????????????????????????????????
					remain.retainAll(branchTargets);
					// ??????????????????????????????
					branchTargets.removeAll(baseTargets);
					addition.addAll(branchTargets);

					// ????????????????????????????????????
					List<Integer> locations = new ArrayList<>();
					branchTargets.forEach(o -> {
						int location = valEdgeMultiTargets.indexOf(o);
						locations.add(location);
					});

					// ??????????????????
					if (branchTargets.size() > 0) {
						// ??????????????????
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						tuple.setEObject(sourceIndex.get(i));
						tuple.setEStructuralFeature(a);
						tuple.getPosition().addAll(locations);
						addValueMayConflict.add(tuple);
					}
				}
			}
			// remain??addition??????????????remain????????????
			remain.addAll(addition);

			// ??????????????-???????????????

			// ??????????????-??????????????????
			if (deleteMayConflict.size() > 0 && addValueMayConflict.size() > 0) {
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				deleteMayConflict.addAll(addValueMayConflict); // ??????
				conflict.getTuples().addAll(deleteMayConflict);
				conflict.setInformation("????????-??????????????????");
				conflictList.add(conflict);
				tupleList.addAll(deleteMayConflict);
			}

			// ??????conflictsSize == valEdgeMultiConflicts.size()????????
			// ??????eSet??????NullPointer????
			if (deleteMayConflict.size() == 0) {
				valEdgeMulti_MultiKeyMap.put(a, sourceIndex, remain);
			}
		}

		/** ValEdgeAdd?????????? */
		System.out.println("\n\n\n");
		for (Entry<ValEdge, List<ValEdge>> entry : valEdgeAddMatchGroupMap.entrySet()) {
			ValEdge key = entry.getKey();

			EAttribute a = key.getType();
			if (a.isChangeable() == false) {
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();
			List<ValEdge> list = entry.getValue();
			Object finalTarget = null;

			boolean flag = true;
			boolean addConflictFlag = false;
			List<Tuple> addMayConflict = new ArrayList<>();

			for (int i = 0; i < size - 1; i++) {
				ValEdge valEdge = list.get(i);
				if (valEdge != null) {
					Object addTarget = valEdge.getTarget();
					// ????????
					Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
					tuple.setBranch(i + 1);
					tuple.setEObject(sourceIndex.get(i));
					tuple.setEStructuralFeature(a);
					// ??????????position??null??~
					addMayConflict.add(tuple);

					if (flag == true) {
						finalTarget = addTarget;
						flag = false;
					} else {
						if (addTarget == null && finalTarget == null) { // null??null??equals??????????
							continue;
						}
						if (addConflictFlag == false && addTarget.equals(finalTarget) == false) { // Object??equals????
							addConflictFlag = true;
						}
					}
				}
			}

			if (addConflictFlag == true) {
				// ??????????????????????????
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				conflict.getTuples().addAll(addMayConflict);
				conflict.setInformation("????????????????????");
				conflictList.add(conflict);
				tupleList.addAll(addMayConflict);
			}

			// ????conflictsSize == valEdgeConflicts.size()
			// ??????eSet??????????
			valEdge_MultiKeyMap.put(a, sourceIndex, finalTarget);

		}

		/** ValEdgeMultiAdd */
		System.out.println("\n\n\n");
		for (Entry<ValEdgeMulti, List<ValEdgeMulti>> entry : valEdgeMultiAddMatchGroupMap.entrySet()) {
			ValEdgeMulti key = entry.getKey();

			EAttribute a = key.getType();
			if (a.isChangeable() == false) {
				continue;
			}

			List<ValEdgeMulti> list = entry.getValue();
			List<Object> remain = new ArrayList<>();
			Set<Object> addition = null;
			boolean flag = true;

			for (int i = 0; i < size - 1; i++) {
				ValEdgeMulti valEdgeMulti = list.get(i);
				if (valEdgeMulti != null) {
					List<Object> addTargets = valEdgeMulti.getTargets();
					if (flag == true) {
						addition = new HashSet<>(addTargets); // ??????????????
						flag = false;
					} else {
						// ??????
						addition.addAll(addTargets);
					}
				}
			}

			// ??????????????????????????????????????????????????????????????????????
			// ??????????????????????????????

			remain.addAll(addition); // ????????????List
			valEdgeMulti_MultiKeyMap.put(a, key.getSourceIndex(), remain);
		}

		/** RefEdge?????????? */
		System.out.println("\n\n\n");
		MultiKeyMap<Object, List<EObject>> refEdge_MultiKeyMap = new MultiKeyMap<>();
		for (Entry<RefEdge, List<RefEdge>> entry : refEdgeMatchGroupMap.entrySet()) {
			RefEdge key = entry.getKey();

			EReference r = key.getType();
			// eType??????????eGenericType????????????
			if (r.isChangeable() == false || r.getName().equals("eGenericType")
					|| r.getName().equals("eGenericSuperTypes")) {										
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();
			List<EObject> baseTargetIndex = key.getTargetIndex();
			List<EObject> finalTargetIndex = baseTargetIndex;
			List<RefEdge> list = entry.getValue();

			List<Tuple> deleteMayConflict = new ArrayList<>();
			List<Tuple> updateMayConflict = new ArrayList<>();

			boolean flag = true;
			boolean updateConflictFlag = false;

			for (int i = 0; i < size - 1; i++) {
				RefEdge refEdge = list.get(i);
				if (refEdge == null) {
					// ??????????????????????????
					Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
					tuple.setBranch(i + 1);
					deleteMayConflict.add(tuple);

				} else {
					List<EObject> branchTargetIndex = refEdge.getTargetIndex();
					if (branchTargetIndex != baseTargetIndex) {
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						tuple.setEObject(sourceIndex.get(i));
						tuple.setEStructuralFeature(r);
						// ??????????position??null??~
						updateMayConflict.add(tuple);

						if (flag == true) {
							finalTargetIndex = branchTargetIndex;
							flag = false;
						} else {
							if (updateConflictFlag == false && branchTargetIndex != finalTargetIndex) {
								updateConflictFlag = true;
							}
						}
					}
				}
			}

			if (updateConflictFlag == true) {
				// ????????????????????????
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				conflict.getTuples().addAll(updateMayConflict);
				conflict.setInformation("??????????????????");
				conflictList.add(conflict);
				tupleList.addAll(updateMayConflict);
			}

			if (deleteMayConflict.size() > 0 && updateMayConflict.size() > 0) {
				// ??????????????-??????????????????
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				deleteMayConflict.addAll(updateMayConflict); // ??????
				conflict.getTuples().addAll(deleteMayConflict);
				conflict.setInformation("????????-??????????????????");
				conflictList.add(conflict);
				tupleList.addAll(deleteMayConflict);
			}

			if (deleteMayConflict.size() == 0) {
				refEdge_MultiKeyMap.put(r, sourceIndex, finalTargetIndex);
			}

		}

		/** RefEdgeMulti?????????? */
		System.out.println("\n\n\n");
		MultiKeyMap<Object, List<List<EObject>>> refEdgeMulti_MultiKeyMap = new MultiKeyMap<>();
		for (Entry<RefEdgeMulti, List<RefEdgeMulti>> entry : refEdgeMultiMatchGroupMap.entrySet()) {
			RefEdgeMulti key = entry.getKey();

			EReference r = key.getType();
			// eType??????????eGenericType????????????
			if (r.isChangeable() == false || r.getName().equals("eGenericType")
					|| r.getName().equals("eGenericSuperTypes")) {
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();
			List<List<EObject>> baseTargetsIndex = key.getTargetsIndex();

			List<RefEdgeMulti> list = entry.getValue();

			List<Tuple> deleteMayConflict = new ArrayList<>();
			List<Tuple> addMayConflict = new ArrayList<>();
			List<Tuple> deleteOtherConflict = new ArrayList<>();

			List<List<EObject>> remain = new ArrayList<>(baseTargetsIndex); // remain????????baseTargetsIndex??????
			Set<List<EObject>> addition = new HashSet<>(); // ??????????????

			for (int i = 0; i < size - 1; i++) {
				RefEdgeMulti refEdgeMulti = list.get(i);
				if (refEdgeMulti == null) {
					// ??????????????????????????????
					Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
					tuple.setBranch(i + 1);
					deleteMayConflict.add(tuple);

				} else { // ??????????
					List<List<EObject>> refEdgeMultiTargetsIndex = refEdgeMulti.getTargetsIndex();
					List<List<EObject>> branchTargetsIndex = new ArrayList<>(refEdgeMultiTargetsIndex); // ??????????????????
					// ????????????????????????????????
					remain.retainAll(branchTargetsIndex);
					// ??????????????????????????????
					branchTargetsIndex.removeAll(baseTargetsIndex);
					addition.addAll(branchTargetsIndex);
					// ??????????????????
					// ??????????????>0??????branchTargetsIndex.get(0)==null????????
					if (branchTargetsIndex.size() > 0 && branchTargetsIndex.get(0) != null) {
//						List<EObject> tmp = new ArrayList<>();
						List<Integer> locations = new ArrayList<>();

						for (List<EObject> index : branchTargetsIndex) {
							if (nodesReverseIndexMap.get(index) == null) {
								for (int j = 0; j < index.size(); j++) {
									if (index.get(j) == null) {
										Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
										tuple.setBranch(j + 1);
										deleteOtherConflict.add(tuple);
									}
								}
							}
//							// ????????????????????
//							EObject eObject1 = index.get(i);
//							tmp.add(eObject1);

							int location = refEdgeMultiTargetsIndex.indexOf(index);
							locations.add(location);

						}

						// ????????????1
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						tuple.setEObject(sourceIndex.get(i));
						tuple.setEStructuralFeature(r);
						tuple.getPosition().addAll(locations);
						addMayConflict.add(tuple);
					}
				}
			}
			// remain??addition??????????????remain????????????
			remain.addAll(addition);

			if (deleteOtherConflict.size() > 0) { // ????????????????addMayConflict>0
				// ??????????????????-??????????????????????
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				deleteOtherConflict.addAll(addMayConflict); // ??????
				conflict.getTuples().addAll(deleteOtherConflict);
				conflict.setInformation("????????????-??????????????????????");
				conflictList.add(conflict);
				tupleList.addAll(deleteOtherConflict);
			}

			if (deleteMayConflict.size() > 0 && addMayConflict.size() > 0) {
				// ??????????????????-??????????????????????
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				deleteMayConflict.addAll(addMayConflict); // ??????????????????????????????????addMayConflict
				conflict.getTuples().addAll(deleteMayConflict);
				conflict.setInformation("????????????-??????????????????????");
				conflictList.add(conflict);
				tupleList.addAll(deleteMayConflict);
			}

			if (deleteMayConflict.size() == 0) {
				refEdgeMulti_MultiKeyMap.put(r, sourceIndex, remain);
			}
		}

		/** RefEdgeAdd?????????? */
		System.out.println("\n\n\n");
		for (Entry<RefEdge, List<RefEdge>> entry : refEdgeAddMatchGroupMap.entrySet()) {
			RefEdge key = entry.getKey();

			EReference r = key.getType();
			// eType??????????eGenericType????????????
			if (r.isChangeable() == false || r.getName().equals("eGenericType")
					|| r.getName().equals("eGenericSuperTypes")) {
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();

			List<RefEdge> list = entry.getValue();
			List<EObject> finalTargetIndex = null;

			boolean flag = true;
			boolean addConflictFlag = false;
			List<Tuple> addMayConflict = new ArrayList<>();

			for (int i = 0; i < size - 1; i++) {
				RefEdge refEdge = list.get(i);
				if (refEdge != null) {
					List<EObject> addTargetIndex = refEdge.getTargetIndex();
					// ????????????1
					if (addTargetIndex != null) {
						Tuple tuple = ConflictFactory.eINSTANCE.createTuple();
						tuple.setBranch(i + 1);
						tuple.setEObject(sourceIndex.get(i));
						tuple.setEStructuralFeature(r);
						// ??????????position??null??~
						addMayConflict.add(tuple);
					}

					if (flag == true) {
						finalTargetIndex = addTargetIndex;
						flag = false;
					} else {
						if (addConflictFlag == false && addTargetIndex != finalTargetIndex) {
							addConflictFlag = true;
						}
					}
				}
			}

			if (addConflictFlag == true) {
				// ??????????????????????????
				Conflict conflict = ConflictFactory.eINSTANCE.createConflict();
				conflict.getTuples().addAll(addMayConflict);
				conflict.setInformation("????????????????????");
				conflictList.add(conflict);
				tupleList.addAll(addMayConflict);
			}

			refEdge_MultiKeyMap.put(r, sourceIndex, finalTargetIndex);
		}

		/** RefEdgeMultiAdd?????????? */
		System.out.println("\n\n\n");
		for (Entry<RefEdgeMulti, List<RefEdgeMulti>> entry : refEdgeMultiAddMatchGroupMap.entrySet()) {
			RefEdgeMulti key = entry.getKey();

			EReference r = key.getType();
			// eType??????????eGenericType????????????
			if (r.isChangeable() == false || r.getName().equals("eGenericType")
					|| r.getName().equals("eGenericSuperTypes")) {
				continue;
			}

			List<RefEdgeMulti> list = entry.getValue();
			List<List<EObject>> remain = new ArrayList<>();
			Set<List<EObject>> addition = null;
			boolean flag = true;

			for (int i = 0; i < size - 1; i++) {
				RefEdgeMulti refEdgeMulti = list.get(i);
				if (refEdgeMulti != null) {
					List<List<EObject>> addTargetsIndex = refEdgeMulti.getTargetsIndex();
					if (flag == true) {
						addition = new HashSet<>(addTargetsIndex); // ??????????????
						flag = false;
					} else {
						// ??????
						addition.addAll(addTargetsIndex);
					}
				}
			}

			// ??????????????????????????????????????????????????????????????????????????????????
			// ??????????????????????????

			remain.addAll(addition); // ????????????List
			refEdgeMulti_MultiKeyMap.put(r, key.getSourceIndex(), remain);
		}
				
		/** ???????????????????????? */
		System.out.println("\n\n\n**********************????????????????????????*************************");
		
		System.out.println("------------------------????????-----------------------------");
		
		// ????????????????????????????????????????
		// ????????????????????????????????????????????????????????
		for (EObject e : m1ResourceEObjects) {
						
			List<EObject> sourceIndex = nodesIndexMap.get(e);
			EClass eClass = e.eClass();
			System.out.println("\n\n\nsourceIndex: " + sourceIndex);
			
			for (EAttribute a : eClass.getEAllAttributes()) {
								
				// ??????????????????????????????????????????????
				if(needOrderSet.contains(a) == true) {
					continue;
				}
				
				if(a.isChangeable() == false || a.getName().equals("isComposite")) {
					continue;
				}
				
				if (a.isMany() == false) { // ????????
					System.out.println("????????: " + a);
					Object eSetTarget = valEdge_MultiKeyMap.get(a, sourceIndex);
					System.out.println("target: " + eSetTarget);
					e.eSet(a, eSetTarget);
					
				} else { // ????????
					System.out.println("??????????" + a);
					List<Object> eSetTargets = (List<Object>) valEdgeMulti_MultiKeyMap.get(a, sourceIndex);

					e.eSet(a, eSetTargets);

				}
			}

			// ????????????????????????????target??????????????????????????????????
			// ??????????????????????????????????????????????
			
		}
		
		
		// ??????????????????????
		for (EObject e : m1ResourceEObjects) {
			List<EObject> sourceIndex = nodesIndexMap.get(e);
			EClass eClass = e.eClass();
			System.out.println("\n\n\nsourceIndex: " + sourceIndex);
			
			for (EAttribute a : eClass.getEAllAttributes()) {
					
				if (a.isMany() == false) {
					continue;
				} 
				
				// ??????????????????????
				if(needOrderSet.contains(a.getName()) == false) {
					continue;
				}
				
				if(a.isChangeable() == false || a.getName().equals("isComposite")) {
					continue;
				}
																					
				System.out.println("??????????" + a);
				List<Object> eSetTargets = (List<Object>) valEdgeMulti_MultiKeyMap.get(a, sourceIndex);

				if (eSetTargets.size() <= 1 ) {
					e.eSet(a, eSetTargets);

				} else {
					List<Object> finalList = new ArrayList<>();

					// ??????????e??????????????
					ValEdgeMulti valEdgeMulti = valEdgeMultiHelper.get(a, sourceIndex);
					ValEdgeMulti valEdgeMultiAdd = null;
					Map<Object, Integer> baseFlag = new HashMap<>();
					MultiKeyMap<Object, Integer> branchFlag = new MultiKeyMap<>();

					if (valEdgeMulti != null) {
						for (int i = 0; i < valEdgeMulti.getTargets().size(); i++) {
							Object obj = valEdgeMulti.getTargets().get(i);
							baseFlag.put(obj, i);
						}

						List<ValEdgeMulti> list = valEdgeMultiMatchGroupMap.get(valEdgeMulti);
						for (int i = 0; i < size - 1; i++) {
							ValEdgeMulti valEdgeMulti2 = list.get(i); // ??????????null??
							for (int j = 0; j < valEdgeMulti2.getTargets().size(); j++) {
								Object obj = valEdgeMulti2.getTargets().get(j);
								branchFlag.put(i, obj, j);
							}
						}

					} else {
						valEdgeMultiAdd = valEdgeMultiAddHelper.get(a, sourceIndex);
						List<ValEdgeMulti> list = valEdgeMultiAddMatchGroupMap.get(valEdgeMultiAdd);
						for (int i = 0; i < size - 1; i++) {
							ValEdgeMulti valEdgeMulti2 = list.get(i); // ??????null
							if (valEdgeMulti2 != null) {
								for (int j = 0; j < valEdgeMulti2.getTargets().size(); j++) {
									Object obj = valEdgeMulti2.getTargets().get(j);
									branchFlag.put(i, obj, j);
								}
							}
						}

					}

					int nodeSize = eSetTargets.size();
					TopoGraph g = new TopoGraph(nodeSize);

					for (int i = 0; i < nodeSize - 1; i++) {
						Object x = eSetTargets.get(i);
						for (int j = i + 1; j < nodeSize; j++) {
							Object y = eSetTargets.get(j);
							Order order = Order.unkown;
							if (valEdgeMulti != null) {
								order = computeOrd(baseFlag, branchFlag, x, y);
							} else if (valEdgeMultiAdd != null) {
								order = computeOrd(null, branchFlag, x, y);
							}
							if (order == order.less) {
								g.addEdge(i, j);
							} else if (order == order.greater) {
								g.addEdge(j, i);
							}
						}
					}

					// ????????????conflicts
					try {
						List<Integer> topologicalSort = g.topologicalSort();
						for (Integer i : topologicalSort) {
							finalList.add(eSetTargets.get(i));
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					// ??????eSet
					e.eSet(a, finalList);
					
				}
			}

			// ????????????????????????????target??????????????????????????????????
			// ??????????????????????????????????????????????
			
		}
		
		
		
							

		System.out.println("------------------------????????-----------------------------");
		
		// ????????????????????????????????????
		// ????????????????????????????????????????????????????????
		for (EObject e : m1ResourceEObjects) {
			
			// uml
			if(e instanceof LiteralSpecificationImpl) {
				continue;
			}									
			
			// sequence
			if(e instanceof InteractionConstraintImpl) {
				continue;
			}
												
			List<EObject> sourceIndex = nodesIndexMap.get(e);
			EClass eClass = e.eClass();
			System.out.println("\n\n\nsourceIndex: " + sourceIndex);
								
			for (EReference r : eClass.getEAllReferences()) {
				
				// ??????????????????????????????????????????????
				if(needOrderSet.contains(r) == true) {
					continue;
				}
																
				// eType??????????eGenericType????????????
				if (r.isChangeable() == false || r.getName().equals("eGenericType")
						|| r.getName().equals("eGenericSuperTypes")) {
					continue;
				}		
																
				if (r.isMany() == false) { // ????????
															
					System.out.println("??????????" + r);
					List<EObject> targetIndex = refEdge_MultiKeyMap.get(r, sourceIndex);
					EObject node = nodesReverseIndexMap.get(targetIndex);
									
//					e.eSet(r, node);
					
					try {
						e.eSet(r, node);
					} catch (Exception e2) {
						System.out.println();
						// class diagram: opposite
					}
					
				} else { // ????????
															
					System.out.println("??????????" + r);
									
					List<List<EObject>> targetsIndex = refEdgeMulti_MultiKeyMap.get(r, sourceIndex);
					List<EObject> finalList = new ArrayList<>();
					
					for (List<EObject> nodeIndex : targetsIndex) {
						EObject node = nodesReverseIndexMap.get(nodeIndex);
						if (node != null) {
							finalList.add(node);
						}
					}
										
//					e.eSet(r, finalList);	
								
					try {
						e.eSet(r, finalList);	
					} catch (UnsupportedOperationException e2) {
						// EnumerationLiteral
					}
					
					// lyt: test
					if (e instanceof MessageOccurrenceSpecificationImpl) {
						if (r.getName().equals("toAfter")) {
							List<GeneralOrderingImpl> list = (List<GeneralOrderingImpl>) e.eGet(r);
							if(list!=null && list.size()>0) {
								for(GeneralOrderingImpl g1 : list) {
									if(g1.getName()!=null) {
										System.out.println(g1);
									}
								}
							}
						}
					}
					
					
				}				
			}
		}
		
		
		// ??????????????????????
		for (EObject e : m1ResourceEObjects) {
									
			// uml
			if(e instanceof LiteralSpecificationImpl) {
				continue;
			}				
			
			// sequence
			if(e instanceof InteractionConstraintImpl) {
				continue;
			}
						
			List<EObject> sourceIndex = nodesIndexMap.get(e);
			EClass eClass = e.eClass();
			System.out.println("\n\n\nsourceIndex: " + sourceIndex);
							
			for (EReference r : eClass.getEAllReferences()) {
																								
				if(r.isMany() == false) {
					continue;
				}
									
				// ??????????????????????
				if(needOrderSet.contains(r.getName()) == false) {
					continue;
				}
				
				// eType??????????eGenericType????????????
				if (r.isChangeable() == false || r.getName().equals("eGenericType")
						|| r.getName().equals("eGenericSuperTypes")) {
					continue;
				}		
											
				System.out.println("??????????" + r);

				List<List<EObject>> targetsIndex = refEdgeMulti_MultiKeyMap.get(r, sourceIndex);							
				List<EObject> finalList = new ArrayList<>();
				
				if(targetsIndex.size() <= 1) {
					for (List<EObject> nodeIndex : targetsIndex) {
						EObject node = nodesReverseIndexMap.get(nodeIndex);
						if (node != null) {
							finalList.add(node);
						}
					}
				} else {
					// initialize
					List<List<EObject>> mergeIndex = new ArrayList<>();

					// ??????????e??????????????
					RefEdgeMulti refEdgeMulti = refEdgeMultiHelper.get(r, sourceIndex);
					RefEdgeMulti refEdgeMultiAdd = null;
					Map<Object, Integer> baseFlag = new HashMap<>();
					MultiKeyMap<Object, Integer> branchFlag = new MultiKeyMap<>();

					if (refEdgeMulti != null) {
						for (int i = 0; i < refEdgeMulti.getTargetsIndex().size(); i++) {
							List<EObject> index = refEdgeMulti.getTargetsIndex().get(i);
							baseFlag.put(index, i);
						}

						List<RefEdgeMulti> list = refEdgeMultiMatchGroupMap.get(refEdgeMulti);
						for (int i = 0; i < size - 1; i++) {
							RefEdgeMulti refEdgeMulti2 = list.get(i); // ??????????null??
							for (int j = 0; j < refEdgeMulti2.getTargetsIndex().size(); j++) {
								List<EObject> index = refEdgeMulti2.getTargetsIndex().get(j);
								branchFlag.put(i, index, j);
							}
						}

					} else {
						refEdgeMultiAdd = refEdgeMultiAddHelper.get(r, sourceIndex);
						List<RefEdgeMulti> list = refEdgeMultiAddMatchGroupMap.get(refEdgeMultiAdd);
						for (int i = 0; i < size - 1; i++) {
							RefEdgeMulti refEdgeMulti2 = list.get(i); // ????????????????????null
							if (refEdgeMulti2 != null) {
								for (int j = 0; j < refEdgeMulti2.getTargetsIndex().size(); j++) {
									List<EObject> index = refEdgeMulti2.getTargetsIndex().get(j);
									branchFlag.put(i, index, j);
								}
							}
						}
					}

					int nodeSize = targetsIndex.size();
					TopoGraph g = new TopoGraph(nodeSize);

					for (int i = 0; i < nodeSize - 1; i++) {
						List<EObject> xIndex = targetsIndex.get(i);
						for (int j = i + 1; j < nodeSize; j++) {
							List<EObject> yIndex = targetsIndex.get(j);
							Order order = Order.unkown;
							if (refEdgeMulti != null) {
								order = computeOrd(baseFlag, branchFlag, xIndex, yIndex);
							} else if (refEdgeMultiAdd != null) {
								order = computeOrd(null, branchFlag, xIndex, yIndex);
							}
							if (order == order.less) {
								g.addEdge(i, j);
							} else if (order == order.greater) {
								g.addEdge(j, i);
							}
						}
					}
					
					// ??????????conflicts
					try {
						List<Integer> topologicalSort = g.topologicalSort();
						for (Integer i : topologicalSort) {
							mergeIndex.add(targetsIndex.get(i));
						}
						for (List<EObject> nodeIndex : mergeIndex) {
							EObject node = nodesReverseIndexMap.get(nodeIndex);
							finalList.add(node);
						}
													
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				

				// ??????eSet
				e.eSet(r, finalList);
										
				// lyt: test
				if(e instanceof BehaviorExecutionSpecificationImpl) {
					if (r.getName().equals("generalOrdering")) {
						List<GeneralOrderingImpl> list = (List<GeneralOrderingImpl>) e.eGet(r);
						if(list.size()>0) {
							for(GeneralOrderingImpl gen : list) {
								if(gen.getName()!=null && gen.getName().equals("WIYHKF")) {
									System.out.println(e);
									System.out.println(r.getName());
									System.out.println(list);
									System.out.println(gen);
									System.out.println();
								}
							}
						}
					}					
				}
																	
			}
		
		}
		
		System.out.println("\n\n??????" + (System.currentTimeMillis() - currentTime) + "ms");

		System.out.println("\n\n\n*****************resourceNodes************************");
		m1ResourceEObjects.forEach(e -> {
			// lyt: test
			
		});
							
		/** ?????????? */
		List<EObject> roots = m1ResourceEObjects.stream().filter(n -> n.eContainer() == null)
				.collect(Collectors.toList());
		m1Resource.getContents().addAll(roots);
		try {
			m1Resource.save(null);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("????????????????");
		
		
		// ????????????????????????
		if(conflictList.size()>0) {
			// ??????						
			Conflicts conflicts = ConflictFactory.eINSTANCE.createConflicts();
			conflicts.getConflicts().addAll(conflictList);
			conflicts.setBaseURI(baseResource.getURI().toString());

			List<String> branchURIString = new ArrayList<>();
			Set<Integer> set = new HashSet<>();
			tupleList.forEach(t -> {
				set.add(t.getBranch());
			});
			TreeSet<Integer> treeSet = new TreeSet<>(set);
			treeSet.forEach(t -> {
				branchURIString.add(resourceList.get(t).getURI().toString());
			});

			conflicts.getBranchURIs().addAll(branchURIString);

			// ????????????
			conflictResourceEObjects.add(conflicts);
			conflictResourceEObjects.addAll(conflictList);
			conflictResourceEObjects.addAll(tupleList);

			List<EObject> roots2 = conflictResourceEObjects.stream().filter(n -> n.eContainer() == null)
					.collect(Collectors.toList());
			conflictResource.getContents().addAll(roots2);

			try {
				conflictResource.save(null);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("\n\n\n????????????????");
		}
		

		System.out.println("done");

	}

	/**
	 * ????xIndex??yIndex????????
	 */
	private static Order computeOrd(Map<Object, Integer> baseFlag, MultiKeyMap<Object, Integer> branchFlag,
			Object xIndex, Object yIndex) {

		List<Tuple2<Force, Order>> ord_k = new ArrayList<>();
		Order o_b = Order.unkown;
		Integer xPositionBase = -1;
		Integer yPositionBase = -1;

		if (baseFlag != null) {
			xPositionBase = baseFlag.get(xIndex);
			yPositionBase = baseFlag.get(yIndex);
			if (xPositionBase != null && yPositionBase != null) {
				Integer resultBase = xPositionBase - yPositionBase;
				if (resultBase < 0) {
					o_b = Order.less;
				} else {
					o_b = Order.greater;
				}
			}
		}

		for (int i = 0; i < size - 1; i++) {
			Force t = Force.soft;
			Order o = Order.unkown;

			Integer xPositionBranch = branchFlag.get(i, xIndex);
			Integer yPositionBranch = branchFlag.get(i, yIndex);
			// ????xIndex??yIndex????????????????
			if (xPositionBranch != null && yPositionBranch != null) {
				Order o_k = Order.unkown;
				Integer result = xPositionBranch - yPositionBranch;
				if (result < 0) {
					o_k = Order.less;
				} else {
					o_k = Order.greater;
				}
				// ????xIndex??yIndex??????????????
				if (xPositionBase != null && yPositionBase != null) {
					o = o_b;
				}

				if (o_k != o) { // o_k??????o????????????????
					t = Force.hard;
					o = o_k;
				}
			}

			ord_k.add(new Tuple2<Force, Order>(t, o));

		}

		// ??????????????
		Tuple2<Force, Order> t1 = ord_k.get(0);
		Tuple2<Force, Order> t2;
		for (int p = 1; p < ord_k.size(); p++) {
			t2 = ord_k.get(p);
			try {
				t1 = mergeOrd(t1, t2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return t1.second;
	}

	public static Tuple2<Force, Order> mergeOrd(Tuple2<Force, Order> c1, Tuple2<Force, Order> c2) throws Exception {

		if (c1.first == Force.hard && c2.first == Force.soft) {
			return new Tuple2<Force, Order>(Force.hard, c1.second);
		} else if (c1.first == Force.soft && c2.first == Force.hard) {
			return new Tuple2<Force, Order>(Force.hard, c2.second);
		} else if (c1.first == Force.hard && c2.first == Force.hard) {
			if (c1.second == c2.second) {
				return new Tuple2<Force, Order>(Force.hard, c1.second);
			} else {
				throw new Exception("###conflict: ????Force.hard??????????\n");
			}
		} else if (c1.first == Force.soft && c2.first == Force.soft) {
			if (c1.second == Order.unkown && c2.second == Order.unkown) {
				return new Tuple2<Force, Order>(Force.soft, c1.second);
			} else if (c1.second == Order.unkown && c2.second != Order.unkown) {
				return new Tuple2<Force, Order>(Force.soft, c2.second);
			} else if (c1.second != Order.unkown && c2.second == Order.unkown) {
				return new Tuple2<Force, Order>(Force.soft, c1.second);
			} else if (c1.second != Order.unkown && c2.second != Order.unkown) {
				if (c1.second == c2.second) {
					return new Tuple2<Force, Order>(Force.soft, c1.second);
				} else {
					throw new Exception("###conflict: ????Force.soft??????????\n");
				}
			}
		}
		return null;
	}

	/**
	 * ????????????
	 */
	private static EClass computeSubType(EClass leftEClass, EClass rightEClass) {
		if (leftEClass == rightEClass) {
			return leftEClass;
		}
		if (leftEClass.isSuperTypeOf(rightEClass)) {
			return rightEClass;
		}
		if (rightEClass.isSuperTypeOf(leftEClass)) {
			return leftEClass;
		}
		return null;
	}

	/**
	 * ??????????????container??????????container????????????????????
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

enum Force {
	soft, hard
}

enum Order {
	less, greater, unkown
}