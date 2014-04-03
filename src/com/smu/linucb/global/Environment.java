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

	// 4 Clustering by K-Mean
	public static Map<Integer, Integer> usrClusterMap = new HashMap<Integer, Integer>();
	public static Map<Integer, List<Integer>> clusterMap = new HashMap<Integer, List<Integer>>();
	public static Map<Integer, List<Integer>> errUsrClsLst = new HashMap<Integer, List<Integer>>();
	public static Set<Integer> errUsrSet = new HashSet<Integer>();
	// public static Map<Integer, List<Integer>> clusterExtraMap = new
	// HashMap<Integer, List<Integer>>();

	// Configure for LinUCB TREE
	public static int numCluster = 16;
	public static int numBranch = 16;

	// Configure for warm-start
	public static int numWarmIter = 2000;

	// Configure parameters
	public static int featureSize = 25;
	public static int numContextVecs = 25;
	public static double delta = 0.5;
	public static double alphaLin = 1 + Math.sqrt(Math.log(2 / delta) / 2);
	public static double alphaUCB = 0.01;
	public static double payoffRight = 1;
	public static double payoffWrong = (double) -1 / 24;
	public static int limitTime = 20000;
	public static int numAvgLoop = 1; // Number of thread for each algorithm
	public static int buffSizeDisplay = 10;
	public static double percentExchange = 0.1; 
}
