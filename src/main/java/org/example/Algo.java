package org.example;

public class Algo {
    public static void main(String[] args) {
        int gridSize = 8; // Dimension of the grid (NxN)
        int totalMoves = gridSize * gridSize - 1; // Total steps needed to complete the path

        // Case 1: All '*' (wildcard moves)
        // String directionCommands = "***************************************************************";
        // Case 2: Mixed commands with specific directions
        String directionCommands = "*****DR******R******R********************R*D************L******";

        // Validate the input length to match the required number of moves
        if (directionCommands.length() != totalMoves) {
            System.out.print("Invalid input!\n");
            System.out.print("Please insert " + totalMoves + " characters totally!\n");
            System.out.print("Insert " + (totalMoves - directionCommands.length()) + " more characters\n");
            return;
        }

        // Initialize the grid and compute paths
        Grid grid = new Grid(gridSize, directionCommands);
        long startTime = System.currentTimeMillis();
        grid.findTotalPaths(0, 0, 0); // Start finding paths from the top-left corner (0, 0)
        long endTime = System.currentTimeMillis();

        // Output the results
        System.out.print("Total paths: " + grid.totalPaths + "\n");
        System.out.print("Total time: " + (endTime - startTime) + "ms\n");
    }
}

class Grid {
    private int gridSize; // Dimension of the grid (NxN)
    private boolean[][] visitedCells; // Tracks visited cells during the search
    private long visitedMask = 0L; // Bitmask representation of visited cells
    private int[][] directionArray = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Movement directions: Up, Down, Left, Right
    private char[] directionCommands; // Input command sequence (e.g., '*', 'U', 'D', 'L', 'R')
    public long totalPaths = 0; // Counter for the total number of valid paths
    private int[] shortestDistancesToTarget; // Precomputed Manhattan distances to the target
    private long[] neighbors; // Precomputed valid neighbors for each cell

    private int wildcardStepCount = 0; // Tracks the number of wildcard steps taken
    private int totalCells; // Total cells in the grid (gridSize^2)
    private int maxSteps; // Total steps required to traverse the grid

    public Grid(int size, String commands) {
        this.gridSize = size;
        this.visitedCells = new boolean[gridSize][gridSize];
        this.directionCommands = commands.toCharArray();
        this.totalCells = gridSize * gridSize;
        this.maxSteps = totalCells - 1;
        initializeShortestDistances();
        precomputeNeighbors();
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

    // Check if a move is valid (within bounds and unvisited)
    private boolean isValidMove(int row, int col) {
        return (row >= 0 && row < gridSize && col >= 0 && col < gridSize && !visitedCells[row][col]);
    }

    // Prune the search if there are not enough steps remaining to reach the target
    private boolean shouldPrune(int row, int col, int step) {
        int currentIndex = row * gridSize + col;
        int remainingSteps = maxSteps - step;
        int distanceToTarget = shortestDistancesToTarget[currentIndex];
        return remainingSteps < distanceToTarget;
    }

    // Detect dead ends based on the movement constraints
    private boolean isDeadEnd(int row, int col) {
        boolean left = (col > 0) && visitedCells[row][col - 1];
        boolean right = (col < gridSize - 1) && visitedCells[row][col + 1];
        boolean up = (row > 0) && visitedCells[row - 1][col];
        boolean down = (row < gridSize - 1) && visitedCells[row + 1][col];

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
                if (!visitedCells[row][x]) return false;
            }
        }
        if (col == gridSize - 1) {
            for (int y = 0; y < row; y++) {
                if (!visitedCells[y][col]) return false;
            }
        }
        if (row == 0 && col > 0) {
            for (int x = 1; x < col; x++) {
                if (!visitedCells[row][x]) return false;
            }
        }
        if (col == 0) {
            for (int y = 0; y < row; y++) {
                if (!visitedCells[y][col]) return false;
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
        if (!isValidMove(row, col)) return; // Invalid move
        if (isDeadEnd(row, col)) return; // Dead end detected
        if (shouldPrune(row, col, step)) return; // Prune based on remaining steps
        if (!checkBorderConstraints(row, col)) return; // Border constraints violated

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
                visitedCells[row][col] = false;
                return;
            }
        }

        visitedCells[row][col] = true; // Mark cell as visited
        visitedMask |= (1L << (row * gridSize + col)); // Update bitmask

        if (command == '*') { // Explore all directions for a wildcard step
            for (int[] direction : directionArray) {
                int newRow = row + direction[0], newCol = col + direction[1];
                if (isValidMove(newRow, newCol)) {
                    findTotalPaths(newRow, newCol, step + 1);
                }
            }
        } else { // Move in the specified direction
            int directionIndex = "UDLR".indexOf(command);
            int newRow = row + directionArray[directionIndex][0];
            int newCol = col + directionArray[directionIndex][1];
            if (isValidMove(newRow, newCol)) {
                findTotalPaths(newRow, newCol, step + 1);
            }
        }

        // Backtrack: Unmark the cell as visited
        visitedCells[row][col] = false;
        visitedMask &= ~(1L << (row * gridSize + col));

        if (command == '*') {
            wildcardStepCount--;
        }
    }
}
