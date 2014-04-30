package com.smu.control;

public class RunAVG {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
		    // Execute command
		    String command = "cmd /c start cmd.exe";
		    Process child = Runtime.getRuntime().exec(command);
		    
		} catch (Exception e) {
		}
	}

}
