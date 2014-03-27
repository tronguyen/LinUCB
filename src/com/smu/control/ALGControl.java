package com.smu.control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

import org.jfree.ui.RefineryUtilities;

import com.smu.linucb.algorithm.LinUCB_IND;
import com.smu.linucb.algorithm.LinUCB_SIN;
import com.smu.linucb.algorithm.LinUCB_TREE;
import com.smu.linucb.global.AlgorithmType;
import com.smu.linucb.global.Environment;
import com.smu.linucb.global.GlobalSQLQuery;
import com.smu.linucb.pca.PrincipleComponentAnalysis;
import com.smu.linucb.preprocessing.Dbconnection;
import com.smu.linucb.preprocessing.Preprocessing;
import com.smu.linucb.verification.TreeFixedCluster;

public class ALGControl extends Thread {

	static File fMatrix = new File("norm_matrix_ejml_full");
	protected Random rBM;
	protected Random rUSR;
	private AlgorithmType algType;

	public ALGControl() {
		this.rUSR = new Random(System.nanoTime()
				* Thread.currentThread().getId());
		// this.rUSR.setSeed((long) (this.rUSR.nextInt() * Math.random() * 10));
		this.rBM = new Random(System.nanoTime()
				* Thread.currentThread().getId());
		// this.rBM.setSeed((long) (this.rBM.nextInt() * Math.random() * 10));
	}

	public void displayResult(int count, double reward) {
		// TODO Auto-generated method stub
		int buffSize = 10;
		if ((count % buffSize) == 0) {
			// System.out.println("\n\nRound: " + count);
			// System.out.println("=========" + reward + "============\n\n");
			Environment.drChart.addData(this.algType, count, reward);
		}
	}

	protected AlgorithmType getAlgType() {
		return algType;
	}

	public void setAlgType(AlgorithmType algType) {
		this.algType = algType;
	}

