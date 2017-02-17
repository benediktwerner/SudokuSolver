package de.benedikt_werner.sudokusolver;

public class NNNeuron
{
	public int inputCount;
	public double[] weights;
	public double[] inputs;
	public double output;
	public double weightedInputs;
	public double deltaBackpropagation;

	public NNNeuron(int inputCount) {
		this.inputCount = inputCount;

		if (inputCount > 0) {
			weights = new double[inputCount];
			inputs = new double[inputCount];
		}
		else {
			weights = null;
			inputs = new double[1];
		}
	}

	public void calculateOutput() {
		weightedInputs = 0.0;
		
		if (inputCount > 0) {
			for (int i = 0; i < inputCount; i++) {
				weightedInputs += weights[i] * inputs[i];
			}
			weightedInputs /= inputCount;
		}
		else {
			weightedInputs = inputs[0];
		}

		output = f(weightedInputs);
	}

	public static double f(double x) {
		return x;
	}
}
