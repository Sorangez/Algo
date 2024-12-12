package org.example;

public class Algo2 {
    public static void main(String[] args) {
        int gridSize = 8; // Dimension of the grid (NxN)
        int totalMoves = gridSize * gridSize - 1; // Total steps needed to complete the path

        // Case 1: All '*' (wildcard moves)
         String directionCommands = "***************************************************************";
        // Case 2: Mixed commands with specific directions
//        String directionCommands = "*****DR******R******R********************R*D************L******";

        // Validate the input length to match the required number of moves
        if (directionCommands.length() != totalMoves) {
            System.out.print("Invalid input!\n");
            System.out.print("Please insert " + totalMoves + " characters totally!\n");
            System.out.print("Insert " + (totalMoves - directionCommands.length()) + " more characters\n");
            return;
        }

        // Initialize the grid and compute paths
        Grid2 grid2 = new Grid2(gridSize, directionCommands);
        long startTime = System.currentTimeMillis();
        grid2.findTotalPaths(0, 0, 0); // Start finding paths from the top-left corner (0, 0)
        long endTime = System.currentTimeMillis();

        // Output the results
        System.out.print("Total paths: " + grid2.totalPaths + "\n");
        System.out.print("Total time: " + (endTime - startTime) + "ms\n");
    }
}

class Grid2 {
    private int gridSize; // Dimension of the grid (NxN)
    private int[][] visitedCells; // Tracks visited cells during the search
    private long visitedMask = 0L; // Bitmask representation of visited cells
    private int[][] directionArray = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Movement directions: Up, Down, Left, Right
    private char[] directionCommands; // Input command sequence (e.g., '*', 'U', 'D', 'L', 'R')
    public long totalPaths = 0; // Counter for the total number of valid paths
    private long[] neighbors; // Precomputed valid neighbors for each cell
    private int[] shortestDistancesToTarget; // Precomputed Manhattan distances to the target

    private int wildcardStepCount = 0; // Tracks the number of wildcard steps taken
    private int totalCells; // Total cells in the grid (gridSize^2)
    private int maxSteps; // Total steps required to traverse the grid

    public Grid2(int size, String commands) {
        this.gridSize = size;
        this.visitedCells = new int[gridSize][gridSize];
        this.directionCommands = commands.toCharArray();
        this.totalCells = gridSize * gridSize;
        this.maxSteps = totalCells - 1;
        precomputeNeighbors();
        initializeMap();
        initializeShortestDistances();
    }

