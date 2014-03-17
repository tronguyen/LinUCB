package com.smu.linucb.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smu.control.ALGControl;
import com.smu.linucb.global.Environment;

class UserItem {
	private double payoff;
	private int visit;

	public UserItem(double payoff, int visit) {
		this.payoff = payoff;
		this.visit = visit;
	}

	public double getPayoff() {
		return payoff;
	}

	public void setPayoff(double payoff) {
		this.payoff = payoff;
	}

	public int getVisit() {
		return visit;
	}

	public void setVisit(int visit) {
		this.visit = visit;
	}
}

public class UCB1 extends ALGControl {
	public List<UCB1> childLst = new ArrayList<UCB1>();
	public Map<Integer, UserItem> payoffMap; // Key: user, Value: payoff for
												// each
												// node
	public UCB1 pNode = null; // parent node
	public UCB1 cNode = null; // child node
	public LinUCB linucb = null;

	public UCB1(UCB1 pNode) {
		this.pNode = pNode;
		this.pNode.childLst.add(this);
		this.payoffMap = new HashMap<Integer, UserItem>();
	}

	public UCB1() {
		this.payoffMap = new HashMap<Integer, UserItem>();
	}

	// UCB1 algorithm
	public static UCB1 impl(int usr, UCB1 pNode) {
		UCB1 selectedNode = null;
		double maxVal = Double.NEGATIVE_INFINITY;
		double val = 0;
		UserItem usrItem = null;
		for (UCB1 child : pNode.childLst) {
			usrItem = child.payoffMap.get(usr);
			// Create user entry
			if (usrItem == null) {
				usrItem = new UserItem(0, 0);
				child.payoffMap.put(usr, usrItem);
			}
			val = usrItem.getPayoff()
					/ (usrItem.getVisit() + 1)
					+ Environment.alpha
					* Math.sqrt(2
							* Math.log(pNode.payoffMap.get(usr).getVisit() + 1)
							/ (usrItem.getVisit() + 1));
			if (val > maxVal) {
				selectedNode = child;
				maxVal = val;
			}
		}
		return selectedNode;
	}
}
