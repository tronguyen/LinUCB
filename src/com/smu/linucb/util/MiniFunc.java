package com.smu.linucb.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.smu.linucb.global.Environment;
import com.smu.linucb.global.GlobalFunction;

public class MiniFunc {

	/**
	 * @param args
	 */
	BufferedReader br;
	BufferedWriter bw;
	String s = "";
	String[] line;
	Map<Integer, Double> res = new TreeMap<Integer, Double>();
	int iter;

	public void mergeFile() throws IOException {
		File f = new File(Environment.RW2FILE);
		File[] fLst = f.listFiles();
		for (File fin : fLst) {
			br = new BufferedReader(new FileReader(fin));
			while ((s = br.readLine()) != null) {
				line = s.split("\\|");
				GlobalFunction.sumValueMap(res, Integer.valueOf(line[0]),
						Double.valueOf(line[1]));
			}
			br.close();
		}
		bw = new BufferedWriter(new FileWriter(new File(Environment.RW2FILE
				+ "_merged")));
		for (Iterator<Integer> it = res.keySet().iterator(); it.hasNext();) {
			iter = it.next();
			bw.write(iter + "|" + res.get(iter) / Environment.numAvgLoop + "\n");
		}
		bw.flush();
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			new MiniFunc().mergeFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
