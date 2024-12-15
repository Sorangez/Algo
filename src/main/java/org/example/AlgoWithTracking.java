package org.example;

public class AlgoWithTracking {
    public static void calculatePath(int size, String commands) {
        int totalMoves = size * size - 1; // Total steps needed to complete the path

        // Validate the input length
        if (commands.length() != totalMoves) {
            System.out.println("Invalid input!");
            System.out.println("Please insert " + totalMoves + " characters total!");
            System.out.println("Insert " + (totalMoves - commands.length()) + " more characters");
            return;
        }

        // Initialize the grid and compute paths
        long precomputedStartTime = System.currentTimeMillis();
        Grid2 grid2 = new Grid2(size, commands);
        long precomputedEndTime = System.currentTimeMillis();

        System.out.println("Precomputed time: " + (precomputedEndTime - precomputedStartTime) + "ms");

        long startTime = System.currentTimeMillis();
        grid2.findTotalPaths(0, 0, 0); // Start from the top-left corner (0, 0)
        long endTime = System.currentTimeMillis();

        // Output the results
        System.out.println("Total paths: " + grid2.totalPaths);
        System.out.println("Total time: " + (endTime - startTime) + "ms");
        System.out.println("Manhattan prune: " + (grid2.manhattan));
        System.out.println("Connectivity prune: " + (grid2.connectivity));
        System.out.println("Border constraints prune: " + (grid2.border));
        System.out.println("Dead end prune: " + (grid2.deadEnd));
    }

    public static void main(String[] args) {
        int gridSize = 8; // Dimension of the grid (NxN)

        // Case 1: All '*' (wildcard moves)
         String directionCommands = "***************************************************************";

        // Case 2: Mixed commands with specific directions (uncomment to test)
//        String directionCommands = "*****DR******R******R********************R*D************L******";

        calculatePath(gridSize, directionCommands);
    }
}


/**
 *  Main class used for computing
 *
 *
 */
class Grid2 {
    private final int gridSize; // Dimension of the grid (N x N)
    private final int maxSteps; // Steps required to traverse the entire grid
    private final int[][] visitedCells; // Tracks visited cells
    private long visitedMask = 0L; // Bitmask of visited cells

    private final char[] directionCommands; // Input command sequence ('*', 'U', 'D', 'L', 'R')
    private final int[][] directionArray = {
        {-1, 0}, // Up
        {1, 0}, // Down
        {0, -1}, // Left
        {0, 1} // Right
    };

    public long totalPaths = 0; // Count of valid paths

    private int[] shortestDistancesToTarget; // Precomputed Manhattan distances to the target

    private long[] neighbors; // Precomputed valid neighbors for each cell

    private int wildcardStepCount = 0; // Number of wildcard steps taken
    private final int totalCells; // Total cells in the grid (gridSize^2)


    public int manhattan = 0;
    public int connectivity = 0;
    public int border = 0;
    public int deadEnd = 0;

    /**
     * Initialize the grid
     * @param size the dimension of the grid (N x N)
     * @param commands the commands in string
     */
    public Grid2(int size, String commands) {
        this.gridSize = size;
        this.visitedCells = new int[gridSize][gridSize];
        this.directionCommands = commands.toCharArray();
        this.totalCells = gridSize * gridSize;
        this.maxSteps = totalCells - 1;

        // Precompute process
        precomputeNeighbors();
        initializeMap();
        initializeShortestDistances();
    }

