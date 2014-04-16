package com.smu.linucb.global;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GlobalFunction {
	public static void addValueMap(Map<Integer, List<Integer>> objMap, int key,
			int value) {
		if (objMap.containsKey(key)) {
			objMap.get(key).add(value);
		} else {
			List<Integer> initLst = new ArrayList<Integer>();
			initLst.add(value);
			objMap.put(key, initLst);
		}
	}

	public static void sumValueMap(Map<Integer, Double> objMap, int key,
			double value) {
		if (objMap.containsKey(key)) {
			objMap.put(key, objMap.get(key) + value);
		} else {
			objMap.put(key, value);
		}
	}
}
