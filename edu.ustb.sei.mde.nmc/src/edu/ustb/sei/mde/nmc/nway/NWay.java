package edu.ustb.sei.mde.nmc.nway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

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
			Set<String> needOrderSet, Resource m1Resource) {
		size = resourceList.size();
		Comparison comparison = null;
		IComparisonScope scope = null;
		Resource baseResource = resourceList.get(0);
		Resource branchResource = null;

		/**
		 * 将原始模型与各个分支模型进行二向匹配 匹配上同一个原始元素的，划分到相同的匹配组
		 */
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
					list.add(branchEObject); // 例如<eb, {e1, null, e3, ..., en}>

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
//		System.out.println("\n\n\n------------------------matchGroupMap------------------------------- ");
//		nodeMatchGroupMap.forEach((key, value) -> {
//			System.out.println("key: " + key);
//			value.forEach(e -> {
//				System.out.println(e);
//			});
//			System.out.println();
//		});
//		System.out.println("\n\n\n");

		/** 预匹配信息：将分支i，分支j作为键 */
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

		/** 对于新加元素，两两比较分支模型 */
		Set<EObject> vertices = new HashSet<>();
		Set<Match> edges = new HashSet<>();
		MultiKeyMap<EObject, Double> distanceMap = new MultiKeyMap<>(); // 还要记录编辑距离
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

		/** 成团、排序、选择 */
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

		// 新加节点的匹配组
		Map<EObject, List<EObject>> nodeAddMatchGroupMap = new HashMap<>();
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
						cSize = 0; // 当团只有一个元素时，让cSize为0，这样选择它的优先级最低
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

			// 按照value进行排序（降序），取第一个作为匹配组
			Optional<Entry<List<EObject>, Double>> findFirst = map.entrySet().stream()
					.sorted(Map.Entry.<List<EObject>, Double>comparingByValue().reversed()).findFirst();
			List<EObject> matchGroup = findFirst.get().getKey();
			maximalCliques.remove(matchGroup);

			List<EObject> nodeAddMatchGroup = new ArrayList<>(Collections.nCopies(size - 1, null));
			matchGroup.forEach(e -> {
				Integer index = resourceMap.get(e.eResource());
				nodeAddMatchGroup.set(index, e); // 其它位置为null，例如{e1, null, null, ..., en}
				// 更新nodesIndexMap，need to improve?
				nodesIndexMap.put(e, nodeAddMatchGroup);
			});
			// 把分支新加的第一个作为key了，方便之后的边计算
			nodeAddMatchGroupMap.put(matchGroup.get(0), nodeAddMatchGroup);

			// tmp
//			System.out.println("**************挑选的团***************");
//			matchGroup.forEach(e -> {
//				System.out.println(e);
//			});
//			System.out.println("\n\n\n\n\n");

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
//		System.out.println("\n\n\n----------------------nodeAddMatchGroupMap: ");
//		nodeAddMatchGroupMap.values().forEach(list -> {
//			list.forEach(e -> {
//				System.out.println(e);
//			});
//			System.out.println();
//		});
//		System.out.println("\n\n\n");

		/** 得到边的匹配组 */
		// 针对非新加点的边
		Map<ValEdge, List<ValEdge>> valEdgeMatchGroupMap = new HashMap<>();
		Map<ValEdgeMulti, List<ValEdgeMulti>> valEdgeMultiMatchGroupMap = new HashMap<>();

		Map<RefEdge, List<RefEdge>> refEdgeMatchGroupMap = new HashMap<>();
		Map<RefEdgeMulti, List<RefEdgeMulti>> refEdgeMultiMatchGroupMap = new HashMap<>();

		// 方便序的计算
		MultiKeyMap<Object, ValEdgeMulti> valEdgeMultiHelper = new MultiKeyMap<>();
		MultiKeyMap<Object, RefEdgeMulti> refEdgeMultiHelper = new MultiKeyMap<>();

		nodeMatchGroupMap.forEach((baseEObject, list) -> {

			System.out.println("\n\n***************************baseEObject***************************");
			System.out.println("baseEObject: " + baseEObject);
			List<EObject> sourceIndex = nodesIndexMap.get(baseEObject); // e.sourceIndex;

			EClass eClass = baseEObject.eClass();

			for (EAttribute a : eClass.getEAllAttributes()) {

				if (a.isChangeable() == false) {
					System.out.println("不能改变的属性：" + a);
					continue;
				}

				if (a.isMany() == false) { // 单值属性
					Object target = baseEObject.eGet(a);
					ValEdge valEdge = new ValEdge(a, sourceIndex, target); // target maybe unset
					List<ValEdge> create = new ArrayList<>(Collections.nCopies(size - 1, null));
					valEdgeMatchGroupMap.put(valEdge, create);

				} else { // 多值属性
					List<Object> targets = (List<Object>) baseEObject.eGet(a); // targets maybe unset
					ValEdgeMulti valEdgeMulti = new ValEdgeMulti(a, sourceIndex, targets);
					List<ValEdgeMulti> create = new ArrayList<>(Collections.nCopies(size - 1, null));
					valEdgeMultiMatchGroupMap.put(valEdgeMulti, create);

					// 方便序的计算，保存了原始版本的
					if (needOrderSet.contains(a.getName())) {
						valEdgeMultiHelper.put(a, sourceIndex, valEdgeMulti);
					}

				}
			}

			System.out.println("-----------------------------------------------------------------------");
			for (EReference r : eClass.getEAllReferences()) {

				if (r.isChangeable() == false) {
					System.out.println("不能改变的引用：" + r);
					continue;
				}

				if (r.isMany() == false) { // 单值引用
					EObject value = (EObject) baseEObject.eGet(r);
					List<EObject> targetIndex = nodesIndexMap.get(value);
					RefEdge refEdge = new RefEdge(r, sourceIndex, targetIndex); // targetIndex maybe unset
					List<RefEdge> create = new ArrayList<>(Collections.nCopies(size - 1, null));
					refEdgeMatchGroupMap.put(refEdge, create);

				} else { // 多值引用
					List<EObject> values = (List<EObject>) baseEObject.eGet(r);
					List<List<EObject>> targetsIndex = new ArrayList<>();
					values.forEach(eObj -> {
						targetsIndex.add(nodesIndexMap.get(eObj));
					});

					RefEdgeMulti refEdgeMulti = new RefEdgeMulti(r, sourceIndex, targetsIndex); // targetsIndex may
																								// unset
					List<RefEdgeMulti> create = new ArrayList<>(Collections.nCopies(size - 1, null));
					refEdgeMultiMatchGroupMap.put(refEdgeMulti, create);

					// 方便序的计算，保存了原始版本的
					if (needOrderSet.contains(r.getName())) {
						refEdgeMultiHelper.put(r, sourceIndex, refEdgeMulti);
					}
				}
			}

			for (int i = 0; i < size - 1; i++) {
				EObject branchEObject = list.get(i);
				System.out.println("\n\n------------------遍历分支list--------------------");
				if (branchEObject != null) {
					// 因为在同一个matchGroup，sourceIndex相同
					System.out.println("branchEObject: " + branchEObject);

					EClass eClass2 = branchEObject.eClass(); // PENDING: may not the same as eClass.

					for (EAttribute a2 : eClass2.getEAllAttributes()) {

						if (a2.isChangeable() == false) {
							System.out.println("不能改变的属性：" + a2);
							continue;
						}

						if (a2.isMany() == false) { // 单值属性
							Object target2 = branchEObject.eGet(a2);
							ValEdge valEdge2 = new ValEdge(a2, sourceIndex, target2);
							valEdgeMatchGroupMap.get(valEdge2).set(i, valEdge2); // PENDING: what if get(valEdge2) is
																					// null?

						} else { // 多值属性
							List<Object> targets2 = (List<Object>) branchEObject.eGet(a2);
							ValEdgeMulti valEdgeMulti2 = new ValEdgeMulti(a2, sourceIndex, targets2);
							valEdgeMultiMatchGroupMap.get(valEdgeMulti2).set(i, valEdgeMulti2);

						}
					}

					System.out.println("-----------------------------------------------------------------------");
					for (EReference r2 : eClass2.getEAllReferences()) {

						if (r2.isChangeable() == false) {
							System.out.println("不能改变的引用：" + r2);
							continue;
						}

						if (r2.isMany() == false) { // 单值引用
							EObject value2 = (EObject) branchEObject.eGet(r2);
							List<EObject> targetIndex2 = nodesIndexMap.get(value2);

							RefEdge refEdge2 = new RefEdge(r2, sourceIndex, targetIndex2);
							refEdgeMatchGroupMap.get(refEdge2).set(i, refEdge2);

						} else { // 多值引用
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

		// 针对新加点的边
		Map<ValEdge, List<ValEdge>> valEdgeAddMatchGroupMap = new HashMap<>();
		Map<ValEdgeMulti, List<ValEdgeMulti>> valEdgeMultiAddMatchGroupMap = new HashMap<>();

		Map<RefEdge, List<RefEdge>> refEdgeAddMatchGroupMap = new HashMap<>();
		Map<RefEdgeMulti, List<RefEdgeMulti>> refEdgeMultiAddMatchGroupMap = new HashMap<>();

		// 方便序的计算
		MultiKeyMap<Object, ValEdgeMulti> valEdgeMultiAddHelper = new MultiKeyMap<>();
		MultiKeyMap<Object, RefEdgeMulti> refEdgeMultiAddHelper = new MultiKeyMap<>();

		for (List<EObject> list : nodeAddMatchGroupMap.values()) {
			for (int i = 0; i < size - 1; i++) {
				EObject addEObject = list.get(i);
				if (addEObject != null) {
					EClass eClass = addEObject.eClass();
					List<EObject> sourceIndex = nodesIndexMap.get(addEObject);

					for (EAttribute a : eClass.getEAllAttributes()) {

						if (a.isChangeable() == false) {
							System.out.println("不能改变的属性：" + a);
							continue;
						}

						if (a.isMany() == false) { // 单值属性
							Object target = addEObject.eGet(a);
							ValEdge valEdge = new ValEdge(a, sourceIndex, target);
							List<ValEdge> exist = valEdgeAddMatchGroupMap.get(valEdge);
							if (exist == null) {
								List<ValEdge> create = new ArrayList<>(Collections.nCopies(size - 1, null));
								create.set(i, valEdge);
								valEdgeAddMatchGroupMap.put(valEdge, create); // 还是把新加匹配组中的第一个看作base的地位，但之后合并不用看base
							} else {
								exist.set(i, valEdge);
							}

						} else { // 多值属性
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

							// 方便序的计算，保存第一个分支的
							if (needOrderSet.contains(a.getName())) {
								valEdgeMultiAddHelper.put(a, sourceIndex, valEdgeMulti);
							}
						}
					}

					for (EReference r : eClass.getEAllReferences()) {

						if (r.isChangeable() == false) {
							System.out.println("不能改变的引用：" + r);
							continue;
						}

						if (r.isMany() == false) { // 单值引用
							EObject value = (EObject) addEObject.eGet(r);
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

						} else { // 多值引用
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

							// 方便序的计算，保存第一个分支的
							if (needOrderSet.contains(r.getName())) {
								refEdgeMultiAddHelper.put(r, sourceIndex, refEdgeMulti);
							}

						}
					}
				}
			}
		}

		/** 先计算点的合并结果 */
		List<NodeConflict> nodeConflicts = new ArrayList<>();
		Map<List<EObject>, EObject> nodesReverseIndexMap = new HashMap<>();
		List<EObject> resourceNodes = new ArrayList<>(); // 一开始不加到m1Resource里面

		// 针对非新加节点
		nodeMatchGroupMap.forEach((baseEObject, list) -> {
			// 先计算点的合并类型
			EClass baseEClass = baseEObject.eClass();
			EClass finalEClass = baseEClass;
			boolean flag = true;
			// 记录可能产生的冲突
			List<Tuple2<Integer, EObject>> deleteMayConflict = new ArrayList<>();
			List<Tuple2<Integer, EObject>> updateMayConflict = new ArrayList<>();
			for (int i = 0; i < size - 1; i++) {
				EObject branchEObject = list.get(i);
				if (branchEObject == null) {
					// 打印出的分支从下标要加1
					Tuple2<Integer, EObject> tuple = new Tuple2<>(i + 1, null);
					deleteMayConflict.add(tuple);
				} else {
					EClass branchEClass = branchEObject.eClass();
					if (branchEClass != baseEClass) { // EClass用地址比较
						// 打印出的分支从下标要加1
						Tuple2<Integer, EObject> tuple = new Tuple2<>(i + 1, branchEObject);
						updateMayConflict.add(tuple);

						if (flag == true) {
							finalEClass = branchEClass;
							flag = true;
						} else {
							finalEClass = computeSubType(finalEClass, branchEClass);
							if (finalEClass == null) {
								// 冲突：点的类型修改不兼容
								int len = updateMayConflict.size();
								NodeConflict conflict = new NodeConflict(updateMayConflict.subList(0, len - 1),
										ConflictKind.Update, updateMayConflict.subList(len - 1, len),
										ConflictKind.Update, "点的类型修改不兼容");
								System.err.println("*****" + conflict.toString());
								nodeConflicts.add(conflict);
								break;
							}
						}
					}
				}
			}

			if (deleteMayConflict.size() > 0 && updateMayConflict.size() > 0) { // 应该还需要后者的条件
				// 冲突：删除了点-修改了点的类型
				NodeConflict conflict = new NodeConflict(deleteMayConflict, ConflictKind.Delete, updateMayConflict,
						ConflictKind.Update, "删除了点-修改了点的类型");
				System.err.println("*****" + conflict.toString());
				nodeConflicts.add(conflict);
			}

			// 省略nodeConflicts.size() == 0的判断条件
			if (deleteMayConflict.size() == 0) {
				EObject create = EcoreUtil.create(finalEClass);
				// 更新nodesIndexMap
				List<EObject> sourceIndex = nodesIndexMap.get(baseEObject);
				nodesIndexMap.put(create, sourceIndex);
				// 更新nodexReverseIndexMap
				nodesReverseIndexMap.put(sourceIndex, create);
				// 加到resourceNodes
				resourceNodes.add(create);
			}

		});

		// 针对新加节点
		int conflictsSize = nodeConflicts.size();
		List<Tuple2<Integer, EObject>> typeMayConflict = new ArrayList<>();

		for (Entry<EObject, List<EObject>> entry : nodeAddMatchGroupMap.entrySet()) {
			EObject key = entry.getKey();
			List<EObject> list = entry.getValue();

			EClass finalType = null;
			boolean flag = true;
			for (int i = 0; i < size - 1; i++) {
				EObject addEObject = list.get(i);
				if (addEObject != null) {
					// 打印出的分支下标要加1
					Tuple2<Integer, EObject> tuple = new Tuple2<>(i + 1, addEObject);
					typeMayConflict.add(tuple);

					EClass addEClass = addEObject.eClass();
					if (flag == true) {
						finalType = addEClass;
						flag = false;
					} else {
						finalType = computeSubType(finalType, addEClass);
						if (finalType == null) {
							// 冲突：新加点的类型不兼容，就是当前分支和之前的分支吧
							int len = typeMayConflict.size();
							NodeConflict conflict = new NodeConflict(typeMayConflict.subList(0, len - 1),
									ConflictKind.Add, typeMayConflict.subList(len - 1, len), ConflictKind.Add,
									"新加点的类型不兼容");
							System.err.println("*****" + conflict.toString());
							nodeConflicts.add(conflict);
							break;
						}
					}
				}
			}

			// 这里感觉可以写此判断条件，如果不兼容则不加到结果图中~
			if (conflictsSize == nodeConflicts.size()) {
				EObject create = EcoreUtil.create(finalType);
				// 更新nodesIndexMap
				List<EObject> sourceIndex = nodesIndexMap.get(key);
				nodesIndexMap.put(create, sourceIndex);
				// 更新nodesReverseIndexMap
				nodesReverseIndexMap.put(sourceIndex, create);
				// 加到resourceNodes
				resourceNodes.add(create);
			}

		}

		/** ValEdge的合并结果 **/
		System.out.println("\n\n\n");
		List<ValEdgeConflict> valEdgeConflicts = new ArrayList<>();
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
			List<Tuple4<Integer, EObject, EAttribute, Object>> deleteMayConflict = new ArrayList<>();
			List<Tuple4<Integer, EObject, EAttribute, Object>> valueUpdateMayConflict = new ArrayList<>();
			Object finalTarget = baseTarget;
			boolean flag = true;
			for (int i = 0; i < size - 1; i++) {
				ValEdge valEdge = list.get(i);
				if (valEdge == null) {
					// 删除了点，相关的边也自动被删除
					// 打印出的分支下标要加1
					Tuple4<Integer, EObject, EAttribute, Object> tuple = new Tuple4<>(i + 1, null, null, null);
					deleteMayConflict.add(tuple);

				} else {
					Object branchTarget = valEdge.getTarget();
					if (branchTarget == null && baseTarget == null) { // null和null用equals比较要报错
						continue;
					}
					if (branchTarget.equals(baseTarget) == false) { // Object用equals比较
						// 打印出的分支下标要加1
						Tuple4<Integer, EObject, EAttribute, Object> tuple = new Tuple4<>(i + 1, sourceIndex.get(i), a,
								branchTarget);
						valueUpdateMayConflict.add(tuple);

						if (flag == true) {
							finalTarget = branchTarget;
							flag = false;
						} else {
							if (branchTarget.equals(finalTarget) == false) { // Object用equals比较
								// 冲突：单值属性的修改矛盾
								int len = valueUpdateMayConflict.size();
								ValEdgeConflict conflict = new ValEdgeConflict(
										valueUpdateMayConflict.subList(0, len - 1), ConflictKind.Update,
										valueUpdateMayConflict.subList(len - 1, len), ConflictKind.Update, "单值属性的修改矛盾");
								System.err.println("*****" + conflict.toString());
								valEdgeConflicts.add(conflict);
								break;
							}
						}
					}
				}
			}

			if (deleteMayConflict.size() > 0 && valueUpdateMayConflict.size() > 0) {
				// 冲突：删除点-修改了点的单值属性
				// 难道不是重复计算？不直接根据sourceIndex检查结果图中有无对象？
				// 不是，因为要看具体哪个分支删了
				ValEdgeConflict conflict = new ValEdgeConflict(deleteMayConflict, ConflictKind.Delete,
						valueUpdateMayConflict, ConflictKind.Update, "删除了点-修改了点的单值属性");
				System.err.println("*****" + conflict.toString());
				valEdgeConflicts.add(conflict);
			}
			// 省略了conflictsSize == valEdgeConflicts.size()
			// 否则，eSet会抛出NullPointer异常
			if (deleteMayConflict.size() == 0) {
				valEdge_MultiKeyMap.put(a, sourceIndex, finalTarget);
			}

		}

		/** ValEdgeMulti的合并结果 */
		System.out.println("\n\n\n");
		List<ValEdgeMultiConflict> valEdgeMultiConflicts = new ArrayList<>();
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
			List<Tuple4<Integer, EObject, EAttribute, List<Object>>> deleteMayConflict = new ArrayList<>();
			List<Tuple4<Integer, EObject, EAttribute, List<Object>>> deleteValueMayConflict = new ArrayList<>(); // 暂时没用这个
			List<Tuple4<Integer, EObject, EAttribute, List<Object>>> addValueMayConflict = new ArrayList<>();

			List<Object> remain = new ArrayList<>(baseTargets); // remain初始化为baseTargets的拷贝
			Set<Object> addition = new HashSet<>(); // 这样就能去重了

			for (int i = 0; i < size - 1; i++) {
				ValEdgeMulti valEdgeMulti = list.get(i);
				if (valEdgeMulti == null) {
					// 删除了点，相关的边也自动被删除
					// 打印出的分支下标要加1
					Tuple4<Integer, EObject, EAttribute, List<Object>> tuple = new Tuple4<>(i + 1, null, null, null);
					deleteMayConflict.add(tuple);

				} else { // 不视为修改
					List<Object> branchTargets = new ArrayList<>(valEdgeMulti.getTargets()); // 这样不影响括号里的
					// 求交集：确定在分支中未删除的元素
					remain.retainAll(branchTargets);
					// 求差集：确定在分支中新加的元素
					branchTargets.removeAll(baseTargets);
					addition.addAll(branchTargets);
					// 记录或许发生的冲突
//					if(remain.size() < baseTargets.size() ) {		// 不对，remain是累积的
//						deleteValueMayConflict.add(i);			
//					}
					if (branchTargets.size() > 0) {
						// 打印出的分支下标要加1
						Tuple4<Integer, EObject, EAttribute, List<Object>> tuple = new Tuple4<>(i + 1,
								sourceIndex.get(i), a, branchTargets);
						addValueMayConflict.add(tuple);
					}

				}
			}
			// remain和addition求并集，最终是remain加到结果图中
			remain.addAll(addition);

			// 冲突：删除了点-删除多值属性，和deleteValueMayConflict有关

			// 冲突：删除了点-新加了点的多值属性
			if (deleteMayConflict.size() > 0 && addValueMayConflict.size() > 0) {
				ValEdgeMultiConflict conflict = new ValEdgeMultiConflict(deleteMayConflict, ConflictKind.Delete,
						addValueMayConflict, ConflictKind.Add, "删除了点-新加了点的多值属性");
				System.err.println("*****" + conflict.toString());
				valEdgeMultiConflicts.add(conflict);
			}

			// 省却了conflictsSize == valEdgeMultiConflicts.size()判断条件
			// 否则，eSet会抛出NullPointer异常
			if (deleteMayConflict.size() == 0) {
				valEdgeMulti_MultiKeyMap.put(a, sourceIndex, remain);
			}

		}

		/** ValEdgeAdd的合并结果 */
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
			List<Tuple4<Integer, EObject, EAttribute, Object>> addMayConflict = new ArrayList<>();

			for (int i = 0; i < size - 1; i++) {
				ValEdge valEdge = list.get(i);
				if (valEdge != null) {
					Object addTarget = valEdge.getTarget();
					// 打印出的分支下标要加1
					Tuple4<Integer, EObject, EAttribute, Object> tuple = new Tuple4<>(i + 1, sourceIndex.get(i), a,
							addTarget);
					addMayConflict.add(tuple);

					if (flag == true) {
						finalTarget = addTarget;
						flag = false;
					} else {
						if (addTarget == null && finalTarget == null) { // null和null用equals比较要报错
							continue;
						}
						if (addTarget.equals(finalTarget) == false) { // Object用equals比较
							// 冲突：新加点的单值属性矛盾
							int len = addMayConflict.size();
							ValEdgeConflict conflict = new ValEdgeConflict(addMayConflict.subList(0, len - 1),
									ConflictKind.Add, addMayConflict.subList(len - 1, len), ConflictKind.Add,
									"新加点的单值属性矛盾");
							System.err.println("*****" + conflict.toString());
							valEdgeConflicts.add(conflict);
							break;
						}
					}
				}
			}

			// 省略conflictsSize == valEdgeConflicts.size()
			// 否则，eSet会抛出异常
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
						addition = new HashSet<>(addTargets); // 这样就能去重了
						flag = false;
					} else {
						// 求并集
						addition.addAll(addTargets);
					}
				}
			}

			// 是否有冲突：新加点不在结果图中（比如类型不兼容）？（应该算之前的冲突）
			// 是否有冲突：超出多重性的上限？

			remain.addAll(addition); // 这样能转换成List
			valEdgeMulti_MultiKeyMap.put(a, key.getSourceIndex(), remain);
		}

		/** RefEdge的合并结果 */
		System.out.println("\n\n\n");
		List<RefEdgeConflict> refEdgeConflicts = new ArrayList<>();

		MultiKeyMap<Object, List<EObject>> refEdge_MultiKeyMap = new MultiKeyMap<>();
		for (Entry<RefEdge, List<RefEdge>> entry : refEdgeMatchGroupMap.entrySet()) {
			RefEdge key = entry.getKey();

			EReference r = key.getType();
			// eType在设置时，eGenericType自动被设置了
			if (r.isChangeable() == false || r.getName().equals("eGenericType")
					|| r.getName().equals("eGenericSuperTypes")) {
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();
			List<EObject> baseTargetIndex = key.getTargetIndex();

			List<RefEdge> list = entry.getValue();
			List<Tuple4<Integer, EObject, EReference, EObject>> deleteMayConflict = new ArrayList<>();
			List<Tuple4<Integer, EObject, EReference, EObject>> updateMayConflict = new ArrayList<>();
			List<EObject> finalTargetIndex = baseTargetIndex;
			boolean flag = true;
			for (int i = 0; i < size - 1; i++) {
				RefEdge refEdge = list.get(i);
				if (refEdge == null) {
					// 删除了点，相关的边也被删除
					// 打印出的分支下标要加1
					Tuple4<Integer, EObject, EReference, EObject> tuple = new Tuple4<>(i + 1, null, null, null);
					deleteMayConflict.add(tuple);

				} else {
					List<EObject> branchTargetIndex = refEdge.getTargetIndex();
					if (branchTargetIndex != baseTargetIndex) {
						// 打印出的分支下标要加1
						Tuple4<Integer, EObject, EReference, EObject> tuple = new Tuple4<>(i + 1, sourceIndex.get(i), r,
								branchTargetIndex.get(i));
						updateMayConflict.add(tuple);

						if (flag == true) {
							finalTargetIndex = branchTargetIndex;
							flag = false;
						} else {
							if (branchTargetIndex != finalTargetIndex) {
								// 冲突：单值引用的矛盾修改
								int len = updateMayConflict.size();
								RefEdgeConflict conflict = new RefEdgeConflict(updateMayConflict.subList(0, len - 1),
										ConflictKind.Update, updateMayConflict.subList(len - 1, len),
										ConflictKind.Update, "单值引用的矛盾修改");
								System.err.println("*****" + conflict.toString());
								refEdgeConflicts.add(conflict);
								break;
							}
						}
					}
				}
			}

			if (deleteMayConflict.size() > 0 && updateMayConflict.size() > 0) {
				// 冲突：删除了点-修改了点的单值引用
				RefEdgeConflict conflict = new RefEdgeConflict(deleteMayConflict, ConflictKind.Delete,
						updateMayConflict, ConflictKind.Update, "删除了点-修改了点的单值引用");
				System.err.println("*****" + conflict.toString());
				refEdgeConflicts.add(conflict);
			}

			if (deleteMayConflict.size() == 0) {
				refEdge_MultiKeyMap.put(r, sourceIndex, finalTargetIndex);
			}

		}

		/** RefEdgeMulti的合并结果 */
		System.out.println("\n\n\n");
		List<RefEdgeMultiConflict> refEdgeMultiConflicts = new ArrayList<>();
		MultiKeyMap<Object, List<List<EObject>>> refEdgeMulti_MultiKeyMap = new MultiKeyMap<>();

		for (Entry<RefEdgeMulti, List<RefEdgeMulti>> entry : refEdgeMultiMatchGroupMap.entrySet()) {
			RefEdgeMulti key = entry.getKey();

			EReference r = key.getType();
			// eType在设置时，eGenericType自动被设置了
			if (r.isChangeable() == false || r.getName().equals("eGenericType")
					|| r.getName().equals("eGenericSuperTypes")) {
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();
			List<List<EObject>> baseTargetsIndex = key.getTargetsIndex();

			List<RefEdgeMulti> list = entry.getValue();
			List<Tuple4<Integer, EObject, EReference, List<EObject>>> deleteMayConflict = new ArrayList<>();
			List<Tuple4<Integer, EObject, EReference, List<EObject>>> addMayConflict = new ArrayList<>();

			List<List<EObject>> remain = new ArrayList<>(baseTargetsIndex); // remain初始化为baseTargetsIndex的拷贝
			Set<List<EObject>> addition = new HashSet<>(); // 这样就能去重了

			for (int i = 0; i < size - 1; i++) {
				RefEdgeMulti refEdgeMulti = list.get(i);
				if (refEdgeMulti == null) {
					// 删除了点，相关的边也自动被删除
					// 打印出的分支下标要加1
					Tuple4<Integer, EObject, EReference, List<EObject>> tuple = new Tuple4<>(i + 1, null, null, null);
					deleteMayConflict.add(tuple);

				} else { // 不视为修改
					List<List<EObject>> branchTargetsIndex = new ArrayList<>(refEdgeMulti.getTargetsIndex()); // 这样不影响括号里的
					// 求交集：确定在分支中未删除的元素
					remain.retainAll(branchTargetsIndex);
					// 求差集：确定在分支中新加的元素
					branchTargetsIndex.removeAll(baseTargetsIndex);
					addition.addAll(branchTargetsIndex);
					// 记录或许发生的冲突
					// 如果判断条件为>0的话，branchTargetsIndex.get(0)==null也会进入
					if (branchTargetsIndex.size() > 0 && branchTargetsIndex.get(0) != null) {
						List<EObject> tmp = new ArrayList<>();

						List<Tuple4<Integer, EObject, EReference, List<EObject>>> deleteOtherConflict = new ArrayList<>();
						for (List<EObject> index : branchTargetsIndex) {
							if (nodesReverseIndexMap.get(index) == null) {
								for (int j = 0; j < index.size(); j++) {
									if (index.get(j) == null) {
										Tuple4<Integer, EObject, EReference, List<EObject>> tuple = new Tuple4<>(j + 1,
												null, null, null);
										deleteOtherConflict.add(tuple);
									}
								}
							}
							// 获得分支中真正的对象
							EObject eObject = index.get(i);
							tmp.add(eObject);
						}
						// 打印出的分支下标要加1
						Tuple4<Integer, EObject, EReference, List<EObject>> tuple = new Tuple4<>(i + 1,
								sourceIndex.get(i), r, tmp);
						addMayConflict.add(tuple);

						if (deleteOtherConflict.size() > 0) {
							// 冲突：删除了点-新加了点的多值引用
							RefEdgeMultiConflict conflict = new RefEdgeMultiConflict(deleteOtherConflict,
									ConflictKind.Delete, addMayConflict, ConflictKind.Add, "删除了其他点-新加了当前点的多值引用");
							System.err.println("*****" + conflict.toString());
							refEdgeMultiConflicts.add(conflict);
						}

					}

				}
			}
			// remain和addition求并集，最终是remain加到结果图中
			remain.addAll(addition);

			if (deleteMayConflict.size() > 0 && addMayConflict.size() > 0) {
				// 冲突：删除了点-新加了点的多值引用
				RefEdgeMultiConflict conflict = new RefEdgeMultiConflict(deleteMayConflict, ConflictKind.Delete,
						addMayConflict, ConflictKind.Add, "删除了当前点-新加了当前点的多值引用");
				System.err.println("*****" + conflict.toString());
				refEdgeMultiConflicts.add(conflict);
			}

			if (deleteMayConflict.size() == 0) {
				refEdgeMulti_MultiKeyMap.put(r, sourceIndex, remain);
			}

		}

		/** RefEdgeAdd的合并结果 */
		System.out.println("\n\n\n");
		for (Entry<RefEdge, List<RefEdge>> entry : refEdgeAddMatchGroupMap.entrySet()) {
			RefEdge key = entry.getKey();

			EReference r = key.getType();
			// eType在设置时，eGenericType自动被设置了
			if (r.isChangeable() == false || r.getName().equals("eGenericType")
					|| r.getName().equals("eGenericSuperTypes")) {
				continue;
			}

			List<EObject> sourceIndex = key.getSourceIndex();

			List<RefEdge> list = entry.getValue();
			List<EObject> finalTargetIndex = null;
			boolean flag = true;
			List<Tuple4<Integer, EObject, EReference, EObject>> addMayConflict = new ArrayList<>();
			for (int i = 0; i < size - 1; i++) {
				RefEdge refEdge = list.get(i);
				if (refEdge != null) {
					List<EObject> addTargetIndex = refEdge.getTargetIndex();
					// 打印出的分支下标要加1
					if (addTargetIndex != null) {
						Tuple4<Integer, EObject, EReference, EObject> tuple = new Tuple4<>(i + 1, sourceIndex.get(i), r,
								addTargetIndex.get(i));
						addMayConflict.add(tuple);
					}

					if (flag == true) {
						finalTargetIndex = addTargetIndex;
						flag = false;
					} else {
						if (addTargetIndex != finalTargetIndex) {
							// 冲突：新加点的单值引用矛盾
							int len = addMayConflict.size();
							RefEdgeConflict conflict = new RefEdgeConflict(addMayConflict.subList(0, len - 1),
									ConflictKind.Add, addMayConflict.subList(len - 1, len), ConflictKind.Add,
									"新加点的单值引用矛盾");
							System.err.println("*****" + conflict.toString());
							refEdgeConflicts.add(conflict);
							break;
						}
					}
				}
			}

			refEdge_MultiKeyMap.put(r, sourceIndex, finalTargetIndex);
		}

		/** RefEdgeMultiAdd的合并结果 */
		System.out.println("\n\n\n");
		for (Entry<RefEdgeMulti, List<RefEdgeMulti>> entry : refEdgeMultiAddMatchGroupMap.entrySet()) {
			RefEdgeMulti key = entry.getKey();

			EReference r = key.getType();
			// eType在设置时，eGenericType自动被设置了
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
						addition = new HashSet<>(addTargetsIndex); // 这样就能去重了
						flag = false;
					} else {
						// 求并集
						addition.addAll(addTargetsIndex);
					}
				}
			}

			// 是否有冲突：新加点不在结果图中（比如类型不兼容、点被删了）？（应该算之前的冲突吧）
			// 是否有冲突：超出多重性限制

			remain.addAll(addition); // 这样能转换成List
			refEdgeMulti_MultiKeyMap.put(r, key.getSourceIndex(), remain);
		}

		/** 设置结果图中的属性和关联 */
		System.out.println("\n\n\n**********************设置结果图中的属性和关联*************************");
		for (EObject e : resourceNodes) {
			List<EObject> sourceIndex = nodesIndexMap.get(e);
			EClass eClass = e.eClass();
			System.out.println("\n\n\nsourceIndex: " + sourceIndex);

			for (EAttribute a : eClass.getEAllAttributes()) {

				if (a.isChangeable() == false) {
					continue;
				}

				if (a.isMany() == false) { // 单值属性
					System.out.println("单值属性: " + a);
					Object target = valEdge_MultiKeyMap.get(a, sourceIndex);
					System.out.println("target: " + target);
					e.eSet(a, target);

				} else { // 多值属性
					System.out.println("多值属性：" + a);
					List<Object> targets = (List<Object>) valEdgeMulti_MultiKeyMap.get(a, sourceIndex);

					if (targets.size() <= 1 || !needOrderSet.contains(a.getName())) {
						e.eSet(a, targets);

					} else {
						List<Object> finalList = new ArrayList<>();

						// 由于不知道e是否为新加的点
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
								ValEdgeMulti valEdgeMulti2 = list.get(i); // 不可能出现null吧
								for (int j = 0; j < valEdgeMulti2.getTargets().size(); j++) {
									Object obj = valEdgeMulti2.getTargets().get(j);
									branchFlag.put(i, obj, j);
								}
							}

						} else {
							valEdgeMultiAdd = valEdgeMultiAddHelper.get(a, sourceIndex);
							List<ValEdgeMulti> list = valEdgeMultiAddMatchGroupMap.get(valEdgeMultiAdd);
							for (int i = 0; i < size - 1; i++) {
								ValEdgeMulti valEdgeMulti2 = list.get(i); // 可能为null
								if (valEdgeMulti2 != null) {
									for (int j = 0; j < valEdgeMulti2.getTargets().size(); j++) {
										Object obj = valEdgeMulti2.getTargets().get(j);
										branchFlag.put(i, obj, j);
									}
								}
							}

						}

						int nodeSize = targets.size();
						TopoGraph g = new TopoGraph(nodeSize);

						for (int i = 0; i < nodeSize - 1; i++) {
							Object x = targets.get(i);
							for (int j = i + 1; j < nodeSize; j++) {
								Object y = targets.get(j);
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

						// 或许还要传进conflicts
						try {
							List<Integer> topologicalSort = g.topologicalSort();
							for (Integer i : topologicalSort) {
								finalList.add(targets.get(i));
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}

						// 最后才eSet
						e.eSet(a, finalList);
					}

				}
			}

			// 在设置当前节点的引用时，可能target的属性还没设置，但不影响最终的结果
			// 所以可以把下面处理引用的放到这里，遍历一次即可

		}

		System.out.println("------------------------设置引用-----------------------------");
		for (EObject e : resourceNodes) {
			List<EObject> sourceIndex = nodesIndexMap.get(e);
			EClass eClass = e.eClass();
			System.out.println("\n\n\nsourceIndex: " + sourceIndex);

			for (EReference r : eClass.getEAllReferences()) {
				// eType在设置时，eGenericType自动被设置了
				if (r.isChangeable() == false || r.getName().equals("eGenericType")
						|| r.getName().equals("eGenericSuperTypes")) {
					continue;
				}

				if (r.isMany() == false) { // 单值引用
					System.out.println("单值引用：" + r);
					List<EObject> targetIndex = refEdge_MultiKeyMap.get(r, sourceIndex);
					EObject node = nodesReverseIndexMap.get(targetIndex);
					e.eSet(r, node);

				} else { // 多值引用
					System.out.println("多值引用：" + r);

					List<List<EObject>> targetsIndex = refEdgeMulti_MultiKeyMap.get(r, sourceIndex);
					List<EObject> finalList = new ArrayList<>();

					if (targetsIndex.size() <= 1 || !needOrderSet.contains(r.getName())) {
						for (List<EObject> nodeIndex : targetsIndex) {
							EObject node = nodesReverseIndexMap.get(nodeIndex);
							if (node != null) {
								finalList.add(node);
							}
						}

					} else {
						// initialize
						List<List<EObject>> mergeIndex = new ArrayList<>();

						// 由于不知道e是否为新加的点
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
								RefEdgeMulti refEdgeMulti2 = list.get(i); // 不可能出现null吧
								for (int j = 0; j < refEdgeMulti2.getTargetsIndex().size(); j++) {
									List<EObject> index = refEdgeMulti2.getTargetsIndex().get(j);
									branchFlag.put(i, index, j);
								}
							}

						} else {
							refEdgeMultiAdd = refEdgeMultiAddHelper.get(r, sourceIndex);
							List<RefEdgeMulti> list = refEdgeMultiAddMatchGroupMap.get(refEdgeMultiAdd);
							for (int i = 0; i < size - 1; i++) {
								RefEdgeMulti refEdgeMulti2 = list.get(i); // 新加的列表，可能出现null
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

						// 或许还要传conflicts
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

					// 最后才eSet
					e.eSet(r, finalList);

				}
			}

		}

		System.out.println("\n\n\n*****************resourceNodes************************");
		resourceNodes.forEach(e -> {
			System.out.println("e: " + e);
			EClass eClass = e.eClass();
			eClass.getEAllReferences().forEach(r -> {
				if (r.isMany() == false) {
					System.out.print("单值引用");
				} else {
					System.out.print("多值引用");
				}
				System.out.println(r.getName());
				System.out.println(e.eGet(r));
			});
			System.out.println("---------------------\n");
		});

		/** 写入到文件 */
		List<EObject> roots = resourceNodes.stream().filter(n -> n.eContainer() == null).collect(Collectors.toList());
		m1Resource.getContents().addAll(roots);
		try {
			m1Resource.save(null);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("已写入到文件");

		System.out.println("done");

	}

	/**
	 * 计算xIndex和yIndex的合并序
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
			// 如果xIndex和yIndex都属于某个分支图
			if (xPositionBranch != null && yPositionBranch != null) {
				Order o_k = Order.unkown;
				Integer result = xPositionBranch - yPositionBranch;
				if (result < 0) {
					o_k = Order.less;
				} else {
					o_k = Order.greater;
				}
				// 如果xIndex和yIndex还都属于基础图
				if (xPositionBase != null && yPositionBase != null) {
					o = o_b;
				}

				if (o_k != o) { // o_k不等于o，以分支的序为准
					t = Force.hard;
					o = o_k;
				}
			}

			ord_k.add(new Tuple2<Force, Order>(t, o));

		}

		// 再计算合并的序
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
				throw new Exception("###conflict: 都是Force.hard，且不相同\n");
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
					throw new Exception("###conflict: 都是Force.soft，且不相同\n");
				}
			}
		}
		return null;
	}

	/**
	 * 计算公共子类
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

enum Force {
	soft, hard
}

enum Order {
	less, greater, unkown
}