    /**
     * Initialize a heat map with values
     * representing how many times a cell can still be visited
     */
    private void initializeMap() {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                // Corners cells have 2 connection
                if ((i == 0 || i == gridSize - 1) && (j == 0 || j == gridSize - 1)) {
                    visitedCells[i][j] = 2;
                }

                // Edges cells have 3 connection
                else if (i == 0 || i == gridSize - 1 || j == 0 || j == gridSize - 1) {
                    visitedCells[i][j] = 3;
                }

                // Inner cells have 4 connection
                else {
                    visitedCells[i][j] = 4;
                }
            }
        }
    }

    /**
     * Precompute valid neighbors for each cell using bitmasks. Each cell's neighbors are stored
     * in a bitmask, allowing quick checks of connectivity.
     */
    private void precomputeNeighbors() {
        neighbors = new long[totalCells];

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int index = row * gridSize + col; // flatten 2D position to 1D array index
                long mask = 0L;

                for (int[] direction : directionArray) {
                    int newRow = row + direction[0];
                    int newCol = col + direction[1];


                    if (inBounds(newRow, newCol)) {
                        int neighborIndex = newRow * gridSize + newCol;
                        mask |= (1L << neighborIndex); // append
                    }
                }

                neighbors[index] = mask;
            }
        }
    }

    /**
     * Check if the move is inbound
     * @param row row index
     * @param col column index
     * @return true if inbound. otherwise false
     */
    private boolean inBounds(int row, int col) {
        return row >= 0 && row < gridSize && col >= 0 && col < gridSize;
    }

    /**
     * Check if a move is valid: the cell must be within bounds, unvisited, meet border constraints,
     * and not lead to a dead end.
     * @param row row index
     * @param col column index
     * @return true if valid. otherwise false
     */
    private boolean isValidMove(int row, int col, int step) {
        return inBounds(row, col)
                && visitedCells[row][col] != 0
                && checkBorderConstraints(row, col)
                && !isDeadEnd(row, col)
//                && isManhattanValid(row, col, step)
                ;
    }

    /**
     * Detect dead ends where the path cannot continue.
     * A dead end is defined here as certain blocked configurations of adjacent cells.
     * @param row row index
     * @param col column index
     * @return true if current cell leads to dead end. otherwise false
     */
    private boolean isDeadEnd(int row, int col) {
        boolean left = (col == 0) || visitedCells[row][col - 1] == 0;
        boolean right = (col == gridSize - 1) || visitedCells[row][col + 1] == 0;
        boolean up = (row == 0) || visitedCells[row - 1][col] == 0;
        boolean down = (row == gridSize - 1) || visitedCells[row + 1][col] == 0;

        boolean horizontalBlock = (left && right) && (!up && !down);
        boolean verticalBlock = (up && down) && (!left && !right);

        if (horizontalBlock || verticalBlock) {
            deadEnd++;
            return true;
        } else {
            return false;
        }

//        return horizontalBlock || verticalBlock;
    }

    private boolean failBorderConstraint() {
        border++;
        return false;
    }

    /**
     * Ensure that certain border constraints are met.
     * @param row row index
     * @param col column index
     * @return false if current cell violate constraints. otherwise true
     */
    private boolean checkBorderConstraints(int row, int col) {
        // For bottom border, all cells to the right must be visited
        if (row == gridSize - 1) {
            for (int x = gridSize - 1; x > col; x--) {
                if (visitedCells[row][x] != 0) return failBorderConstraint();
            }
        }

        // For right border, all cells above must be visited
        else if (col == gridSize - 1) {
            for (int y = 0; y < row; y++) {
                if (visitedCells[y][col] != 0) return failBorderConstraint();
            }
        }

        // For top border, all cells between must be visited
        else if (row == 0) {
            for (int x = 1; x < col; x++) {
                if (visitedCells[row][x] != 0) return failBorderConstraint();
            }
        }

        // For left border, all cells above must be visited
        else if (col == 0) {
            for (int y = 1; y < row; y++) {
                if (visitedCells[y][col] != 0) return failBorderConstraint();
            }
        }

        return true;
    }

    /**
     * Count how many valid moves are available from the given cell.
     * @param row row index
     * @param col column index
     * @return
     */
    private int countValidMoves(int row, int col, int step) {
        int count = 0;
        for (int[] direction : directionArray) {
            int newRow = row + direction[0];
            int newCol = col + direction[1];
            if (isValidMove(newRow, newCol, step)) {
                count++;
                if (count > 1) break; // Early exit if more than 1 move is possible
            }
        }
        return count;
    }

    /**
     * Check if all remaining cells are reachable from the current cell by performing a BFS-like check
     * using bitmasks. This is used to prune paths that cannot cover all cells.
     */
    private boolean canVisitAllRemainingCells(int row, int col, int step) {
        int requiredCells = totalCells - step;
        int startIndex = row * gridSize + col;
        long startMask = 1L << startIndex;

        // If the start cell is already visited, return false
        if ((visitedMask & startMask) != 0) {
            return false;
        }

        // Initialize BFS state
        long reached = startMask;
        long frontier = startMask;
        long invertedVisited = ~visitedMask;

        // Perform BFS to explore all reachable cells
        while (frontier != 0) {
            long nextFrontier = 0L;
            while (frontier != 0) {
                int cell = Long.numberOfTrailingZeros(frontier);
                frontier &= (frontier - 1);
                long candidates = neighbors[cell] & invertedVisited & ~reached;
//                if (candidates != 0) {
                    reached |= candidates;
                    nextFrontier |= candidates;
//                }
            }
            frontier = nextFrontier;
        }

        return (Long.bitCount(reached) == requiredCells);
    }

    /**
     * Get a bitmask representing valid moves (up, down, left, right) from the current cell.
     * If a cell with a value 1 is found, only that direction is considered valid.
     */
    private int getValidMoves(int row, int col, int step) {
        int validMoves = 0;
        for (int i = 0; i < directionArray.length; i++) {
            int newRow = row + directionArray[i][0];
            int newCol = col + directionArray[i][1];

            if (isValidMove(newRow, newCol, step) ) {
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
     * Attempt to move into a given cell. Mark it as visited and decrement the count
     * of how many times neighboring cells can be visited.
     * @return a bitmask indicating which neighbors were affected.
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
     * If we have reached the final cell in the correct number of steps,
     * increment the total path count if the final cell is the target cell.
     * @param row row index
     * @param col column index
     */
    private void checkEnding(int row, int col) {
        if (row == gridSize - 1 && col == 0) { // Target: bottom-left corner
            totalPaths++;
        }
    }


    /**
     * Recursively moving and backtracking to find all valid paths.
     * @param row row index
     * @param col column index
     * @param step step index in the path
     */
    public void findTotalPaths(int row, int col, int step) {
        // If we have reached the last step, check if we end at the target cell
        if (step == maxSteps) {
            checkEnding(row, col);
            return;
        }

        char command = directionCommands[step];

        int originalValue = visitedCells[row][col];

        int updatedDirections = moveToPosition(row, col);
        int validMoves = getValidMoves(row, col, step + 1);

        if (command == '*') {
            wildcardStepCount++;
            // Explore all directions for a wildcard step
            for (int i = 0; i < directionArray.length; i++) {
                if ((validMoves & (1 << i)) != 0) { // check if the move is valid
                    int newRow = row + directionArray[i][0];
                    int newCol = col + directionArray[i][1];

                    if (wildcardStepCount % 5 == 0 && validMoves > 0 && !canVisitAllRemainingCells(newRow, newCol, step + 1)) {
                        connectivity++;
                        break;
                    }
                    findTotalPaths(newRow, newCol, step + 1);
                }
            }
        } else {
            // Follow the specific direction provided
            int directionIndex = "UDLR".indexOf(command);
            if ((validMoves & (1 << directionIndex)) != 0) { // check if the move is valid
                int newRow = row + directionArray[directionIndex][0];
                int newCol = col + directionArray[directionIndex][1];
                findTotalPaths(newRow, newCol, step + 1);
            }
        }

        // Backtrack
        undoMove(row, col, originalValue, updatedDirections);

        if (command == '*') {
            wildcardStepCount--;
        }
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
     * Precompute the shortest Manhattan distances to the target cell (bottom-left corner).
     * This information is used for pruning.
     */
    private void initializeShortestDistances() {
        this.shortestDistancesToTarget = computeShortestDistancesToTarget();
    }

    /**
     * Determine if we should prune the search from the current cell at the given step.
     * We prune if there are not enough steps left to reach the target.
     */
    private boolean isManhattanValid(int row, int col, int step) {
        int currentIndex = row * gridSize + col;
        int remainingSteps = maxSteps - step;
        int distanceToTarget = shortestDistancesToTarget[currentIndex];

        if (remainingSteps < distanceToTarget) {
            manhattan++;
            return false;
        } else {
            return true;
        }

//        return remainingSteps > distanceToTarget;
    }
}
