package de.benedikt_werner.sudokusolver;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.UIManager;


public class NumberDetectorTest {
	
	public static int blackRGB = Color.BLACK.getRGB();
	public static int whiteRGB = Color.WHITE.getRGB();

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		BufferedImage img = null;
		BufferedImage[][] cutImages = null;
		Integer[][] board = new Integer[9][9];
		int[][] nn_input = null;
		NeuralNetwork nn = new NeuralNetwork();
		NNTrainingData[] training_data = null;
		
		in.useLocale(Locale.US);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.print(">>> ");
		while (true) {
			if (in.hasNext()) {
				String[] input = in.nextLine().split(" "); 
				
				try {
					switch (input[0]) {
						case "exit": in.close(); System.exit(0); return;
						case "load": 
							if (input[1].equals("image")) img = loadImage(selectFile());
							else if (input[1].equals("training")) training_data = loadTrainingData(selectFile());
							break;
						case "default": img = loadImage(new File("C:/Users/Bene/Desktop/SudokuSolver/sudoku_1.jpg")); break;
						case "contrast": changeContrast(img, Float.parseFloat(input[1])); break;
						case "brightness": changeBrightness(img, Float.parseFloat(input[1])); break;
						case "brightnessAndroid": changeBrightnessAndroid(img, Integer.parseInt(input[1])); break;
						case "clean": cleanColors(img); break;
						case "remove":
							if (input[1].equals("border")) removeBorder(img);
							else if (input[1].equals("particles")) removeParticles(img, Integer.parseInt(input[2]));
							else if(input[1].equals("corners")) img = cutToCorners(img);
							break;
						case "fill": if (input[1].equals("gaps")) fillGaps(img); break;
						case "std": standardClean(img); break;
						case "cut": cutImages = cut9x9(img); break;
						case "detect": if (input[1].equals("empty")) detectEmpty(cutImages); break;
						case "crop":
							for (int x = 0; x < cutImages.length; x++) {
								for (int y = 0; y < cutImages[0].length; y++) {
									cutImages[x][y] = crop(cutImages[x][y]);
								}
							}
							break;
						case "border":
							for (int x = 0; x < cutImages.length; x++) {
								for (int y = 0; y < cutImages[0].length; y++) {
									cutImages[x][y] = addBorder(cutImages[x][y]);
								}
							}
							break;
						case "split": cutImages = split(img); break;
						case "center": center(cutImages); break;
						case "scale":
							for (int x = 0; x < cutImages.length; x++) {
								for (int y = 0; y < cutImages[0].length; y++) {
									cutImages[x][y] = scale(cutImages[x][y], Integer.parseInt(input[1]), Integer.parseInt(input[1]));
								}
							}
							break;
						case "convert": nn_input = convertAll(cutImages); break;
						case "export": export(nn_input); break;
						case "nn":
							switch (input[1]) {
								case "init": initializeNetwork(nn); break;
								case "train": trainNetwork(nn, training_data, Integer.parseInt(input[2])); break;
								case "compute": compute(cutImages[Integer.parseInt(input[2])][Integer.parseInt(input[3])], nn); break;
								case "export": nn_toFile(nn, selectFile()); break;
								case "import": nn.setWeightsFromFile(selectFile()); break;
							}
							break;
						case "corners":
							Point[] corners = findCorners(img);
							System.out.println(corners[0] + ", " + corners[1]);
							showImage(img, corners);
							break;
						case "show":
							if (input.length > 1 && input[1].equals("cut")) showCutImages(cutImages);
							else if (input.length > 1 && input[1].equals("board")) printBoard(board);
							else if (input.length > 1 && input[1].equals("training"))
								for (NNTrainingData data : training_data) {
									System.out.println(data.result + " - " + data.data.length);
								}
							else if (input.length > 2) showImage(cutImages[Integer.parseInt(input[1])][Integer.parseInt(input[2])]);
							else showImage(img);
							break;
						default: System.out.println("Command unknown");
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.print(">>> ");
		}
	}
	
	public static void nn_toFile(NeuralNetwork nn, File file) {
		String[] weights = nn_toString(nn);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			
			for (String s : weights) {
				writer.write(s);
				writer.newLine();
			}
			
			writer.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String[] nn_toString(NeuralNetwork nn) {
		String[] weights = new String[9];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = "";
		}
		
		NNLayer layer = nn.layers.get(1);
		for (int i = 0; i < layer.neuronCount; i++) {
			NNNeuron n = layer.neurons.get(i);
			for (double d : n.weights)
				weights[i] += d + " ";
		}
		
		return weights;
	}
	
	public static void compute(BufferedImage img, NeuralNetwork nn) {
		int[] img_data = convert(img);
		double[] input_data = new double[img_data.length];
		for (int i = 0; i < input_data.length; i++) {
			input_data[i] = img_data[i];
		}
		
		System.out.println(nn.compute(input_data));
	}
	
	public static void trainNetwork(NeuralNetwork nn, NNTrainingData[] training_data, int trainingRounds) {
		nn.trainBackpropagation(training_data, trainingRounds);
	}
	
	public static void initializeNetwork(NeuralNetwork nn) {
		nn.createLayers(
				NeuralNetwork.INPUT_SIZE, 0,
				NeuralNetwork.OUTPUT_SIZE, NeuralNetwork.INPUT_SIZE
				);
	}

	public static int[][] convertAll(BufferedImage[][] cutImages) {
		LinkedList<BufferedImage> list = new LinkedList<BufferedImage>();
		
		for (int x = 0; x < cutImages.length; x++) {
			for (int y = 0; y < cutImages[0].length; y++) {
				if (cutImages[x][y] != null) list.add(cutImages[x][y]);
			}
		}
		
		BufferedImage[] cutImagesArray = list.toArray(new BufferedImage[list.size()]);
		int[][] array = new int[cutImagesArray.length][];
		
		for (int i = 0; i < cutImagesArray.length; i++) {
			array[i] = convert(cutImagesArray[i]);
		}
		
		return array;
	}
	
	public static int[] convert(BufferedImage img) {
		int[] array = new int[img.getHeight() * img.getWidth()];
		
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getWidth(); y++) {
				array[x + y * img.getWidth()] = img.getRGB(x, y) == whiteRGB ? 0 : 1;
			}
		}
		
		return array;
	}
	