	/**
	 * @param args
	 */
	public static void readMatrix() {
		String line = "";
		String[] arrStr;
		int bmid;
		Double[] tagVals;
		try {
			BufferedReader br = new BufferedReader(new FileReader(fMatrix));
			while ((line = br.readLine()) != null) {
				arrStr = line.split(",");
				bmid = Integer.parseInt(arrStr[0]);
				tagVals = new Double[Environment.featureSize];
				for (int i = 0; i < arrStr.length - 1; i++) {
					tagVals[i] = Double.parseDouble(arrStr[i + 1]);
				}
				Environment.normMatrix.put(bmid, tagVals);
				Environment.bmidLst.add(bmid);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void initData4LinUCB(Preprocessing pr) {
		// Preprocessing pr = new Preprocessing();
		Dbconnection dbconn = Dbconnection._getConn();
		try {
			// Build tag set for counting
			pr.buildTags(dbconn.getResultSet(GlobalSQLQuery.GETVIEWTAG));
			pr.sensorToken();

			// Build tags for each bookmark
			pr.buildBookmark_Tags(dbconn
					.getResultSet(GlobalSQLQuery.GETBOOKMARK_TAG));

			// Build user list
			// pr.buildUserList(dbconn.getResultSet(GlobalSQLQuery.GETUSER));

			// Print statistical data
			System.out.println("Size tags: "
					+ Environment.hm_token_weight.size());
			System.out.println("Size bookmark-tags: "
					+ Environment.hm_bookmark_tag.size());

			pr.buildTagChecking(dbconn.getResultSet(GlobalSQLQuery.GETTAG));
			System.out.println("Size true-tags: " + Environment.tagSet.size());

			// Calculate TF-IDF & normalization
			pr.calTF_IDF();

			// pr.writeBookmark_Tags(new File("Output4Stats/matrix"));
			// writeBookmark_Tags_ARFF(new File("matrix_arff.arff"));
			// writeBookmark_Tags_Matlab(new File("matrix_matlab_try_1"), new
			// File(
			// "bm_index_try_1"));
		} catch (SQLException sqlEx) {
			System.out.println("SQL exception...");
		}
	}

	public static void executePCA(Preprocessing pr) throws Exception {
		// Using PCA from EJML
		// PrincipleComponentAnalysis pca = new PrincipleComponentAnalysis();
		// pca.setup(Environment.hm_bookmark_tag.size(),
		// Environment.hm_token_weight.size());
		// BufferedReader br = new BufferedReader(new FileReader(
		// new File("matrix")));
		// String s = "";
		// String[] arrStr;
		// double[] nums = null;
		// while ((s = br.readLine()) != null) {
		// arrStr = s.split("\t");
		// nums = new double[arrStr.length - 1];
		// for (int i = 0; i < nums.length; i++) {
		// nums[i] = Double.parseDouble(arrStr[i + 1]);
		// }
		// pca.addSample(nums);
		// }
		// pca.computeBasis(Environment.featureSize);
		// System.out.println(pca.sampleToEigenSpace(nums));

		PrincipleComponentAnalysis pca = new PrincipleComponentAnalysis();
		// int bm_num = 68479;
		// int tg_num = 11619;
		pca.setup(Environment.hm_bookmark_tag.size(),
				Environment.hm_token_weight.size());
		File mxIN = new File("Output4Stats/matrix");
		File mxOUT = new File("Output4Stats/norm_matrix_ejml");
		BufferedReader br = new BufferedReader(new FileReader(mxIN));
		String s = "";
		String[] arrStr;
		double[] nums = null;
		System.out.println("Start reading matrix...");
		while ((s = br.readLine()) != null) {
			arrStr = s.split("\t");
			nums = new double[arrStr.length - 1];
			for (int i = 0; i < nums.length; i++) {
				nums[i] = Double.parseDouble(arrStr[i + 1]);
			}
			pca.addSample(nums);
		}
		br.close();

		System.out.println("Start PCA...");
		pca.computeBasis(Environment.featureSize);

		// Write down to file
		System.out.println("Start writing norm matrix...");
		pr.writeNormMatrix(pca, mxIN, mxOUT);
	}

	public static ALGControl factoryInstanceAlg(AlgorithmType type) {
		ALGControl alg = null;
		switch (type) {
		case LINUCB_SIN:
			alg = new LinUCB_SIN();
			Environment.drChart.genDiffConfig(AlgorithmType.LINUCB_SIN);
			break;
		case LINUCB_IND:
			alg = new LinUCB_IND();
			Environment.drChart.genDiffConfig(AlgorithmType.LINUCB_IND);
			break;
		case LINUCB_TREE:
			alg = new LinUCB_TREE();
			Environment.drChart.genDiffConfig(AlgorithmType.LINUCB_TREE);
			break;
		case LINUCB_VER:
			alg = new TreeFixedCluster(false);
			Environment.drChart.genDiffConfig(AlgorithmType.LINUCB_VER);
			break;
		case LINUCB_WARM:
			alg = new TreeFixedCluster(true);
			Environment.drChart.genDiffConfig(AlgorithmType.LINUCB_WARM);
			break;
		}
		return alg;
	}

	@Override
	public void run() {
	};

	public static void main(String[] args) throws SQLException {
		// TODO Auto-generated method stub
		Preprocessing pr = new Preprocessing();
		// Init data for PCA
		ALGControl.initData4LinUCB(pr);
		// Get users from db
		pr.buildUserList(Dbconnection._getConn().getResultSet(
				GlobalSQLQuery.GETUSER));
		// Read norm matrix from file outside
		ALGControl.readMatrix();

		// Plot graph
		Environment.drChart.pack();
		RefineryUtilities.centerFrameOnScreen(Environment.drChart);
		Environment.drChart.setVisible(true);

		ALGControl alg;
		// Running LinSIN
//		alg = ALGControl.factoryInstanceAlg(AlgorithmType.LINUCB_SIN);
//		alg.start();

		// Running LinIND
//		alg = ALGControl.factoryInstanceAlg(AlgorithmType.LINUCB_IND);
//		alg.start();

		// Run LinUCBTREE
//		alg = ALGControl.factoryInstanceAlg(AlgorithmType.LINUCB_TREE);
//		alg.start();

		// Running verification && Warmstart
		TreeFixedCluster.doCluster();
//		alg = ALGControl.factoryInstanceAlg(AlgorithmType.LINUCB_VER);
//		alg.start();

		alg = ALGControl.factoryInstanceAlg(AlgorithmType.LINUCB_WARM);
		alg.start();
	}
}
