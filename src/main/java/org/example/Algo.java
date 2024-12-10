package org.example;

public class Algo{
    public static void main(String[] args) {
        int GRIDSIZE = 8;
        int MOVES = GRIDSIZE*GRIDSIZE - 1;

        // String inputDirectionCommand = "*********************************"; //for 6x6
        // String inputDirectionCommand = "**********************************************";
        // String inputDirectionCommand = "******";
        String inputDirectionCommand = "*****DR******R******R********************R*D************L******";


        if (inputDirectionCommand.length() != MOVES){
            System.out.print("Invalid input!\n");
            System.out.print("Please insert " + MOVES + " characters totally!\n");
            System.out.print("Insert " + (MOVES - inputDirectionCommand.length()) + " more characters");
            return;
        }

        grid myGrid = new grid(GRIDSIZE, inputDirectionCommand);
        long startTime = System.currentTimeMillis();
        myGrid.findTotalPath(0, 0, 0);
        long endTime = System.currentTimeMillis();

        System.out.print("Total paths: " + myGrid.totalPaths + "\n");
        System.out.print("Total time: " + (endTime - startTime) + "ms");
    }
}


class grid {
    int gridSize;
    boolean[][] visitedCell; // Tracks visited cells
    int[][] directionArray = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Up, Down, Left, Right
    char[] directionChar;
    long totalPaths = 0;
    int[] shortestDistance2Target;

    public grid(int size, String directionString) { // Constructor
        this.gridSize = size;
        this.visitedCell = new boolean[gridSize][gridSize];
        this.directionChar = directionString.toCharArray();
        initShortestDistances(); // Precompute distance at initialization
    }

    private void initShortestDistances() {
        this.shortestDistance2Target = precomputeShortestDistance2Target();
    }

    private boolean isValidMove(int row, int col){
        return (row >= 0 && row < gridSize //row within bounds
                && col < gridSize && col >= 0 //col within bounds
                && !visitedCell[row][col]); //the visited cell must be also false
    }

    private int[] precomputeShortestDistance2Target() {
        // Precompute and store the shortest distance to the target cell
        int[] distances = new int[gridSize * gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int index = i * gridSize + j;
                distances[index] = Math.abs(i - (gridSize - 1)) + j;
            }
        }
        return distances;
    }

    //this method prune when it detects the next moves are impossible to reach the target cell in a valid way
    private boolean isPrune(int row, int col, int step){
        //calculate the remaining steps
        int remainingSteps = (gridSize * gridSize - 1) - step;
        int distanceToTarget = shortestDistance2Target[row * gridSize + col];

        //prune if the remaining steps are insufficient to reach the target distance
        return remainingSteps < distanceToTarget;
    }


    boolean canVisitAllRemainingCells(int row, int col,int step) {
        boolean[][] visitedCopy = new boolean[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            System.arraycopy(visitedCell[i], 0, visitedCopy[i], 0, gridSize);
        }
        return floodFill(row, col, visitedCopy) >= (gridSize * gridSize - step);
    }

    int floodFill(int row, int col, boolean[][] visitedCopy) {
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize || visitedCopy[row][col]) {
            return 0;
        }
        visitedCopy[row][col] = true;
        int count = 1;
        for (int[] direction : directionArray) {
            count += floodFill(row + direction[0], col + direction[1], visitedCopy);
        }
        return count;
    }

    private boolean isDeadEnd(int row, int col) {
        boolean left = (col > 0) && visitedCell[row][col - 1]; // Check if left is within bounds
        boolean right = (col < gridSize - 1) && visitedCell[row][col + 1]; // Check if right is within bounds
        boolean up = (row > 0) && visitedCell[row - 1][col]; // Check if up is within bounds
        boolean down = (row < gridSize - 1) && visitedCell[row + 1][col]; // Check if down is within bounds

        return ((left && right) && (!up && !down)) ||
                ((up && down) && (!left && !right));
    }


    public void findTotalPath(int startRow, int startCol, int step) {
        // Base case: Stop recursion when path is complete
        if (step == gridSize * gridSize - 1) {
            if (startRow == gridSize - 1 && startCol == 0) {
                totalPaths++; // Found a valid path
            }
            return;
        }

        // Skip invalid moves
        if (!isValidMove(startRow, startCol)) return;

        if (isDeadEnd(startRow, startCol)) return; // Prune dead-end paths

        if (isPrune(startRow, startCol, step)) return;

        if (!canVisitAllRemainingCells(startRow, startCol, step)) {
            return;
        }

        visitedCell[startRow][startCol] = true;

        char dir = directionChar[step];
        if (dir == '*'){ //if detect there is a ''
            //try all possible directions
            for (int[] direction : directionArray){
                int newRow = startRow + direction[0];
                int newCol = startCol + direction[1];
                if (isValidMove(newRow, newCol)){
                    // long oldTotalPaths = totalPaths;
                    findTotalPath(newRow, newCol, step + 1);
                    // pathsFromHere += (totalPaths - oldTotalPaths);
                }
            }
        } else {
            //Moving in the specified direction
            int dirIndex = "UDLR".indexOf(dir); //UDLR order must follow the order of declared directionArray
            int newRow = startRow + directionArray[dirIndex][0];
            int newCol = startCol + directionArray[dirIndex][1];
            if (isValidMove(newRow, newCol)){
                // long oldTotalPaths = totalPaths;
                findTotalPath(newRow, newCol, step + 1);
                // pathsFromHere += (totalPaths - oldTotalPaths);
            }
        }
        visitedCell[startRow][startCol] = false; //backtrack
    }
}