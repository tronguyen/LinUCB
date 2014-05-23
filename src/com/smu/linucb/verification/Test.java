package com.smu.linucb.verification;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class Test {

	public static void main(String[] args) throws RserveException,
			REXPMismatchException {
		// TODO Auto-generated method stub
		RConnection c = new RConnection();
		REXP x = c.eval("R.version.string");
		System.out.println(x.asString());
	}
}
