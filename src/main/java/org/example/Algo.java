package org.example;

public class Algo {
    static final int GRID_SIZE = 8; // Size of the grid (8x8)

    // Possible movement directions: UP, DOWN, LEFT, RIGHT
    static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };
    static final String DIRECTION_CHARACTERS = "UDLR"; // String representation of directions

    // Precomputed Manhattan distances between all cells in the grid
    static final int[][] manhattanDistances = new int[GRID_SIZE * GRID_SIZE][GRID_SIZE * GRID_SIZE];

    static {
        // Initialize Manhattan distances
        for (int x1 = 0; x1 < GRID_SIZE; x1++) {
            for (int y1 = 0; y1 < GRID_SIZE; y1++) {
                for (int x2 = 0; x2 < GRID_SIZE; x2++) {
                    for (int y2 = 0; y2 < GRID_SIZE; y2++) {
                        manhattanDistances[x1 * GRID_SIZE + y1][x2 * GRID_SIZE + y2] =
                                Math.abs(x1 - x2) + Math.abs(y1 - y2);
                    }
                }
            }
        }
    }

    // Precomputed valid moves for each cell in the grid
    static final boolean[][][] validMoves = new boolean[GRID_SIZE][GRID_SIZE][4];

    static {
        // Initialize valid moves
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                validMoves[x][y][0] = x > 0;         // Can move UP
                validMoves[x][y][1] = x < GRID_SIZE - 1; // Can move DOWN
                validMoves[x][y][2] = y > 0;         // Can move LEFT
                validMoves[x][y][3] = y < GRID_SIZE - 1; // Can move RIGHT
            }
        }
    }

    /**
     * Checks if a cell is valid and not yet visited.
     */
    static boolean isValidCell(int x, int y, long visitedCells) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE
                && (visitedCells & (1L << (x * GRID_SIZE + y))) == 0;
    }

    /**
     * Checks if the current state can be pruned based on constraints such as
     * Manhattan distance and connectivity.
     */
    static boolean isPrunable(int currentX, int currentY, int targetX, int targetY,
                              int currentStep, int totalSteps, long visitedCells) {
        // Prune if remaining steps are insufficient to reach the target
        int remainingSteps = totalSteps - currentStep;
        int manhattanDistance = manhattanDistances[currentX * GRID_SIZE + currentY][targetX * GRID_SIZE + targetY];
        if (remainingSteps < manhattanDistance) {
            return true;
        }

        // Count valid neighbors to check connectivity
        int neighborCount = 0;
        for (int direction = 0; direction < 4; direction++) {
            if (!validMoves[currentX][currentY][direction]) continue;
            int newX = currentX + DIRECTIONS[direction][0];
            int newY = currentY + DIRECTIONS[direction][1];
            if (isValidCell(newX, newY, visitedCells)) {
                neighborCount++;
                if (neighborCount > 2) break; // No need to continue checking
            }
        }

        // If degree of connectivity is ≤2, ensure the grid is connected
        if (neighborCount <= 2 && !isGridConnected(currentX, currentY, visitedCells)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the grid is still connected by performing a BFS from the current position.
     */
    static boolean isGridConnected(int startX, int startY, long visitedCells) {
        boolean[][] visitedNodes = new boolean[GRID_SIZE][GRID_SIZE];
        int[][] queue = new int[GRID_SIZE * GRID_SIZE][2];
        int front = 0, rear = 0;

        // Start BFS from the current cell
        queue[rear][0] = startX;
        queue[rear][1] = startY;
        rear++;
        visitedNodes[startX][startY] = true;

        int reachableCells = 0;
        int unvisitedCells = GRID_SIZE * GRID_SIZE - Long.bitCount(visitedCells);

        while (front < rear) {
            int[] currentCell = queue[front++];
            int x = currentCell[0];
            int y = currentCell[1];
            reachableCells++;

            if (reachableCells == unvisitedCells) {
                return true; // All unvisited cells are reachable
            }

            for (int direction = 0; direction < 4; direction++) {
                if (!validMoves[x][y][direction]) continue;
                int newX = x + DIRECTIONS[direction][0];
                int newY = y + DIRECTIONS[direction][1];
                if (newX >= 0 && newX < GRID_SIZE && newY >= 0 && newY < GRID_SIZE
                        && !visitedNodes[newX][newY]) {
                    int cellIndex = newX * GRID_SIZE + newY;
                    if ((visitedCells & (1L << cellIndex)) == 0) {
                        visitedNodes[newX][newY] = true;
                        queue[rear][0] = newX;
                        queue[rear][1] = newY;
                        rear++;
                    }
                }
            }
        }

        return reachableCells == unvisitedCells;
    }

    /**
     * Recursive function to find all valid paths from the current position to the target.
     */
    static long findPaths(int currentX, int currentY, int targetX, int targetY,
                          long visitedCells, char[] directions, int step, int previousDirection) {
        // Base case: Reached the end of the directions string
        if (step == directions.length) {
            return (currentX == targetX && currentY == targetY) ? 1 : 0;
        }

        // Prune paths that cannot lead to a solution
        if (isPrunable(currentX, currentY, targetX, targetY, step, directions.length, visitedCells)) {
            return 0;
        }

        // Mark the current cell as visited
        visitedCells |= (1L << (currentX * GRID_SIZE + currentY));
        long pathCount = 0;

        char direction = directions[step];
        if (direction == '*') { // Handle wildcard
            for (int dir = 0; dir < 4; dir++) {
                if (dir == (previousDirection ^ 1) || !validMoves[currentX][currentY][dir]) continue;
                int newX = currentX + DIRECTIONS[dir][0];
                int newY = currentY + DIRECTIONS[dir][1];
                if (isValidCell(newX, newY, visitedCells)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visitedCells, directions, step + 1, dir);
                }
            }
        } else { // Handle specific directions
            int dirIndex = DIRECTION_CHARACTERS.indexOf(direction);
            if (dirIndex != -1 && validMoves[currentX][currentY][dirIndex]) {
                int newX = currentX + DIRECTIONS[dirIndex][0];
                int newY = currentY + DIRECTIONS[dirIndex][1];
                if (isValidCell(newX, newY, visitedCells)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visitedCells, directions, step + 1, dirIndex);
                }
            }
        }

        return pathCount;
    }

    /**
     * Processes the input string and calculates the total number of valid paths.
     */
    static void processInput(String input) {
        input = input.toUpperCase();

        if (!input.matches("[UDLR*]+")) {
            System.out.println("Error: Input contains invalid characters.");
            return;
        }

        if (input.length() != (GRID_SIZE * GRID_SIZE) - 1) {
            System.out.println("Error: Input length must be " + ((GRID_SIZE * GRID_SIZE) - 1)
                    + ". Current length: " + input.length());
            return;
        }

        char[] directions = input.toCharArray();

        long startTime = System.currentTimeMillis();
        long totalPaths = findPaths(0, 0, GRID_SIZE - 1, 0, 0L, directions, 0, -1);
        long endTime = System.currentTimeMillis();

        System.out.println("Total paths: " + totalPaths);
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
    }

    public static void main(String[] args) {
        String input = "*****DR******R******R********************R*D******L*****L******";
        processInput(input);
    }
}
