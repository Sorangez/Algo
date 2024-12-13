package org.example;

public class Algo2 {
    public static void main(String[] args) {

        int gridSize = 8; // Dimension of the grid (NxN)
        int totalMoves = gridSize * gridSize - 1; // Total steps needed to complete the path

        // Case 1: All '*' (wildcard moves)
        // String directionCommands = "***************************************************************";

        // Case 2: Mixed commands with specific directions (uncomment to test)
        String directionCommands = "*****DR******R******R********************R*D************L******";

        // Validate the input length
        if (directionCommands.length() != totalMoves) {
            System.out.println("Invalid input!");
            System.out.println("Please insert " + totalMoves + " characters total!");
            System.out.println("Insert " + (totalMoves - directionCommands.length()) + " more characters");
            return;
        }

        // Initialize the grid and compute paths
        Grid2 grid2 = new Grid2(gridSize, directionCommands);
        long startTime = System.currentTimeMillis();
        grid2.findTotalPaths(0, 0, 0); // Start from the top-left corner (0, 0)
        long endTime = System.currentTimeMillis();

        // Output the results
        System.out.println("Total paths: " + grid2.totalPaths);
        System.out.println("Total time: " + (endTime - startTime) + "ms");
    }
}

class Grid2 {
    private final int gridSize;                  // Dimension of the grid (NxN)
    private final int[][] visitedCells;          // Tracks visited cells
    private long visitedMask = 0L;               // Bitmask of visited cells
    private final int[][] directionArray = {
        {-1, 0}, // Up
        {1, 0},  // Down
        {0, -1}, // Left
        {0, 1}   // Right
    };
    private final char[] directionCommands;      // Input command sequence ('*', 'U', 'D', 'L', 'R')
    public long totalPaths = 0;                  // Count of valid paths
    private long[] neighbors;                    // Precomputed valid neighbors for each cell
    private int[] shortestDistancesToTarget;     // Precomputed Manhattan distances to the target

    private int wildcardStepCount = 0;           // Number of wildcard steps taken
    private final int totalCells;                // Total cells in the grid (gridSize^2)
    private final int maxSteps;                  // Steps required to traverse the entire grid

    public Grid2(int size, String commands) {
        this.gridSize = size;
        this.visitedCells = new int[gridSize][gridSize];
        this.directionCommands = commands.toCharArray();
        this.totalCells = gridSize * gridSize;
        this.maxSteps = totalCells - 1;

        //Precompute solutions
        precomputeNeighbors();
        initializeMap();
//        initializeShortestDistances();
    }