    private void initializeMap() {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if ((i == 0 || i == gridSize - 1) && (j == 0 || j == gridSize - 1)) {
                    visitedCells[i][j] = 2;
                } else if (i == 0 || i == gridSize - 1 || j == 0 || j == gridSize - 1) {
                    visitedCells[i][j] = 3;
                } else {
                    visitedCells[i][j] = 4;
                }
            }
        }
    }

    // Precompute Manhattan distances for pruning
    private void initializeShortestDistances() {
        this.shortestDistancesToTarget = computeShortestDistancesToTarget();
    }

    // Compute Manhattan distance from each cell to the target (bottom-left corner)
    private int[] computeShortestDistancesToTarget() {
        int[] distances = new int[totalCells];
        int targetRow = gridSize - 1;
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int index = row * gridSize + col;
                distances[index] = Math.abs(row - targetRow) + col; // Distance to (gridSize-1, 0)
            }
        }
        return distances;
    }

    private int getValidMoves(int row, int col) {
        int validMoves = 0;

        // Check each direction in the order of up, down, left, right
        for (int i = 0; i < directionArray.length; i++) {
            int newRow = row + directionArray[i][0];
            int newCol = col + directionArray[i][1];

            if (isValidMove(newRow, newCol)) {
                // If visitedCells[newRow][newCol] == 1, return only that direction
                if (visitedCells[newRow][newCol] == 1) {
                    return (1 << i);  // Return a bitmask for that specific direction
                }
                // Otherwise, accumulate all valid moves in the bitmask
                validMoves |= (1 << i);
            }
        }

        return validMoves;
    }

    private int moveToPosition(int row, int col) {
        int directionBitmask = 0b0000;
        visitedMask |= (1L << (row * gridSize + col)); // Update bitmask

        // Check if the position is valid
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) {
            System.out.println("Invalid position");
            return directionBitmask;
        }

        visitedCells[row][col] = 0;

        // Adjust the surrounding cells
        for (int i = 0; i < directionArray.length; i++) {
            int[] direction = directionArray[i];
            int newRow = row + direction[0];
            int newCol = col + direction[1];

            // Check if the new position is within bounds
            if (newRow >= 0 && newRow < gridSize && newCol >= 0 && newCol < gridSize && visitedCells[newRow][newCol] != 0) {
                // Reduce the value by 1, ensuring it doesn't go below 0
                visitedCells[newRow][newCol] = Math.max(1, --visitedCells[newRow][newCol]);

                // Set the corresponding bit in the directionBitmask
                directionBitmask |= (1 << i);  // Set the bit for the direction
            }
        }

        return directionBitmask;
    }

    private void undoMove(int row, int col, int originalValue, int directionBitmask) {
        visitedCells[row][col] = originalValue;
        visitedMask &= ~(1L << (row * gridSize + col));

        // Iterate through each direction (up, down, left, right)
        for (int i = 0; i < directionArray.length; i++) {
            // Check if the current direction bit is set in the bitmask
            if ((directionBitmask & (1 << i)) != 0) {
                int[] direction = directionArray[i];
                int newRow = row + direction[0];
                int newCol = col + direction[1];

                // Undo the modification: increment the visited cell by 1 (reverting the change)
                visitedCells[newRow][newCol]++;
            }
        }
    }



    // Precompute valid neighbors for each cell using bitmasks
    private void precomputeNeighbors() {
        neighbors = new long[totalCells];
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int index = row * gridSize + col;
                long mask = 0L;
                for (int[] direction : directionArray) {
                    int newRow = row + direction[0], newCol = col + direction[1];
                    if (newRow >= 0 && newRow < gridSize && newCol >= 0 && newCol < gridSize) {
                        int neighborIndex = newRow * gridSize + newCol;
                        mask |= (1L << neighborIndex);
                    }
                }
                neighbors[index] = mask;
            }
        }
    }

    // Prune the search if there are not enough steps remaining to reach the target
    private boolean shouldPrune(int row, int col, int step) {
        int currentIndex = row * gridSize + col;
        int remainingSteps = maxSteps - step;
        int distanceToTarget = shortestDistancesToTarget[currentIndex];
        return remainingSteps < distanceToTarget;
    }

    // Check if a move is valid (within bounds and unvisited)
    private boolean isValidMove(int row, int col) {
        return (row >= 0 && row < gridSize && col >= 0 && col < gridSize && visitedCells[row][col] != 0) && checkBorderConstraints(row, col) && !isDeadEnd(row, col);
    }

    // Detect dead ends based on the movement constraints
    private boolean isDeadEnd(int row, int col) {
        boolean left = (col > 0) && visitedCells[row][col - 1]  == 0;
        boolean right = (col < gridSize - 1) && visitedCells[row][col + 1]  == 0;
        boolean up = (row > 0) && visitedCells[row - 1][col]  == 0;
        boolean down = (row < gridSize - 1) && visitedCells[row + 1][col]  == 0;

        boolean horizontalBlock = (left && right && !up && !down);
        boolean verticalBlock = (up && down && !left && !right);
        return horizontalBlock || verticalBlock;
    }

    // Check if all remaining cells are reachable using a BFS-like approach with bitmasks
    private boolean canVisitAllRemainingCells(int row, int col, int step) {
        int requiredCells = totalCells - step;
        int startIndex = row * gridSize + col;
        long startMask = 1L << startIndex;

        if ((visitedMask & startMask) != 0) {
            return false;
        }

        long reached = startMask;
        long frontier = startMask;
        long invertedVisited = ~visitedMask;

        while (frontier != 0) {
            long nextFrontier = 0L;
            while (frontier != 0) {
                int cell = Long.numberOfTrailingZeros(frontier);
                frontier &= (frontier - 1);
                long candidates = neighbors[cell] & invertedVisited & ~reached;
                if (candidates != 0) {
                    reached |= candidates;
                    nextFrontier |= candidates;
                }
            }
            frontier = nextFrontier;
        }

        return (Long.bitCount(reached) == requiredCells);
    }

    // Ensure specific constraints for edge and corner cells are met
    private boolean checkBorderConstraints(int row, int col) {
        if (row == gridSize - 1) {
            for (int x = col + 1; x < gridSize; x++) {
                if (visitedCells[row][x] != 0) return false;
            }
        }
        if (col == gridSize - 1) {
            for (int y = 0; y < row; y++) {
                if (visitedCells[y][col] != 0) return false;
            }
        }
        if (row == 0 && col > 0) {
            for (int x = 1; x < col; x++) {
                if (visitedCells[row][x] != 0) return false;
            }
        }
        if (col == 0) {
            for (int y = 0; y < row; y++) {
                if (visitedCells[y][col] != 0) return false;
            }
        }
        return true;
    }

    // Count the number of valid moves available from the current cell
    private int countValidMoves(int row, int col) {
        int count = 0;
        for (int[] direction : directionArray) {
            int newRow = row + direction[0], newCol = col + direction[1];
            if (isValidMove(newRow, newCol)) {
                count++;
                if (count > 1) break; // No need to count more than 2
            }
        }
        return count;
    }

    // Recursive backtracking function to find all valid paths
    public void findTotalPaths(int row, int col, int step) {
//        if (!isValidMove(row, col)) return; // Invalid move
//        if (isDeadEnd(row, col)) return; // Dead end detected
//        if (!checkBorderConstraints(row, col)) return; // Border constraints violated
        if (shouldPrune(row, col, step)) return; // Prune based on remaining steps

        int visitCellValue = visitedCells[row][col];

        // If the path reaches the last step, check if it ends at the target cell
        if (step == maxSteps) {
            if (row == gridSize - 1 && col == 0) { // Target: bottom-left corner
                totalPaths++;
            }
            return;
        }

        char command = directionCommands[step]; // Get the current command
        boolean performConnectivityCheck = false;
        if (command == '*') { // Wildcard step
            wildcardStepCount++;
            // Perform connectivity check every 5 wildcard steps
            if (wildcardStepCount % 5 == 0) {
                if (countValidMoves(row, col) > 1) {
                    performConnectivityCheck = true;
                }
            }
        }

        if (performConnectivityCheck) {
            if (!canVisitAllRemainingCells(row, col, step)) {
                visitedCells[row][col] = visitCellValue;
                return;
            }
        }

        int updateDirections = moveToPosition(row, col);

        // Explore all directions for a wildcard step
        if (command == '*') {
            // Get the valid moves as a bitmask
            int validMoves = getValidMoves(row, col);

            // Check each direction (up, down, left, right) based on the bitmask
            for (int i = 0; i < directionArray.length; i++) {
                // If the bit at position i is set, the move in this direction is valid
                if ((validMoves & (1 << i)) != 0) {
                    int newRow = row + directionArray[i][0];
                    int newCol = col + directionArray[i][1];
                    findTotalPaths(newRow, newCol, step + 1);
                }
            }
        } else {
            int directionIndex = "UDLR".indexOf(command);
            int validMoves = getValidMoves(row, col); // Get the valid moves bitmask

            // Check if the specified direction is valid by checking the corresponding bit in the bitmask
            if ((validMoves & (1 << directionIndex)) != 0) {
                int newRow = row + directionArray[directionIndex][0];
                int newCol = col + directionArray[directionIndex][1];
                findTotalPaths(newRow, newCol, step + 1);
            }
        }

        // Backtrack
        undoMove(row, col, visitCellValue, updateDirections);

        if (command == '*') {
            wildcardStepCount--;
        }
    }
}
