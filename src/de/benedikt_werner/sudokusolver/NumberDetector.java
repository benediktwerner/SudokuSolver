package de.benedikt_werner.sudokusolver;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;


public class NumberDetector {

	public static final int blackRGB = Color.BLACK.getRGB();
	public static final int whiteRGB = Color.WHITE.getRGB();
	public static final int trainingRounds = 10000;
	public NeuralNetwork nn;
    public Point[] corners;
	
	public NumberDetector(File trainingDataFile) {
		//Create 2 layer network
		nn = new NeuralNetwork();
		nn.createLayers(
				NeuralNetwork.INPUT_SIZE, 0,
				NeuralNetwork.OUTPUT_SIZE, NeuralNetwork.INPUT_SIZE);
		
		nn.trainBackpropagation(loadTrainingData(trainingDataFile), trainingRounds);
	}
	
	public Integer[][] detectBoard(BufferedImage img) {
		Integer[][] board = new Integer[9][9];
		
		// Clean image
		changeBrightness(img, 2);
		changeContrast(img, 1000);
		removeColors(img);
		removeParticles(img, 80);
		fillGaps(img);
		
		// Remove border
        img = cutToCorners(img);

        if (img == null) return null;

        removeBorder(img);
        removeParticles(img, 100);
		
		// Cut image
		BufferedImage[][] cutImages = cut9x9(img);

		// Remove empty squares
		detectEmpty(cutImages);
		
		// Center and scale images
		for (int x = 0; x < cutImages.length; x++) {
			for (int y = 0; y < cutImages[0].length; y++) {
				if (cutImages[x][y] == null) continue;
				cutImages[x][y] = crop(cutImages[x][y]);
				cutImages[x][y] = addBorder(cutImages[x][y]);
				cutImages[x][y] = scale(cutImages[x][y], 30, 30);
			}
		}
		
		// Detect numbers
		for (int x = 0; x < cutImages.length; x++)
			for (int y = 0; y < cutImages[0].length; y++)
				if (cutImages[x][y] == null)
					board[y][x] = null;
				else
					board[y][x] = nn.compute(convert(cutImages[x][y]));
		
		return board;
	}
	
	public static double[] convert(BufferedImage img) {
		double[] array = new double[img.getHeight() * img.getWidth()];
		
		for (int x = 0; x < img.getWidth(); x++)
			for (int y = 0; y < img.getWidth(); y++)
				array[x + y * img.getWidth()] = img.getRGB(x, y) == whiteRGB ? 0.0 : 1.0;
		
		return array;
	}

    public BufferedImage cutToCorners(BufferedImage img) {
        corners = findCorners(img);

        if (corners == null) return null;

        int x1 = corners[0].x;
        int y1 = corners[0].y;
        int x2 = corners[1].x - corners[0].x;
        int y2 = corners[1].y - corners[0].y;

        for (int i = 10; i >= 0; i--) {
            if (img.getRGB(x1 + i, y1 + i) == blackRGB) {
                x1 += i - 1;
                y1 += i - 1;
                break;
            }
        }

        for (int i = 11; i >= 0; i--) {
            if (img.getRGB(corners[1].x - i, corners[1].y - i) == blackRGB) {
                x2 -= i + 1;
                y2 -= i + 1;
                break;
            }
        }

        return img.getSubimage(x1, y1, x2, y2);
    }

	public Point[] findCorners(BufferedImage img) {
        Point[] corners = new Point[2];
        int[] dist = new int[2];

        int width = img.getWidth();
        int height = img.getHeight();

        for (int i = 0; i < dist.length; i++) {
            dist[i] = Integer.MAX_VALUE;
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
	
	public static BufferedImage addBorder(BufferedImage img) {
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
	
	public static void removeColors(BufferedImage img) {
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
	
	public static void changeBrightness(BufferedImage img, float amount) {
		RescaleOp op = new RescaleOp(amount, 0, null);
		img = op.filter(img, img);
	}
	
	//Helper methods
	
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
	
	public static BufferedImage copyImage(BufferedImage source){
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return b;
	}
	
	public static int diff(int a, int b) {
		return Math.abs(a - b);
	}
	
	private int square(int a) {
		return a * a;
	}
}
