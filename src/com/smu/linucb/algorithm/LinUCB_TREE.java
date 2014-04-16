package com.smu.linucb.algorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import com.aliasi.cluster.ClusterScore;
import com.smu.control.AlgorithmThreadBuilder;
import com.smu.linucb.global.Environment;
import com.smu.linucb.global.GlobalFunction;

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
		List<Integer> itemOrder;
		int rightBackOrder = 0;
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
					if (Environment.usrReturnMap.containsKey(usr)) {
						rightBackOrder = Environment.usrReturnMap.get(usr).get(
								0) + 1;
						Environment.usrReturnMap.get(usr)
								.set(0, rightBackOrder);
						if (cur.getIndexLeaf() == Environment.errUsrClsMap
								.get(usr)) {
							Environment.usrReturnMap.get(usr).add(
									rightBackOrder);
						}
					} else {
						itemOrder = new ArrayList<Integer>();
						itemOrder.add(1);
						if (cur.getIndexLeaf() == Environment.errUsrClsMap
								.get(usr)) {
							itemOrder.add(1);
						}
						Environment.usrReturnMap.put(usr, itemOrder);

					}
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

			// Tracking user reward
			GlobalFunction.sumValueMap(Environment.trackUserRewardMap, usr,
					cluster.getPayoff());

		}
		// Compare to K-Mean clustering
		// compare2Kmean();
		compare2Origin();
		this.interrupt();
	}

	// Compare only 5% changes (95% fixed class) to original clustering (K-Mean)
	private void compare2Origin() {
		List<Integer> usrItems;
		int usr, cls, right = 0, wrong = 0, itemVal, usrType;
		double rate = 0, usrRW;
		File f = new File("Output4Stats/TrackingUser/Results["
				+ Environment.numCluster + "cls-" + Environment.alphaUCB
				+ "alpha]");
		List<Integer> returnLst;
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));

			for (Iterator<Integer> i = Environment.errUsrClsMap.keySet()
					.iterator(); i.hasNext();) {
				usr = i.next();
				cls = Environment.errUsrClsMap.get(usr);
				returnLst = new ArrayList<Integer>();
				// for (int j = 0; j < usrLst.size(); j++) {
				// Turn back their right cluster
				if (this.userLeafMap.get(usr) == cls) {
					right++;
				} else {
					wrong++;
				}
				usrItems = Environment.usrReturnMap.get(usr);
				rate += (double) (usrItems.size() - 1) / usrItems.get(0);
				bw.write("User: " + usr + " Chances: " + usrItems.get(0) + "|");
				for (int k = 1; k < usrItems.size(); k++) {
					itemVal = usrItems.get(k);
					returnLst.add(itemVal);
					bw.write(" " + itemVal);
				}
				usrType = getTypeUser(returnLst, usrItems.get(0));
				usrRW = Environment.trackUserRewardMap.get(usr);
				bw.write(" |Type: " + usrType + " |Reward: " + usrRW + "\n");
			}
			// Print result comparison
			bw.write("Right back: " + right + "\n");
			bw.write("Wrong back: " + wrong + "\n");
			bw.write("Hit Err-User: " + this.hitBranch + "\n");
			bw.write("Size error: " + Environment.errUsrSet.size() + "\n");
			bw.write("Compare: " + (double) right / (right + wrong) + "\n");
			bw.write("Hit Rate: " + (double) this.hitBranch
					/ Environment.errUsrSet.size() + "\n");
			bw.write("Right Back Rate: " + rate
					/ Environment.errUsrClsMap.keySet().size() + "\n");
			bw.write("Reward: " + this.rewardTotal + "\n");
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get type of user based on list of return-times
	 * Type1: User has right back
	 * Type2: User keeps staying in wrong cluster (maybe refined cluster)
	 * Type3: User wanders around, tries nearly regular-times in each cluster
	 * otherwise, return type0
	 * 
	 * @param returnLst
	 * @return
	 */

	private int getTypeUser(List<Integer> returnLst, int totalReturn) {
		int type = 0, size = returnLst.size() - 1;
		if (size < 0)
			return type;
		double[] val = new double[size];
		double sdValue, meanValue;
		StandardDeviation sd = new StandardDeviation();
		Mean mn = new Mean();
		for (int i = 1; i <= size; i++) {
			val[i - 1] = returnLst.get(i) - returnLst.get(i - 1);
		}
		sdValue = sd.evaluate(val);
		meanValue = mn.evaluate(val);
		if (size >= 2 * Math.floor((double) this.hitBranch
				/ (Environment.errUsrSet.size() * Environment.numCluster))
				&& meanValue < 3) {
			if (sdValue < 2 && returnLst.get(size) == totalReturn) {
				type = 1;
			} else {
				if (returnLst.get(size) == totalReturn) {
					type = getTypeUser(returnLst.subList(2, returnLst.size()),
							totalReturn);
				} else if (totalReturn - returnLst.get(size) <= 3) {
					type = getTypeUser(returnLst.subList(2, returnLst.size()),
							returnLst.get(size));
				}
			}
		} else if ((totalReturn - returnLst.get(size)) / Environment.numCluster >= 2) {
			type = 2;
		} else if (sdValue < 2
				&& Math.abs(meanValue - Environment.numCluster) < 2) {
			type = 3;
		}
		return type;
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
			GlobalFunction.addValueMap(tempMap, this.userLeafMap.get(usr), usr);
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
