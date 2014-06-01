package com.smu.linucb.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.twod.Euclidean2D;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

import com.smu.linucb.global.AlgorithmType;
import com.smu.linucb.global.Environment;
import com.smu.linucb.global.GlobalFunction;

public class LinUCB_TREE extends UCB1 {
	private double rewardTotal = 0;
	private UCB1 rootTree;
	private List<UCB1> leavesTree = new ArrayList<UCB1>();
	private List<Integer> fstTimeUsrLst = new ArrayList<Integer>();
	private Random rClus = new Random();
	private boolean fixedCluster = false;
	private boolean isWarmStart = false;
	private Map<Integer, IndItem> userItemMap = new HashMap<Integer, IndItem>();
	private Map<Integer, Set<Integer>> clusterItemLstMap = new HashMap<Integer, Set<Integer>>();
	private int indexLeaf = 0;
	private EuclideanDistance ed = new EuclideanDistance();

	public LinUCB_TREE() {
		this.setAlgType(AlgorithmType.LINUCB_TREE);
		this.rClus.setSeed(System.nanoTime() * Thread.currentThread().getId());
	}

	// Build empty tree
	public void buildTree(UCB1 pRoot, int maxLv) {
		if (maxLv == 0) {
			// Create LinUCB node
			pRoot.linucb = new LinUCB();
			pRoot.setIndexLeaf(this.indexLeaf);
			this.leavesTree.add(pRoot);
			this.indexLeaf++;
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
		int oldIdx;
		for (int i = 1; i <= Environment.limitTime; i++) {
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
					oldIdx = this.userItemMap.get(usr).getClusterIndex();
					if (cur.getIndexLeaf() != oldIdx) { // If change cluster
						// Add condition to switch new cluster
						if (isSwitched(usr)) {
							// Delete link to old cluster
							GlobalFunction.delValueMap(this.clusterItemLstMap,
									oldIdx, usr);
							// Update link to new cluster
							this.userItemMap.get(usr).setClusterIndex(
									cur.getIndexLeaf());
							GlobalFunction.addValueMap(this.clusterItemLstMap,
									cur.getIndexLeaf(), usr);
						} else {
							// Keep user in old cluster
							cur = this.leavesTree.get(oldIdx);
						}
					}
				} else {
					this.fstTimeUsrLst.add(usr);
					// Select randomly cluster for user having the first time
					// falling
					// down
					if (!this.isWarmStart()) {
						cur = this.leavesTree.get(this.rClus
								.nextInt(Environment.numCluster));
						this.userItemMap.put(usr,
								new IndItem(cur.getIndexLeaf()));
						GlobalFunction.addValueMap(this.clusterItemLstMap,
								cur.getIndexLeaf(), usr);
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
			cluster.implICML(this.clusterItemLstMap.get(cur.getIndexLeaf()),
					this.userItemMap, i);
			cluster.reset();

			// Update weight for the path
			LinUCB_TREE.backPropagation(cur, cluster.getPayoff(), usr);

			this.rewardTotal += cluster.getPayoff();
			// Draw chart
			this.displayResult(i, this.rewardTotal);
		}
		this.interrupt();
	}

	public boolean isSwitched(int usr) {
		boolean check = false;
		IndItem u = this.userItemMap.get(usr);
		SimpleMatrix userVector = u.getM().invert().mult(u.getB());
		SimpleMatrix avgVector = userVector;
		int clusterIdx = this.userItemMap.get(usr).getClusterIndex();
		int clusterSize = this.clusterItemLstMap.get(clusterIdx).size();
		UCB1 cluster = this.leavesTree.get(clusterIdx);
		int visits = cluster.payoffMap.get(usr).getVisit(), totalVisits = visits;
		double userCB = calConfidenceBound(usr, visits);
		double avgCB = userCB;

		for (int uItem : this.clusterItemLstMap.get(clusterIdx)) {
			if (usr == uItem)
				continue;
			u = this.userItemMap.get(uItem);
			avgVector = avgVector.plus(u.getM().invert().mult(u.getB()));
			visits = cluster.payoffMap.get(uItem).getVisit();
			avgCB += calConfidenceBound(uItem, visits);
			totalVisits += visits;
		}
		CommonOps.scale((double) 1 / clusterSize, avgVector.getMatrix());
		avgCB = avgCB / clusterSize;
		double para = Math.pow(
				(1 + totalVisits) / (1 + Math.log(1 + totalVisits)), 0.5);
		if (this.ed.compute(GlobalFunction.convert2DoubleArr(userVector),
				GlobalFunction.convert2DoubleArr(avgVector)) >  (userCB + avgCB)) {
			check = true;
		}
		return check;
	}

	public double calConfidenceBound(int usr, int times) {
		double val = 0;
		val = Environment.alphaUCB
				* Math.sqrt((1 + Math.log(1 + times)) / (1 + times));
		return val;
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
