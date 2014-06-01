package com.smu.linucb.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smu.alg.view.DrawChart;

public class Environment {
	public static Map<String, Double> hm_token_weight = new HashMap<String, Double>();
	public static Map<String, Set<Integer>> hs_df = new HashMap<String, Set<Integer>>();
	public static Map<Integer, Map<String, Double>> hm_bookmark_tag = new HashMap<Integer, Map<String, Double>>();
	public static Set<String> tagSet = new HashSet<String>();
	public static List<Integer> userLst = new ArrayList<Integer>();
	public static Map<Integer, Double[]> normMatrix = new HashMap<Integer, Double[]>();
	public static List<Integer> bmidLst = new ArrayList<Integer>();
	public static Set<Integer> removedBM = new HashSet<Integer>();
	public static DrawChart drChart = new DrawChart("Multi-Bandits Algorithm");
	public static Map<Integer, Integer> usrClusterMap = new HashMap<Integer, Integer>();

	// Config for LinUCB TREE
	public static int numCluster = 16;
	public static int numBranch = 16;

	// Config parameters
	public static int featureSize = 25;
	public static int numContextVecs = 25;
	public static double delta = 0.5;
	public static double alpha = 1 + Math.sqrt(Math.log(2 / delta) / 2);
	public static double alphaUCB = 0.1;
	public static double payoffRight = 1;
	public static double payoffWrong = (double) -1 / 24;
	public static int limitTime = 25000;
}
