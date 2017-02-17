package de.benedikt_werner.sudokusolver;

import java.util.ArrayList;


public class NNLayer {

	public int neuronCount;
	public int inputsCount;
	public ArrayList<NNNeuron> neurons;
	
	public NNLayer(int neuronCount, int inputsCount) {
		this.neuronCount = neuronCount;
		this.inputsCount = inputsCount;
		neurons = new ArrayList<NNNeuron>();
		
		for (int i=0 ; i<neuronCount ; i++) {
			neurons.add(new NNNeuron(inputsCount));
		}
	}

}
