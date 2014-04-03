package com.smu.linucb.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import com.smu.control.AlgorithmThreadBuilder;
import com.smu.linucb.algorithm.LinUCB_TREE;
import com.smu.linucb.global.AlgorithmType;
import com.smu.linucb.global.Environment;
import com.smu.linucb.global.GlobalSQLQuery;
import com.smu.linucb.preprocessing.Dbconnection;

/*
 * 1: K-mean to cluster users
 * 2: Apply LinUCB_SIN with fixed clusters
 */
public class TreeFixedCluster extends AlgorithmThreadBuilder {
	private static SimpleKMeans kmean;
	private static Instances dataset;
	private boolean warmStart;

	public TreeFixedCluster(boolean warmStart) {
		this.warmStart = warmStart;
	}

	private static Instance createInstance(List<Integer> bookmarkLst) {
		Instance inst = null;
		Double[] tagVal;
		double[] avgTagVec = new double[Environment.featureSize];
		for (int bm : bookmarkLst) {
			tagVal = Environment.normMatrix.get(bm);
			// Calculate average tags
			for (int i = 0; i < Environment.featureSize; i++) {
				avgTagVec[i] += tagVal[i] / bookmarkLst.size();
			}
		}
		inst = new Instance(1, avgTagVec);
		return inst;
	}

	public static void doCluster() {
		kmean = new SimpleKMeans();
		Instance instance;
		List<Integer> lsTrueBM;
		Attribute attr = null;
		int cluster, user;
		try {
			getKmean().setNumClusters(Environment.numCluster);
			FastVector attrs = new FastVector();
			dataset = new Instances("my_dataset", attrs, 0);
			for (int i = 0; i < Environment.featureSize; i++) {
				attr = new Attribute(String.valueOf(i));
				attrs.addElement(attr);
			}

			// Build instances
			for (int usr : Environment.userLst) {
				lsTrueBM = Dbconnection._getConn().getBookmark4User(
						GlobalSQLQuery.GETBM4USER, usr);
				instance = createInstance(lsTrueBM);
				getDataset().add(instance);
			}
			getKmean().buildClusterer(getDataset());

			// Instances centroids = kmean.getClusterCentroids();
			// for (int i = 0; i < centroids.numInstances(); i++) {
			// System.out.println(centroids.instance(i).dataset()
			// .numInstances());
			// }

			// Put users into their cluster
			for (int i = 0; i < getDataset().numInstances(); i++) {
				// System.out.print((getDataset().instance(i)));
				// System.out.print(" is in cluster ");
				// System.out.print(getKmean().clusterInstance(
				// getDataset().instance(i))
				// + 1 + "\n");
				user = Environment.userLst.get(i);
				cluster = getKmean().clusterInstance(getDataset().instance(i));
				Environment.usrClusterMap.put(user, cluster);
				addSpecMap(Environment.clusterMap, cluster, user);
			}

			// Make noise original data
			genSyntheticData();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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

	public static void genSyntheticData() {
		List<Integer> clusterLst = new ArrayList<Integer>(
				Environment.clusterMap.keySet());
		// Map<Integer, List<Integer>> rmMap = new HashMap<Integer,
		// List<Integer>>();
		Random r = new Random();
		int lenClus = 0, lenRun = 0;
		int chosenUsr, chosenUsrIndex, chosenCls, clsIndex;
		for (int cls : clusterLst) {
			lenClus = (int) (Environment.clusterMap.get(cls).size() * Environment.percentExchange);
			lenRun = lenClus;
			// pick random 5% items
			for (int k = 0; k < lenClus; k++) {
				// pick randomly one user in cluster
				chosenUsrIndex = r.nextInt(lenRun--);
				chosenUsr = Environment.clusterMap.get(cls).get(chosenUsrIndex);
				// pick randomly cluster to push the user in
				clsIndex = r.nextInt(clusterLst.size());
				chosenCls = clusterLst.get(clsIndex);
				if (chosenCls == cls) {
					chosenCls = clusterLst.get((clsIndex + 1)
							% clusterLst.size());
				}
				// Check 5%: Keep track of original status of user
				addSpecMap(Environment.errUsrClsLst, cls, chosenUsr);
				Environment.errUsrSet.add(chosenUsr);
				Environment.clusterMap.get(cls).remove(chosenUsrIndex);
				// addSpecMap(Environment.clusterExtraMap, chosenCls,
				// chosenUsr);
				// addSpecMap(rmMap, cls, chosenUsr);

				// Change user-cluster Map
				Environment.usrClusterMap.put(chosenUsr, chosenCls);
			}
		}
	}

	@Override
	public void run() {
		LinUCB_TREE alg;
		// Enable fixedCluster mode --> true
		alg = new LinUCB_TREE();
		alg.setInClass(this.getInClass());
		if (!this.warmStart) {
			alg.setFixedCluster(true);
			alg.setAlgType(AlgorithmType.LINUCB_VER);
		} else {
			alg.setWarmStart(true);
			alg.setAlgType(AlgorithmType.LINUCB_WARM);
		}
		alg.start();
		this.interrupt();
	}

	public static SimpleKMeans getKmean() {
		return kmean;
	}

	public static Instances getDataset() {
		return dataset;
	}

}
