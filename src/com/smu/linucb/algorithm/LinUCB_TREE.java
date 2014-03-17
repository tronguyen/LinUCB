package com.smu.linucb.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.smu.control.ALGControl;
import com.smu.linucb.global.AlgorithmType;
import com.smu.linucb.global.Environment;

public class LinUCB_TREE extends UCB1 {
	private static double rewardTotal = 0;
	public static UCB1 rootTree;
	public static List<UCB1> leavesTree = new ArrayList<UCB1>();
	public static List<Integer> fstTimeUsrLst = new ArrayList<Integer>();
	public static Random rClus = new Random();

	public LinUCB_TREE() {
		this.setAlgType(AlgorithmType.LINUCB_TREE);
		this.rClus.setSeed(System.nanoTime() * Thread.currentThread().getId());
	}

	// Build empty tree
	public static void buildTree(UCB1 pRoot, int maxLv) {
		if (maxLv == 0) {
			// Create LinUCB node
			pRoot.linucb = new LinUCB();
			LinUCB_TREE.leavesTree.add(pRoot);
			return;
		}
		UCB1 pNext = null;
		for (int i = 0; i < Environment.numBranch; i++) {
			pNext = new UCB1(pRoot);
			// Generate payoff list for all users

			buildTree(pNext, maxLv - 1);
		}
	}

	public static void backPropagation(UCB1 curNode, double payoff, int usr) {
		UCB1 cur = curNode;
		UserItem usrItem = null;
		while (cur != null) {
			if (cur.payoffMap.containsKey(usr)) {
				usrItem = cur.payoffMap.get(usr);
				usrItem.setVisit(usrItem.getVisit() + 1);
				usrItem.setPayoff(usrItem.getPayoff() + payoff);
			} else {
				cur.payoffMap.put(usr, new UserItem(payoff, 1));
			}
			cur = cur.pNode;
		}
	}

	@Override
	public void run() {
		int usr;
		LinUCB_TREE.rootTree = new UCB1();
		UCB1 cur = null;
		LinUCB cluster = null;
		LinUCB_TREE.buildTree(LinUCB_TREE.rootTree,
				(int) Math.ceil((Math.log(Environment.numCluster) / Math
						.log(Environment.numBranch))));

		Environment.drChart.genDiffConfig(AlgorithmType.LINUCB_TREE);
		for (int i = 1; i < Environment.limitTime; i++) {
			// Pick user randomly
			usr = Environment.userLst.get(rUSR.nextInt(Environment.userLst
					.size()));

			if (LinUCB_TREE.fstTimeUsrLst.contains(usr)) {
				/*
				 * Run UCB1. Find the cluster the user to which belongs
				 */
				cur = LinUCB_TREE.rootTree;
				while (cur.childLst.size() != 0) {
					cur = UCB1.impl(usr, cur);
				}
			} else {
				LinUCB_TREE.fstTimeUsrLst.add(usr);
				// Select randomly cluster for user having the first time
				// falling
				// down
				cur = LinUCB_TREE.leavesTree.get(LinUCB_TREE.rClus
						.nextInt(Environment.numCluster));
			}

			// Run LinUCB for the cluster
			cluster = cur.linucb;
			cluster.setUser(usr);
			cluster.impl();
			cluster.reset();

			// Update weight for the path
			LinUCB_TREE.backPropagation(cur, cluster.getPayoff(), usr);

			LinUCB_TREE.rewardTotal += cluster.getPayoff();
			// Draw chart
			this.displayResult(i, LinUCB_TREE.rewardTotal);
		}
		this.interrupt();
	}
}
