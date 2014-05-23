package com.smu.linucb.global;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ejml.simple.SimpleMatrix;

public class GlobalFunction {
	public static void addValueMap(Map<Integer, Set<Integer>> objMap, int key,
			int value) {
		if (objMap.containsKey(key)) {
			objMap.get(key).add(value);
		} else {
			Set<Integer> initLst = new HashSet<Integer>();
			initLst.add(value);
			objMap.put(key, initLst);
		}
	}

	public static void delValueMap(Map<Integer, Set<Integer>> objMap, int key,
			int value) {
		objMap.get(key).remove(value);
	}

	public static void sumValueMap(Map<Integer, Double> objMap, int key,
			double value) {
		if (objMap.containsKey(key)) {
			objMap.put(key, objMap.get(key) + value);
		} else {
			objMap.put(key, value);
		}
	}
	
	public static double[] convert2DoubleArr(SimpleMatrix mx){
		double[] out = new double[Environment.featureSize];
		for(int i=0; i< Environment.featureSize; i++){
			out[i] = mx.get(i);
		}
		return out;
	}
}
