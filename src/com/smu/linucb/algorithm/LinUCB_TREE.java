package com.smu.linucb.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.smu.linucb.global.AlgorithmType;
import com.smu.linucb.global.Environment;

public class LinUCB_TREE extends UCB1 {
	private double rewardTotal = 0;
	private UCB1 rootTree;
	private List<UCB1> leavesTree = new ArrayList<UCB1>();
	private List<Integer> fstTimeUsrLst = new ArrayList<Integer>();
	private Random rClus = new Random();
	private boolean fixedCluster = false;
	private boolean isWarmStart = false;

	public LinUCB_TREE() {
		this.setAlgType(AlgorithmType.LINUCB_TREE);
		this.rClus.setSeed(System.nanoTime() * Thread.currentThread().getId());
	}

	// Build empty tree
	public void buildTree(UCB1 pRoot, int maxLv) {
		if (maxLv == 0) {
			// Create LinUCB node
			pRoot.linucb = new LinUCB();
			this.leavesTree.add(pRoot);
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
		this.rootTree = new UCB1();
		UCB1 cur = null;
		LinUCB cluster = null;
		buildTree(this.rootTree,
				(int) Math.ceil((Math.log(Environment.numCluster) / Math
						.log(Environment.numBranch))));

		for (int i = 1; i < Environment.limitTime; i++) {
			// Pick user randomly
			usr = Environment.userLst.get(rUSR.nextInt(Environment.userLst
					.size()));
			if (!this.isFixedCluster()) {
				if (this.fstTimeUsrLst.contains(usr)) {
					/*
					 * Run UCB1. Find the cluster the user to which belongs
					 */
					cur = this.rootTree;
					while (cur.childLst.size() != 0) {
						cur = UCB1.impl(usr, cur);
					}
				} else {
					this.fstTimeUsrLst.add(usr);
					// Select randomly cluster for user having the first time
					// falling
					// down
					if (!this.isWarmStart()) {
						cur = this.leavesTree.get(this.rClus
								.nextInt(Environment.numCluster));
					} else {
						cur = this.leavesTree.get(Environment.usrClusterMap
								.get(usr));
					}
				}
			} else {
				cur = this.leavesTree.get(Environment.usrClusterMap.get(usr));
			}

			// Run LinUCB for the cluster
			cluster = cur.linucb;
			cluster.setUser(usr);
			cluster.impl();
			cluster.reset();

			// Update weight for the path
			LinUCB_TREE.backPropagation(cur, cluster.getPayoff(), usr);

			this.rewardTotal += cluster.getPayoff();
			// Draw chart
			this.displayResult(i, this.rewardTotal);
		}
		this.interrupt();
	}

	public boolean isFixedCluster() {
		return fixedCluster;
	}

	public void setFixedCluster(boolean fixedCluster) {
		this.fixedCluster = fixedCluster;
	}

	public boolean isWarmStart() {
		return isWarmStart;
	}

	public void setWarmStart(boolean isWarmStart) {
		this.isWarmStart = isWarmStart;
	}
}