    /**
     * Initialize the map with values representing how many times a cell
     * can still be visited. Corners start with 2, edges with 3, and inner cells with 4.
     */
    private void initializeMap() {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if ((i == 0 || i == gridSize - 1) && (j == 0 || j == gridSize - 1)) {
                    visitedCells[i][j] = 2; // Corners
                } else if (i == 0 || i == gridSize - 1 || j == 0 || j == gridSize - 1) {
                    visitedCells[i][j] = 3; // Edges
                } else {
                    visitedCells[i][j] = 4; // Inner cells
                }
            }
        }
    }

    /**
     * Precompute the shortest Manhattan distances to the target cell (bottom-left corner).
     * This information is used for pruning.
     */
    private void initializeShortestDistances() {
        this.shortestDistancesToTarget = computeShortestDistancesToTarget();
    }

    /**
     * Compute the Manhattan distance from every cell to the target cell (gridSize-1, 0).
     */
    private int[] computeShortestDistancesToTarget() {
        int[] distances = new int[totalCells];
        int targetRow = gridSize - 1;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int index = row * gridSize + col;
                distances[index] = Math.abs(row - targetRow) + col;
            }
        }

        return distances;
    }

    /**
     * Precompute valid neighbors for each cell using bitmasks. Each cell's neighbors are stored
     * in a bitmask, allowing quick checks of connectivity.
     */
    private void precomputeNeighbors() {
        neighbors = new long[totalCells];

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int index = row * gridSize + col; //flatten 2D to 1D array index
                long mask = 0L;

                for (int[] direction : directionArray) {
                    int newRow = row + direction[0];
                    int newCol = col + direction[1];
                    if (newRow >= 0 && newRow < gridSize && newCol >= 0 && newCol < gridSize) {
                        int neighborIndex = newRow * gridSize + newCol;
                        mask |= (1L << neighborIndex);
                    }
                }

                neighbors[index] = mask;
            }
        }
    }

    /**
     * Determine if we should prune the search from the current cell at the given step.
     * We prune if there are not enough steps left to reach the target.
     */
    private boolean shouldPrune(int row, int col, int step) {
        int currentIndex = row * gridSize + col;
        int remainingSteps = maxSteps - step;
        int distanceToTarget = shortestDistancesToTarget[currentIndex];
        return remainingSteps < distanceToTarget;
    }

    /**
     * Check if a move is valid: the cell must be within bounds, unvisited, meet border constraints,
     * and not lead to a dead end.
     */
    private boolean isValidMove(int row, int col) {
        return (row >= 0 && row < gridSize &&
                col >= 0 && col < gridSize &&
                visitedCells[row][col] != 0 &&
                checkBorderConstraints(row, col) &&
                !isDeadEnd(row, col));
    }

    /**
     * Detect dead ends where the path cannot continue.
     * A dead end is defined here as certain blocked configurations of adjacent cells.
     */
    private boolean isDeadEnd(int row, int col) {
        boolean left = (col > 0) && visitedCells[row][col - 1] == 0;
        boolean right = (col < gridSize - 1) && visitedCells[row][col + 1] == 0;
        boolean up = (row > 0) && visitedCells[row - 1][col] == 0;
        boolean down = (row < gridSize - 1) && visitedCells[row + 1][col] == 0;

        boolean horizontalBlock = (left && right && !up && !down);
        boolean verticalBlock = (up && down && !left && !right);

        return horizontalBlock || verticalBlock;

        //IMPROVE DEADEND BY PRECOMPUTING PATTERN
    }

    /**
     * Ensure that certain border constraints are met.
     */
    private boolean checkBorderConstraints(int row, int col) {
        if (row == gridSize - 1) {
            // For bottom row, all cells to the right must be visited
            // Reasons: no cells to the right should remain unvisited when in the bottom row
            for (int x = gridSize - 1; x > col; x--) {
                if (visitedCells[row][x] != 0) return false;
            }
        } else if (col == gridSize - 1) {
            // For rightmost column, all cells above must be visited
            // Paths cannot revisit the right column after moving away, so all cells above must be visited first
            for (int y = 0; y < row; y++) {
                if (visitedCells[y][col] != 0) return false;
            }
        } else if (row == 0) {
            // For top row, all cells between must be visited
            // Path cannot revisit the top row once it moves away, so the cells to the left must already be visited
            for (int x = 1; x < col; x++) {
                if (visitedCells[row][x] != 0) return false;
            }
        } else if (col == 0) {
            // For left column, all cells above must be visited
            // Path cannot revisite the left column once it moves away, so the cells above must already be visited
            for (int y = 1; y < row; y++) {
                if (visitedCells[y][col] != 0) return false;
            }
        }

        return true;
    }

    /**
     * Count how many valid moves are available from the given cell.
     */
    private int countValidMoves(int row, int col) {
        int count = 0;
        for (int[] direction : directionArray) {
            int newRow = row + direction[0];
            int newCol = col + direction[1];
            if (isValidMove(newRow, newCol)) {
                count++;
                if (count > 1) break; // Early exit if more than 1 move is possible
            }
        }
        return count;
    }

    /**
     * Attempt to move into a given cell. Mark it as visited and decrement the count
     * of how many times neighboring cells can be visited.
     * Returns a bitmask indicating which neighbors were affected.
     */
    private int moveToPosition(int row, int col) {
        int directionBitmask = 0b0000;
        visitedMask |= (1L << (row * gridSize + col)); // Mark cell as visited in bitmask

        visitedCells[row][col] = 0;

        // Adjust surrounding cells
        for (int i = 0; i < directionArray.length; i++) {
            int newRow = row + directionArray[i][0];
            int newCol = col + directionArray[i][1];

            if (newRow >= 0 && newRow < gridSize && newCol >= 0 && newCol < gridSize && visitedCells[newRow][newCol] != 0) {
                visitedCells[newRow][newCol] = Math.max(1, visitedCells[newRow][newCol] - 1);
                directionBitmask |= (1 << i);  // Set the bit for this direction
            }
        }

        return directionBitmask;
    }

    /**
     * Undo a move performed by moveToPosition. Restores the original state.
     */
    private void undoMove(int row, int col, int originalValue, int directionBitmask) {
        visitedCells[row][col] = originalValue;
        visitedMask &= ~(1L << (row * gridSize + col));

        for (int i = 0; i < directionArray.length; i++) {
            if ((directionBitmask & (1 << i)) != 0) {
                int newRow = row + directionArray[i][0];
                int newCol = col + directionArray[i][1];
                visitedCells[newRow][newCol]++;
            }
        }
    }

    /**
     * Get a bitmask representing valid moves (up, down, left, right) from the current cell.
     * If a cell with a value 1 is found, only that direction is considered valid.
     */
    private int getValidMoves(int row, int col) {
        int validMoves = 0;
        for (int i = 0; i < directionArray.length; i++) {
            int newRow = row + directionArray[i][0];
            int newCol = col + directionArray[i][1];

            if (isValidMove(newRow, newCol)) {
                // If a cell with value == 1 is found, return only that direction
                if (visitedCells[newRow][newCol] == 1) {
                    return (1 << i);
                }
                // Otherwise accumulate all valid moves
                validMoves |= (1 << i);
            }
        }
        return validMoves;
    }

    /**
     * Check if all remaining cells are reachable from the current cell by performing a BFS-like check
     * using bitmasks. This is used to prune paths that cannot cover all cells.
     */
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

    /**
     * Recursive backtracking to find all valid paths.
     * 
     * @param row  current row
     * @param col  current column
     * @param step current step index in the path
     */

    public void findTotalPaths(int row, int col, int step) {
        // If we have reached the last step, check if we end at the target cell
        if (step == maxSteps) {
            checkEnding(row, col);
            return;
        }

        char command = directionCommands[step];
        int originalValue = visitedCells[row][col];

        // Perform connectivity checks periodically during wildcard steps
        boolean performConnectivityCheck = false;
        if (command == '*') {
            wildcardStepCount++;
            if (wildcardStepCount % 5 == 0 && countValidMoves(row, col) > 1) {
                performConnectivityCheck = true;
            }
        }

        if (performConnectivityCheck && !canVisitAllRemainingCells(row, col, step)) {
            visitedCells[row][col] = originalValue;
            if (command == '*') {
                wildcardStepCount--;
            }
            return;
        }

        int updateDirections = moveToPosition(row, col);
        int validMoves = getValidMoves(row, col);

        if (command == '*') {
            // Explore all directions for a wildcard step
            for (int i = 0; i < directionArray.length; i++) {
                if ((validMoves & (1 << i)) != 0) {
                    int newRow = row + directionArray[i][0];
                    int newCol = col + directionArray[i][1];
                    findTotalPaths(newRow, newCol, step + 1);
                }
            }
        } else {
            // Follow the specific direction provided
            int directionIndex = "UDLR".indexOf(command);
            if ((validMoves & (1 << directionIndex)) != 0) {
                int newRow = row + directionArray[directionIndex][0];
                int newCol = col + directionArray[directionIndex][1];
                findTotalPaths(newRow, newCol, step + 1);
            }
        }

        // Backtrack
        undoMove(row, col, originalValue, updateDirections);

        if (command == '*') {
            wildcardStepCount--;
        }
    }

    /**
     * If we have reached the final cell in the correct number of steps,
     * increment the total path count if the final cell is the target cell.
     */
    private void checkEnding(int row, int col) {
        if (row == gridSize - 1 && col == 0) { // Target: bottom-left corner
            totalPaths++;
        }
    }
}
