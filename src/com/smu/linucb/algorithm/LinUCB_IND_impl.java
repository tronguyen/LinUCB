package com.smu.linucb.algorithm;

public class LinUCB_IND_impl extends LinUCB {

	public LinUCB_IND_impl(int usr) {
		super();
		this.setUser(usr);
		// TODO Auto-generated constructor stub
	}

	public void run_nonthread() {
		// create new thread for each user
		this.impl();
		this.reset();
	}

}