	public static BufferedImage cutToCorners(BufferedImage img) {
        Point[] c = findCorners(img);

        return img.getSubimage(c[0].x + 10, c[0].y + 10, c[1].x - c[0].x - 10, c[1].y - c[0].y - 10);
	}
	
	public static Point[] findCorners(BufferedImage img) {
		Point[] corners = new Point[2];
        int[] dist = new int[2];

        int width = img.getWidth();
        int height = img.getHeight();

        for (int i = 0; i < dist.length; i++) {
            dist[i] = (int) Math.pow(Math.min(width, height) / 10, 2);
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (img.getRGB(x, y) == blackRGB) {
                    if (square(x) + square(y) < dist[0]) {
                        corners[0] = new Point(x, y);
                        dist[0] = square(x) + square(y);
                    }
                    else if (square(width - x) + square(height - y) < dist[1]) {
                        corners[1] = new Point(x, y);
                        dist[1] = square(width - x) + square(height - y);
                    }
                }
            }
        }

        for (Point corner : corners) {
            if (corner == null) return null;
        }

        return corners;
	}
	
	public static void center(BufferedImage[][] cutImages) {
		
		for (int x = 0; x < cutImages.length; x++) {
			for (int y = 0; y < cutImages[0].length; y++) {
				cutImages[x][y] = crop(cutImages[x][y]);
				cutImages[x][y] = addBorder(cutImages[x][y]);
			}
		}
	}

	public static BufferedImage[][] split(BufferedImage img) {
		BufferedImage[][] cutImages = cut9x9(img);
		
		detectEmpty(cutImages);
		center(cutImages);
		
		return cutImages;
	}

	public static void detectEmpty(BufferedImage[][] cutImages) {
		for (int x = 0; x < cutImages.length; x++) {
			for (int y = 0; y < cutImages[0].length; y++) {
				BufferedImage img = cutImages[x][y];
				boolean blackFound = false;
				
				for (int x2 = 0; x2 < img.getWidth(); x2++) {
					for (int y2 = 0; y2 < img.getHeight(); y2++) {
						if (img.getRGB(x2, y2) == blackRGB) {
							blackFound = true;
							break;
						}
					}
					
					if (blackFound) break;
				}
				
				if (!blackFound)
					cutImages[x][y] = null;
			}
		}
	}
	
	public static BufferedImage addBorder(BufferedImage img) {
		if (img == null)
			return null;

		int width = img.getWidth();
		int height = img.getHeight();
		
		int size = Math.max(width, height);
		
		BufferedImage tmp = new BufferedImage(size, size, BufferedImage.TYPE_3BYTE_BGR);
		int x = (int) Math.floor((size - width) / 2);
		int y = (int) Math.floor((size - height) / 2);
		
		Graphics g = tmp.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, size, size);
		g.drawImage(img, x, y, width, height, null);
		g.dispose();
		
		return tmp;
	}
	
	public static BufferedImage crop(BufferedImage img) {
		if (img == null)
			return null;
		
		int minX = img.getWidth();
		int minY = img.getHeight();
		int maxX = 0;
		int maxY = 0;
		
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				if (img.getRGB(x, y) == blackRGB) {
					if (x < minX)
						minX = x;
					else if (x > maxX)
						maxX = x;
					
					if (y < minY)
						minY = y;
					else if (y > maxY)
						maxY = y;
				}
			}	
		}

		return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}
	
	public static BufferedImage[][] cut9x9(BufferedImage img) {
		BufferedImage[][] cutImages = new BufferedImage[9][9];
		int width = img.getWidth() / 9;
		int height = img.getHeight() / 9;
		
		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 9; y++) {
				cutImages[x][y] = img.getSubimage(x * width, y * height, width, height);
			}
		}
		
		return cutImages;
	}
	
	public static void standardClean(BufferedImage img) {
		System.out.print("Adjusting Brightness... ");
		changeBrightness(img, 2);
		System.out.println("Done");

		System.out.print("Adjusting Contrast... ");
		changeContrast(img, 1000);
		System.out.println("Done");
		
		System.out.print("Cleaning Colors... ");
		cleanColors(img);
		System.out.println("Done");
		
		System.out.print("Removing particles... ");
		removeParticles(img, 70);
		System.out.println("Done");

		System.out.print("Filling gaps... ");
		fillGaps(img);
		System.out.println("Done");
	}
	
	public static void cleanColors(BufferedImage img) {
		for (int x = 0; x < img.getWidth(); x++){
			for (int y = 0; y < img.getHeight(); y++){
			    Color pixel = new Color(img.getRGB(x, y));

			    int r = pixel.getRed();
			    int g = pixel.getGreen();
			    int b = pixel.getBlue();
			    
			    int avgValue = (r + g + b) / 3;
			    
			    if (avgValue < 200 || diff(avgValue, r) > 15 || diff(avgValue, g) > 15 || diff(avgValue, b) > 15)
			    	img.setRGB(x, y, blackRGB);
			    else
			    	img.setRGB(x, y, whiteRGB);
			}
		}
	}
	
	public static void fillGaps(BufferedImage img) {
		int width = img.getWidth(); int height = img.getHeight();
		
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				
				if (img.getRGB(x, y) == whiteRGB) {
					
					int blackNeighbors = 0;
					if (x != 0 && img.getRGB(x - 1, y) == blackRGB)
						blackNeighbors++;
					if (y != 0 && img.getRGB(x, y - 1) == blackRGB)
						blackNeighbors++;
					if (x != width - 1 && img.getRGB(x + 1, y) == blackRGB)
						blackNeighbors++;
					if (y != height - 1 && img.getRGB(x, y + 1) == blackRGB)
						blackNeighbors++;
					
					if (blackNeighbors > 2)
						img.setRGB(x, y, blackRGB);
				}
			}
		}
	}
	
	public static void removeParticles(BufferedImage image, int maxSize) {
		BufferedImage img = copyImage(image);
		int width = img.getWidth(); int height = img.getHeight();
		
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				if (img.getRGB(x, y) == blackRGB) {		// Found black pixel
					
					//Check size of black area
					LinkedList<Point> stack = new LinkedList<Point>();
					LinkedList<Point> area = new LinkedList<Point>();
					stack.push(new Point(x, y));
					
					while (!stack.isEmpty()) {
						Point p = stack.pop();
						img.setRGB(p.x, p.y, whiteRGB);
						area.add(p);
						
						if (p.x != 0 && img.getRGB(p.x - 1, p.y) == blackRGB)
							stack.push(new Point(p.x - 1, p.y));
						if (p.y != 0 && img.getRGB(p.x, p.y - 1) == blackRGB)
							stack.push(new Point(p.x, p.y - 1));
						if (p.x != width - 1 && img.getRGB(p.x + 1, p.y) == blackRGB)
							stack.push(new Point(p.x + 1, p.y));
						if (p.y != height - 1 && img.getRGB(p.x, p.y + 1) == blackRGB)
							stack.push(new Point(p.x, p.y + 1));
					}
					
					// If area is small, remove it
					if (area.size() < maxSize) {
						for (Point p : area) {
							image.setRGB(p.x, p.y, whiteRGB);
						}
					}
				}
			}
		}
	}
	
	public static void removeBorder(BufferedImage img) {
		if (img.getRGB(0, 0) == whiteRGB) {
			return;
		}
		
		LinkedList<Point> stack = new LinkedList<Point>();
		stack.push(new Point(0, 0));
		
		int width = img.getWidth(); int height = img.getHeight();
		
		while (!stack.isEmpty()) {
			Point p = stack.pop();
			img.setRGB(p.x, p.y, whiteRGB);
			
			if (p.x != 0 && img.getRGB(p.x - 1, p.y) == blackRGB)
				stack.push(new Point(p.x - 1, p.y));
			if (p.y != 0 && img.getRGB(p.x, p.y - 1) == blackRGB)
				stack.push(new Point(p.x, p.y - 1));
			if (p.x != width - 1 && img.getRGB(p.x + 1, p.y) == blackRGB)
				stack.push(new Point(p.x + 1, p.y));
			if (p.y != height - 1 && img.getRGB(p.x, p.y + 1) == blackRGB)
				stack.push(new Point(p.x, p.y + 1));
		}
	}
	
	public static void changeBrightness(BufferedImage img, float amount) {
		RescaleOp op = new RescaleOp(amount, 0, null);
		img = op.filter(img, img);
	}
	
	public static void changeBrightnessAndroid(BufferedImage img, float value) {
        // image size
        int width = img.getWidth();
        int height = img.getHeight();
        // color information
        int A, R, G, B;
        Color pixel;

        // scan through all pixels
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = new Color(img.getRGB(x, y));
                A = pixel.getAlpha();
			    R = pixel.getRed();
			    G = pixel.getGreen();
			    B = pixel.getBlue();

                // increase/decrease each channel
                R *= value;
                if (R > 255) { R = 255; }
                else if (R < 0) { R = 0; }

                G *= value;
                if (G > 255) { G = 255; }
                else if (G < 0) { G = 0; }

                B *= value;
                if (B > 255) { B = 255; }
                else if (B < 0) { B = 0; }

                // apply new pixel color to output bitmap
                img.setRGB(x, y, new Color(R, G, B, A).getRGB());
            }
        }
    }
	
	public static void changeContrast(BufferedImage img, float amount) {
		float value = (255.0f + amount) / 255.0f;
		value *= value;
		
		for (int x = 0; x < img.getWidth(); x++){
			for (int y = 0; y < img.getHeight(); y++){
			    Color pixel = new Color(img.getRGB(x, y));
			    int r = pixel.getRed();
			    int g = pixel.getGreen();
			    int b = pixel.getBlue();
	
			    float red = r / 255.0f;
			    float green = g / 255.0f;
			    float blue = b / 255.0f;
	
			    red = (((red - 0.5f) * value) + 0.5f) * 255.0f;
			    green = (((green - 0.5f) * value) + 0.5f) * 255.0f;
			    blue = (((blue - 0.5f) * value) + 0.5f) * 255.0f;
	
			    int iR = (int)red;
			    iR = iR > 255 ? 255 : iR;
			    iR = iR < 0 ? 0 : iR;
			    int iG = (int)green;
			    iG = iG > 255 ? 255 : iG;
			    iG = iG < 0 ? 0 : iG;
			    int iB = (int)blue;
			    iB = iB > 255 ? 255 : iB;
			    iB = iB < 0 ? 0 : iB;

			    Color newPixel = new Color(iR, iG, iB);
			    img.setRGB(x, y, newPixel.getRGB());
			}
		}
	}
	
	public static void showCutImages(BufferedImage[][] cutImages) {
		JDialog dialog = new JDialog();
		dialog.setAlwaysOnTop(true);
		ImagePanel label = new ImagePanel(cutImages);
		dialog.add(label);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	public static void showImage(BufferedImage img) {
		JDialog dialog = new JDialog();
		dialog.setAlwaysOnTop(true);
		ImagePanel label = new ImagePanel(img);
		dialog.add(label);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	public static void showImage(BufferedImage img, Point[] corners) {
		JDialog dialog = new JDialog();
		dialog.setAlwaysOnTop(true);
		ImagePanel label = new ImagePanel(img, corners);
		dialog.add(label);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	public static void printBoard(Integer[][] board) {
        for (int y = 0; y < board.length; y++) {
    		String line = "";
            for (int x = 0; x < board[0].length; x++) {
            	line += board[y][x] != null ? board[y][x].toString() : " ";
				if (x == 2 || x == 5) line += " | ";
            }
            
            System.out.println(line);
			if (y == 2 || y == 5) System.out.println("---------------\n");
        }
    }
	
	public static File selectFile() {
		JFileChooser fileChooser = new JFileChooser();
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			return fileChooser.getSelectedFile();
		}
		return null;
	}
	
	public static BufferedImage loadImage(File file) {
		if (file != null && file.isFile()) {
			try {
				return ImageIO.read(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private static NNTrainingData[] loadTrainingData(File file) {
		NNTrainingData[] data = null;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			String line = reader.readLine();
			LinkedList<NNTrainingData> list = new LinkedList<NNTrainingData>();
			
			while (line != null && !line.equals("")) {
				int result = Integer.parseInt(line.substring(0, 1));
				
				String[] line_array = line.substring(2, line.length() - 1).split(" ");
				double[] array = new double[line_array.length - 1];
				
				for (int i = 0; i < array.length; i++) {
					array[i] = Double.parseDouble(line_array[i + 1]);
				}
				
				list.add(new NNTrainingData(array, result));
				
				line = reader.readLine();
			}
			
			reader.close();
			
			data = list.toArray(new NNTrainingData[list.size()]);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return data;
	}
	
	public static void export(int[][] data) {
		JFileChooser fileChooser = new JFileChooser();
		int returnValue = fileChooser.showSaveDialog(null);
		
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				
				for (int[] is : data) {
					for (int i : is) {
						writer.write(i + " ");
					}
					writer.newLine();
				}
				
				writer.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static BufferedImage scale(BufferedImage img, int dWidth, int dHeight) {
	    BufferedImage scaledImage = null;
	    
	    if(img != null) {
	    	double fWidth = (double) dWidth / img.getWidth();
	    	double fHeight = (double) dHeight / img.getHeight();
	    	
	        scaledImage = new BufferedImage(dWidth, dHeight, BufferedImage.TYPE_3BYTE_BGR);
	        Graphics2D g = scaledImage.createGraphics();
	        AffineTransform at = AffineTransform.getScaleInstance(fWidth, fHeight);
	        g.drawRenderedImage(img, at);
	    }
	    
	    return scaledImage;
	}
	
	public static BufferedImage copyImage(BufferedImage source){
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return b;
	}
	
	public static int square(int x) {
		return x * x;
	}
	
	public static int diff(int a, int b) {
		return Math.abs(a - b);
	}
}
