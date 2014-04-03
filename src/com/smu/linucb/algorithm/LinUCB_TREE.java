package com.smu.linucb.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.aliasi.cluster.ClusterScore;
import com.smu.control.AlgorithmThreadBuilder;
import com.smu.linucb.global.Environment;
import com.smu.linucb.verification.TreeFixedCluster;

public class LinUCB_TREE extends AlgorithmThreadBuilder {
	private double rewardTotal = 0;
	private UCB1 rootTree;
	private List<UCB1> leavesTree = new ArrayList<UCB1>();
	private Set<Integer> fstTimeUsrLst = new HashSet<Integer>();
	private Random rClus = new Random();
	private boolean fixedCluster = false; // fix clusters permanently &
											// temporarily
	private boolean isWarmStart = false;
	private int warmIter;
	private int indexLeaf = 0;
	private Map<Integer, Integer> userLeafMap = new HashMap<Integer, Integer>();
	private int hitBranch = 0;

	public LinUCB_TREE() {
		// super(AlgorithmType.LINUCB_TREE);
		// this.setAlgType(AlgorithmType.LINUCB_TREE);
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
		int index;
		for (int i = 1; i <= Environment.limitTime; i++) {
			// Pick user randomly
			usr = Environment.userLst.get(rUSR.nextInt(Environment.userLst
					.size()));
			if (!this.isFixedCluster()) {
				if (this.fstTimeUsrLst.contains(usr)
						&& (!this.isWarmStart || (this.isWarmStart
								&& Environment.errUsrSet.contains(usr) && this.warmIter > Environment.numWarmIter))) {
					/*
					 * Run UCB1. Find the cluster the user to which belongs
					 */
					cur = this.rootTree;
					while (cur.childLst.size() != 0) {
						cur = UCB1.impl(usr, cur);
					}
					// Increase num of hits (users err-switched)
					this.hitBranch++;
				} else {
					// Select randomly cluster for user having the first time
					// falling
					// down
					this.fstTimeUsrLst.add(usr);
					if (!this.isWarmStart) {
						cur = this.leavesTree.get(this.rClus
								.nextInt(Environment.numCluster));
					} else {
						cur = this.leavesTree.get(Environment.usrClusterMap
								.get(usr));
						this.warmIter++;
					}
				}
			} else {
				cur = this.leavesTree.get(Environment.usrClusterMap.get(usr));
			}
			// Put user into leaf
			this.userLeafMap.put(usr, cur.getIndexLeaf());

			// Run LinUCB for the cluster
			cluster = cur.linucb;
			cluster.setUser(usr);
			cluster.impl();
			cluster.reset();

			// Update weight for the path
			LinUCB_TREE.backPropagation(cur, cluster.getPayoff(), usr);

			this.rewardTotal += cluster.getPayoff();

			// Draw chart
			// this.displayResult(i, this.rewardTotal);
			this.updateRewardMap(this.getInClass(), i, this.rewardTotal);
		}
		// Compare to K-Mean clustering
		// compare2Kmean();
		compare2Origin();
		this.interrupt();
	}

	// Compare only 5% changes (95% fixed class) to original clustering (K-Mean)
	private void compare2Origin() {
		List<Integer> usrLst;
		int cls, right = 0, wrong = 0;
		for (Iterator<Integer> i = Environment.errUsrClsLst.keySet().iterator(); i
				.hasNext();) {
			cls = i.next();
			usrLst = Environment.errUsrClsLst.get(cls);
			for (int j = 0; j < usrLst.size(); j++) {
				// Turn back their right cluster
				if (this.userLeafMap.get(usrLst.get(j)) == cls) {
					right++;
				} else {
					wrong++;
				}
			}
		}
		// Print result comparison
		System.out.println("Right back: " + right);
		System.out.println("Wrong back: " + wrong);
		System.out.println("Hit Err-User: " + this.hitBranch);
		System.out.println("Size error: " + Environment.errUsrSet.size());
		System.out.println("Compare: " + (double) right / (right + wrong));
		System.out.println("Hit Rate: " + (double) this.hitBranch
				/ Environment.errUsrSet.size());
	}

	// Compare B-cube-measured clustering
	private void compare2Kmean() {
		Set<Set<Integer>> referencePartition = new HashSet<Set<Integer>>();
		Set<Set<Integer>> responsePartition = new HashSet<Set<Integer>>();
		for (Iterator<Integer> i = Environment.clusterMap.keySet().iterator(); i
				.hasNext();) {
			referencePartition.add(new HashSet(Environment.clusterMap.get(i
					.next())));
		}

		System.out.println("reference set");
		printMap(Environment.clusterMap);
		Map<Integer, List<Integer>> tempMap = new HashMap<Integer, List<Integer>>();
		// Convert response to clusterMap
		for (Iterator<Integer> i = this.userLeafMap.keySet().iterator(); i
				.hasNext();) {
			int usr = i.next();
			TreeFixedCluster
					.addSpecMap(tempMap, this.userLeafMap.get(usr), usr);
		}

		for (Iterator<Integer> i = tempMap.keySet().iterator(); i.hasNext();) {
			responsePartition.add(new HashSet(tempMap.get(i.next())));
		}
		System.out.println("response set");
		printMap(tempMap);

		ClusterScore<Integer> score = new ClusterScore<Integer>(
				referencePartition, responsePartition);
		System.out.println("\nB-Cubed Measures");
		System.out.println("  Cluster Averaged Precision = "
				+ score.b3ClusterPrecision());
		System.out.println("  Cluster Averaged Recall = "
				+ score.b3ClusterRecall());
		System.out.println("  Cluster Averaged F(1) = " + score.b3ClusterF());
		System.out.println("  Element Averaged Precision = "
				+ score.b3ElementPrecision());
		System.out.println("  Element Averaged Recall = "
				+ score.b3ElementRecall());
		System.out.println("  Element Averaged F(1) = " + score.b3ElementF());
	}

	private void printMap(Map<Integer, List<Integer>> mp) {
		int count = 0;
		for (Iterator<Integer> i = mp.keySet().iterator(); i.hasNext();) {
			int cls = i.next();
			System.out.print("Cluster: " + cls);
			System.out.println(Arrays.toString(mp.get(cls).toArray()));
			count += mp.get(cls).size();
		}
		System.out.println("User Total: " + count);
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
		this.warmIter = 0;
	}
}
