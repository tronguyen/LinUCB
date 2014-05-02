package com.smu.linucb.algorithm;

import java.util.Random;

import com.smu.linucb.global.AlgorithmType;
import com.smu.linucb.global.Environment;

public class LinUCB_SIN extends LinUCB {

	// public static int time = 0;
	private double rewardTotal = 0;

	public LinUCB_SIN() {
		// TODO Auto-generated constructor stub
		this.setAlgType(AlgorithmType.LINUCB_SIN);
	}

	@Override
	public void run() {
//		Environment.drChart.genDiffConfig(AlgorithmType.LINUCB_SIN);
		// TODO Auto-generated method stub
		for (int i = 1; i <= Environment.limitTime; i++) {
			// Pick user randomly
			this.setUser(Environment.userLst.get(rUSR
					.nextInt(Environment.userLst.size())));
			// System.out.println("User: " + this.getUser());
			this.impl();
			this.reset();
			this.rewardTotal += this.getPayoff();
			// Draw chart
//			this.displayResult(i, LinUCB_SIN.rewardTotal);
			this.updateRewardMap(this.getInClass(), i, this.rewardTotal);
		}
		this.interrupt();
	}
}
