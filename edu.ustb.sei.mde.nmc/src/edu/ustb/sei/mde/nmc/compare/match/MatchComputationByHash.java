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
			 * ��sourece �ĳ��ȹ��̣��ᵼ��hash�㷨ʧЧ�������Ҫ�Թ��̵Ĵʲ���
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
				// 3������һ������Ϊ64����������(����Ҫ����64λ������ָ��,Ҳ��������������),
				// ��ÿһ���ִ�hash������н����ж�,�����1000...1,��ô����ĵ�һλ��ĩβһλ��1,
				// �м��62λ��һ,Ҳ����˵,��1��1,��0��1.һֱ�������еķִ�hash����ȫ���ж����.
				double tf = (double) wordCount.get(word) / len;
				Double weight = 100 * tf * wordCount.get(word); // ���Ȩ�أ�Ȩ��Ӧ��Ϊ���ִ����������Ǹ��ݴ�����ָ����
				if (t.and(bitmask).signum() != 0) {
					// �����Ǽ��������ĵ�������������������
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
