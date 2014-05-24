package com.smu.linucb.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

import com.smu.linucb.global.AlgorithmType;
import com.smu.linucb.global.Environment;
import com.smu.linucb.global.GlobalFunction;

public class LinUCB_KMEAN extends LinUCB {

	private static double rewardTotal = 0;
	private List<Integer> fstTimeUsrLst = new ArrayList<Integer>();
	private List<UCB1> clusterLst = new ArrayList<UCB1>();
	private Map<Integer, IndItem> userItemMap = new HashMap<Integer, IndItem>();
	private Map<Integer, Set<Integer>> clusterItemLstMap = new HashMap<Integer, Set<Integer>>();
	int initClusCount = 0;
	private Random rClus = new Random();
	private EuclideanDistance edd = new EuclideanDistance();

	public LinUCB_KMEAN() {
		this.setAlgType(AlgorithmType.LINUCB_KMEAN);
	}

	@Override
	public void run() {
		int usr;
		UCB1 cur = null;
		// Environment.drChart.genDiffConfig(AlgorithmType.LINUCB_SIN);
		// TODO Auto-generated method stub
		for (int i = 1; i < Environment.limitTime; i++) {
			// Pick user randomly
			usr = Environment.userLst.get(rUSR.nextInt(Environment.userLst
					.size()));

			if (this.fstTimeUsrLst.contains(usr)) {
				// Update user info: M, b matrix
				System.out.println("###User: " + usr + " << "
						+ this.userItemMap.get(usr).getClusterIndex());
				calPayoff(this.clusterLst.get(this.userItemMap.get(usr)
						.getClusterIndex()), usr, i);
				// Reassign to a new cluster
				updateMembership(usr);

			} else {
				this.fstTimeUsrLst.add(usr);
				if (this.initClusCount < Environment.numCluster) {
					cur = new UCB1();
					cur.linucb = new LinUCB();
					cur.setIndexLeaf(this.initClusCount++);
					this.clusterLst.add(cur);

					this.userItemMap.put(usr, new IndItem(cur.getIndexLeaf()));
					GlobalFunction.addValueMap(this.clusterItemLstMap,
							cur.getIndexLeaf(), usr);
					calPayoff(cur, usr, i);
					// Set centroid for K first users
					IndItem u = this.userItemMap.get(usr);
					cur.centroid = u.getM().invert().mult(u.getB());
				} else {
					cur = this.clusterLst.get(this.rClus
							.nextInt(Environment.numCluster));
					this.userItemMap.put(usr, new IndItem(cur.getIndexLeaf()));
					GlobalFunction.addValueMap(this.clusterItemLstMap,
							cur.getIndexLeaf(), usr);
					calPayoff(cur, usr, i);
					updateMembership(usr);
				}
			}
			// Draw chart
			this.displayResult(i, LinUCB_KMEAN.rewardTotal);
		}
		this.interrupt();
	}

	// Update user's membership
	private void updateMembership(int usr) {
		UCB1 chosenCls = null;
		double maxDistance = Double.NEGATIVE_INFINITY, tempDistance;
		IndItem uItem = this.userItemMap.get(usr), u;
		SimpleMatrix uMx = uItem.getM().invert().mult(uItem.getB()), avgVector = uMx;
		for (UCB1 cls : this.clusterLst) {
			tempDistance = this.edd.compute(
					GlobalFunction.convert2DoubleArr(uMx),
					GlobalFunction.convert2DoubleArr(cls.centroid));
			if (tempDistance > maxDistance) {
				maxDistance = tempDistance;
				chosenCls = cls;
			}
		}

		int clusterIdx = chosenCls.getIndexLeaf();

		// Change membership
		int oldIdx = this.userItemMap.get(usr).getClusterIndex();
		GlobalFunction.delValueMap(this.clusterItemLstMap, oldIdx, usr);
		// Update link to new cluster
		this.userItemMap.get(usr).setClusterIndex(clusterIdx);
		GlobalFunction.addValueMap(this.clusterItemLstMap, clusterIdx, usr);
		int clusterSize = this.clusterItemLstMap.get(clusterIdx).size();

		// Update cluster's centroid
		for (int uItemID : this.clusterItemLstMap.get(clusterIdx)) {
			if (usr == uItemID)
				continue;
			u = this.userItemMap.get(uItemID);
			avgVector = avgVector.plus(u.getM().invert().mult(u.getB()));
		}
		CommonOps.scale((double) 1 / clusterSize, avgVector.getMatrix());
		chosenCls.centroid = avgVector;

	}

	private void calPayoff(UCB1 cur, int usr, int times) {
		LinUCB cluster = cur.linucb;
		int clusterIdx = cur.getIndexLeaf();

		cluster.setUser(usr);
		cluster.implICML(this.clusterItemLstMap.get(clusterIdx),
				this.userItemMap, times);
		cluster.reset();
		LinUCB_KMEAN.rewardTotal += cluster.getPayoff();
	}
}
