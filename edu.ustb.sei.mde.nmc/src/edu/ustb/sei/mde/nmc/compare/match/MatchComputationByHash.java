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

	
//	private static double getSimilarity(String doc1, String doc2) {
//		if (StringUtils.isBlank(doc1) or StringUtils.isBlank(doc2)) {
//				return 0L;
//		}
//		Map<Character,int[]> algMap=new HashMap<>();
//		for (int i = 0; i<doc1.length(); i++) {
//				char d1 = doc1.charAt(i);
//				int[] fq = algMap.get(d1);
//				if (fq != null && fq.length == 2) {
//						fq[0]++;
//				} else {
//						fq = new int[2];
//						fq[0] = 1;
//						fq[1] = 0;
//						algMap.put(d1, fq);
//				}
//		}
//		for (int i = 0; i<doc2.length(); i++) {
//				char d2 = doc2.charAt(i);
//				int[] fq = algMap.get(d2);
//				if (fq != null && fq.length == 2) {
//						fq[1]++;
//				} else {
//						fq = new int[2];
//						fq[0] = 0;
//						fq[1] = 1;
//						algMap.put(d2, fq);
//				}
//		}
//		double sqdoc1 = 0;
//		double sqdoc2 = 0;
//		double denuminator = 0;
//		for (Map.Entry entry : algMap.entrySet()) {
//				int[] c = (int[]) entry.getValue();
//				denuminator += c[0] * c[1];
//				sqdoc1 += c[0] * c[0];
//				sqdoc2 += c[1] * c[1];
//		}
//		return denuminator / Math.sqrt(sqdoc1 * sqdoc2);
//}
	
	private BigInteger hash(String source) {
		if (source == null || source.length() == 0) {
			return new BigInteger("0");
		} else {
			/**
			 * 当sourece 的长度过短，会导致hash算法失效，因此需要对过短的词补偿
			 */
			while (source.length() < 3) {
				source = source + source.charAt(0);
			}
			char[] sourceArray = source.toCharArray();
			BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
			BigInteger m = new BigInteger("1000003");
			BigInteger mask = new BigInteger("2").pow(this.hashbits).subtract(new BigInteger("1"));
			for (char item : sourceArray) {
				BigInteger temp = BigInteger.valueOf((long) item);
				x = x.multiply(m).xor(temp).and(mask);
			}
			x = x.xor(new BigInteger(String.valueOf(source.length())));
			if (x.equals(new BigInteger("-1"))) {
				x = new BigInteger("-2");
			}
			return x;
		}
	}
	
	public BigInteger simHash(Map<String, Integer> wordCount) {

		int[] v = new int[this.hashbits];
		
		int len = wordCount.size();            
		for(Map.Entry<String,Integer> entry : wordCount.entrySet()){
            String word = entry.getKey();
			int count = entry.getValue();
			BigInteger t = this.hash(word);
			for (int i = 0; i < this.hashbits; i++) {
				BigInteger bitmask = new BigInteger("1").shiftLeft(i);
				// 3、建立一个长度为64的整数数组(假设要生成64位的数字指纹,也可以是其它数字),
				// 对每一个分词hash后的数列进行判断,如果是1000...1,那么数组的第一位和末尾一位加1,
				// 中间的62位减一,也就是说,逢1加1,逢0减1.一直到把所有的分词hash数列全部判断完毕.
				double tf = (double) wordCount.get(word) / len;
				Double weight = 100 * tf * wordCount.get(word); // 添加权重，权重应改为出现次数，而不是根据词性来指定。
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
	
	public boolean areIdentic(BigInteger one, BigInteger two) {
		if (one == two)
			return true;
		else
			return false;
	}

	public double distance(BigInteger one, BigInteger two) {
//		double maxDist = Math.max(getThresholdAmount(a), getThresholdAmount(b));
//		double measuredDist = new CountingDiffEngine(maxDist, this.fakeComparison)
//				.measureDifferences(inProgress, a, b);
//		
//		if (measuredDist > maxDist) {
//			return Double.MAX_VALUE;
//		}
		return hammingDistance(one,two);
	}
	
	public int hammingDistance(BigInteger one, BigInteger two) {
		BigInteger m = new BigInteger("1").shiftLeft(64).subtract(new BigInteger("1"));
		BigInteger x = one.xor(two).and(m);
		int dis = 0;
		while (x.signum() != 0) {
			dis += 1;
			x = x.and(x.subtract(new BigInteger("1")));
		}
		return dis;
	}
}
