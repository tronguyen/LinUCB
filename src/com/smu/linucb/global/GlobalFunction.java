package com.smu.linucb.global;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GlobalFunction {
	public static void addSpecMap(Map<Integer, List<Integer>> objMap, int key,
			int value) {
		if (objMap.containsKey(key)) {
			objMap.get(key).add(value);
		} else {
			List<Integer> initLst = new ArrayList<Integer>();
			initLst.add(value);
			objMap.put(key, initLst);
		}
	}
}
