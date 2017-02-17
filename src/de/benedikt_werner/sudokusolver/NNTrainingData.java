package de.benedikt_werner.sudokusolver;


public class NNTrainingData {

	public double[] data;
	public int result;
	
	public NNTrainingData(double[] data, int result) {
		this.data = data;
		this.result = result;
	}
}
