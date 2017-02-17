package de.benedikt_werner.sudokusolver;

import java.util.List;

public class SudokuSolver {
    private static final int SIZE = 9;

    public boolean findSolution(SudokuBoard board) {
        return findSolution(board, 0);
    }

    private boolean findSolution(SudokuBoard board, int cellIndex) {
        if (cellIndex == SIZE * SIZE) return true;
        int row = cellIndex / SIZE, column = cellIndex % SIZE;

        if (board.isFilled(row, column)) return findSolution(board, cellIndex + 1);

        List<Integer> optionsForCell = board.getOptionsForCell(row, column);
        for (Integer option : optionsForCell) {
            board.setCellValue(row, column, option);
            if (findSolution(board, cellIndex+1)) return true;
        }
        board.clearCell(row, column);
        return false;
    }
}