package de.benedikt_werner.sudokusolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;


public class NeuralNetwork {

	public static final int INPUT_SIZE = 900;
	public static final int OUTPUT_SIZE = 9;
	public static final double LEARNING_RATE = 0.1;
	public Random random = new Random();
	public ArrayList<NNLayer> layers;

	// Parameter for every layer: neurons per layer, inputs per neuron
	public void createLayers(int... n) {
		layers = new ArrayList<NNLayer>();
		
		for (int i = 0; i < n.length / 2; i++) {
			layers.add(new NNLayer(n[2 * i], n[2 * i + 1]));
		}
	}
	
	public int compute(double[] input_data) {
		input(input_data);
		
		calculateCompleteNetwork();

		double maxValue = 0;
		int maxOutput = 0;
		
		for (int i = 0; i < OUTPUT_SIZE; i++) {
			double result = layers.get(layers.size() - 1).neurons.get(i).output;
			System.out.println("Output " + (i + 1) + ": " + result);
			
			if (result > maxValue) {
				maxValue = result;
				maxOutput = i + 1;
			}
		}
		
		System.out.println("Most probable answer: " + maxOutput);
		
		return maxOutput;
	}

	public void setWeightsFromFile(File file) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
		
			for (int i = 0; i < 9; i++) {
				layers.get(1).neurons.get(i).weights = generateWeightsFromString(reader.readLine());
			}
			
			reader.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public double[] generateWeightsFromString(String s) {
		String[] array = s.split(" ");
		double[] result = new double[array.length];
		
		for (int i = 0; i < array.length; i++) {
			result[i] = Double.parseDouble(array[i]);
		}
		
		return result;
	}

	public void setWeightsRandom() {
		for (NNLayer layer : layers) {
			for (NNNeuron neuron : layer.neurons) {
				for (int i = 0; i < neuron.inputCount; i++) {
					neuron.weights[i] = random.nextDouble();
				}
			}
		}
	}

	public void calculateCompleteNetwork() {
		for (int l = 0; l < layers.size(); l++) {
			NNLayer layer = layers.get(l);
			
			for (int n = 0; n < layer.neuronCount; n++) {
				NNNeuron neuron = layer.neurons.get(n);
				
				if (l > 0) {
					NNLayer previousLayer = layers.get(l - 1);
					
					for (int pn = 0; pn < previousLayer.neuronCount; pn++) {
						NNNeuron previousNeuron = previousLayer.neurons.get(pn);
						neuron.inputs[pn] = previousNeuron.output;
					}
				}
				neuron.calculateOutput();
			}
		}
	}

	public void trainBackpropagation(NNTrainingData[] training_data, int trainingRounds) {
		setWeightsRandom();

		double[] example = new double[INPUT_SIZE];
		int number = 0;
		
		for (int round = 0; round < trainingRounds; round++) {
			number = getRandomTrainingExample(training_data, example);
			input(example);

			calculateCompleteNetwork();

			for (int l = layers.size() - 1; l > 0; l--) {
				NNLayer layer = layers.get(l);
				
				for (int n = 0; n < layer.neuronCount; n++) {
					NNNeuron neuron = layer.neurons.get(n);
					
					
					for (int pn = 0; pn < layers.get(l - 1).neuronCount; pn++) {
						if (l == layers.size() - 1) {
							double expectedResult = (n + 1 == number) ? 1.0 : 0.0;
							double currentResult = neuron.output;
							neuron.deltaBackpropagation = fDerivative(neuron.weightedInputs) * (expectedResult - currentResult);
						}
						else {
							double deltaSumme = 0.0;
							
							for (int nn = 0; nn < layers.get(l + 1).neuronCount; nn++) {
								NNNeuron nextNeuron = layers.get(l + 1).neurons.get(nn);
								deltaSumme += nextNeuron.deltaBackpropagation * nextNeuron.weights[n];
							}
							
							neuron.deltaBackpropagation = fDerivative(neuron.weightedInputs) * deltaSumme;
						}
						neuron.weights[pn] += LEARNING_RATE * neuron.inputs[pn] * neuron.deltaBackpropagation;
					}
				}
			}
		}
	}

	public int getRandomTrainingExample(NNTrainingData[] training_data, double[] result) {
		int number = random.nextInt(training_data.length);
		double[] example = training_data[number].data;
		
		for (int i = 0; i < INPUT_SIZE; i++)
			result[i] = example[i];
		
		return training_data[number].result;
	}

	public void input(double[] example) {
		for (int n = 0; n < INPUT_SIZE; n++) {
			NNNeuron inputNeuron = layers.get(0).neurons.get(n);
			inputNeuron.inputs[0] = example[n];
			inputNeuron.calculateOutput();
		}
	}
	
	public static double fDerivative(double x) {
		return 1.0;
	}
}
