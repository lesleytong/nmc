package edu.ustb.sei.mde.nmc.compare.match;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.nmc.compare.Comparison;
import edu.ustb.sei.mde.nmc.compare.HashFunction;

public class MatchComputationByHash implements HashFunction {

	/**
	 * 
	 * @param Map<String, Integer> wordCount
	 * recording words frequency by mapping
	 */
	public Map<String, Integer> wordCount;
	/**
	 * @param hashbits 
	 * define hash length
	 */
	private int hashbits = 64;
	public int hammingDistance(String s1, String s2) {
		BigInteger one = this.simHash(s1);
		BigInteger two = this.simHash(s2);
		BigInteger m = new BigInteger("1").shiftLeft(this.hashbits).subtract(new BigInteger("1"));
		BigInteger x = one.xor(two).and(m);
		int tot = 0;
		while (x.signum() != 0) {
			tot += 1;
			x = x.and(x.subtract(new BigInteger("1")));
		}
		return tot;
	}
	
	private static double getSimilarity(String doc1, String doc2) {
		if (StringUtils.isBlank(doc1) or StringUtils.isBlank(doc2)) {
				return 0L;
		}
		Map<Character,int[]> algMap=new HashMap<>();
		for (int i = 0; i<doc1.length(); i++) {
				char d1 = doc1.charAt(i);
				int[] fq = algMap.get(d1);
				if (fq != null && fq.length == 2) {
						fq[0]++;
				} else {
						fq = new int[2];
						fq[0] = 1;
						fq[1] = 0;
						algMap.put(d1, fq);
				}
		}
		for (int i = 0; i<doc2.length(); i++) {
				char d2 = doc2.charAt(i);
				int[] fq = algMap.get(d2);
				if (fq != null && fq.length == 2) {
						fq[1]++;
				} else {
						fq = new int[2];
						fq[0] = 0;
						fq[1] = 1;
						algMap.put(d2, fq);
				}
		}
		double sqdoc1 = 0;
		double sqdoc2 = 0;
		double denuminator = 0;
		for (Map.Entry entry : algMap.entrySet()) {
				int[] c = (int[]) entry.getValue();
				denuminator += c[0] * c[1];
				sqdoc1 += c[0] * c[0];
				sqdoc2 += c[1] * c[1];
		}
		return denuminator / Math.sqrt(sqdoc1 * sqdoc2);
}
	
	public BigInteger simHash(Map<String, Integer> wordCount) {

		int[] v = new int[this.hashbits];

		Map<String, Integer> wordCount = new HashMap<String, Integer>();
		
		Integer count = 0;
		for (Term term : ansjList) {
			count = wordCount.get(term.getName());
			if (count == null) {
				wordCount.put(term.getName(), 1);
			} else {
				wordCount.put(term.getName(), count + 1);
			}
		}
		
		int len = wordCount.size();
		String word = "";
		for (Term term : ansjList) {
			word = term.getName(); // 分词字符串
			// 2、将每一个分词hash为一组固定长度的数列.比如 64bit 的一个整数.
			BigInteger t = this.hash(word);
			for (int i = 0; i < this.hashbits; i++) {
				BigInteger bitmask = new BigInteger("1").shiftLeft(i);
				// 3、建立一个长度为64的整数数组(假设要生成64位的数字指纹,也可以是其它数字),
				// 对每一个分词hash后的数列进行判断,如果是1000...1,那么数组的第一位和末尾一位加1,
				// 中间的62位减一,也就是说,逢1加1,逢0减1.一直到把所有的分词hash数列全部判断完毕.
				double tf = (double) wordCount.get(word) / len;
				if(IDF.get(word)==null) continue;
				Double weight = 100 * tf * IDF.get(word); // 添加权重，权重应改为出现次数，而不是根据词性来指定。
//				if(weight==null) continue;
				// if (wordCount.containsKey(word)) {
				// weight = wordCount.get(word);
				// }
				if (t.and(bitmask).signum() != 0) {
					// 这里是计算整个文档的所有特征的向量和
					v[i] += weight;
				} else {
					v[i] -= weight;
				}
			}
		}

		BigInteger fingerprint = new BigInteger("0");
		for (int i = 0; i < this.hashbits; i++) {
			if (v[i] >= 0) {
				fingerprint = fingerprint.add(new BigInteger("1").shiftLeft(i));
			}
		}
		return fingerprint;
	}
	
	public boolean areIdentic(EObject eObj, EObject fastCheck) {
		return false;
		
	}

	public double distance(EObject a, EObject b) {
		double maxDist = Math.max(getThresholdAmount(a), getThresholdAmount(b));
		double measuredDist = new CountingDiffEngine(maxDist, this.fakeComparison)
				.measureDifferences(inProgress, a, b);
		
		if (measuredDist > maxDist) {
			return Double.MAX_VALUE;
		}
		return measuredDist;
	}
	
}
