package de.benedikt_werner.sudokusolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;


public class Boot {
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		JOptionPane.showMessageDialog(null, "Select trainging data!");
		NumberDetector numberDetector = new NumberDetector(selectFile());

		JOptionPane.showMessageDialog(null, "Select sudoku!");
		File file = selectFile();
		SudokuBoard board = new SudokuBoard();
		try {
			board.setBoard(numberDetector.detectBoard(ImageIO.read(file)));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	    
	    print("Solving: \n" + board.asString());
	    
	    SudokuSolver solver = new SudokuSolver();
	    
	    if (solver.findSolution(board))
	    	print("Sudoku solved!\n");
		else
			print("Sudoku could not get solved!\n");
	    
		print(board.asString());
	}
	
	public static String readFromFile(File file) {
		String sudoku = "";
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
		
			String line;
			for (int i = 0; i < 9; i++) {
				line = reader.readLine();
				sudoku += line;
			}
			reader.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return sudoku;
	}
	
	public static File selectFile() {
		JFileChooser fileChooser = new JFileChooser();
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			return fileChooser.getSelectedFile();
		}
		return null;
	}

	public static void print(String s) {
		System.out.println(s);
	}
}
