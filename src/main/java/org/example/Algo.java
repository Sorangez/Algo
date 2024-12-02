package org.example;

public class Algo {
    static final int N = 8;
    static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1} // Up, Down, Left, Right
    };
    static final String DIRECTION_CHARS = "UDLR"; // Corresponding direction characters

    // Precomputed Manhattan distances
    static final int[][] MANHATTAN_DISTANCE = new int[N * N][N * N];

    static {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    for (int l = 0; l < N; l++) {
                        MANHATTAN_DISTANCE[i * N + j][k * N + l] = Math.abs(i - k) + Math.abs(j - l);
                    }
                }
            }
        }
    }

    // Precomputed edge-specific rules
    static final boolean[][][] VALID_MOVES = new boolean[N][N][4]; // [x][y][direction]

    static {
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                // Determine valid moves for each direction
                VALID_MOVES[x][y][0] = x > 0;         // Up
                VALID_MOVES[x][y][1] = x < N - 1;    // Down
                VALID_MOVES[x][y][2] = y > 0;        // Left
                VALID_MOVES[x][y][3] = y < N - 1;    // Right
            }
        }
    }

    static boolean isValid(int x, int y, long visited) {
        return x >= 0 && x < N && y >= 0 && y < N && (visited & (1L << (x * N + y))) == 0;
    }

    static boolean isPrunable(int x, int y, int targetX, int targetY, int step, int totalSteps, long visited) {
        // Prune if remaining steps are insufficient to cover the Manhattan distance to the target
        int manhattanDistance = MANHATTAN_DISTANCE[x * N + y][targetX * N + targetY];
        if ((totalSteps - step) < manhattanDistance) {
            return true;
        }

        // Perform a connectivity check if we are touching the right or bottom edge
        if ((x == N - 1 || y == N - 1) && !nodesConnected(x, y, visited)) {
            return true;
        }

        return false;
    }

    static boolean nodesConnected(int startX, int startY, long visited) {
        boolean[][] visitedNodes = new boolean[N][N];
        int[][] queue = new int[N * N][2];
        int front = 0, rear = 0;

        // Add the starting node to the queue
        queue[rear][0] = startX;
        queue[rear][1] = startY;
        rear++;
        visitedNodes[startX][startY] = true;

        int reachableCount = 0;
        int unvisitedCount = N * N - Long.bitCount(visited); // Total unvisited nodes

        while (front < rear) {
            int[] current = queue[front++];
            int x = current[0];
            int y = current[1];

            reachableCount++;

            // Early exit: If we've reached all unvisited nodes, return true
            if (reachableCount == unvisitedCount) {
                return true;
            }

            // Explore neighbors
            for (int d = 0; d < 4; d++) {
                if (!VALID_MOVES[x][y][d]) continue; // Use precomputed edge-specific rules
                int newX = x + DIRECTIONS[d][0];
                int newY = y + DIRECTIONS[d][1];
                if (newX >= 0 && newX < N && newY >= 0 && newY < N && !visitedNodes[newX][newY]) {
                    int cellIndex = newX * N + newY;
                    if ((visited & (1L << cellIndex)) == 0) { // Check if unvisited
                        visitedNodes[newX][newY] = true;
                        queue[rear][0] = newX;
                        queue[rear][1] = newY;
                        rear++;
                    }
                }
            }
        }

        // If we exhaust the flood-fill without reaching all unvisited nodes
        return reachableCount == unvisitedCount;
    }

    static long findPaths(int x, int y, int targetX, int targetY, long visited, char[] directions, int step, int prevDir) {
        // Base case: If all steps are exhausted, check if we reached the target
        if (step == directions.length) {
            return (x == targetX && y == targetY) ? 1 : 0;
        }

        // Pruning: Check if it's possible to reach the target within the remaining steps
        if (isPrunable(x, y, targetX, targetY, step, directions.length, visited)) {
            return 0;
        }

        // Mark the current cell as visited
        visited |= (1L << (x * N + y));
        long pathCount = 0;

        char dir = directions[step];
        if (dir == '*') {
            // If '*', try all valid directions except the opposite of the previous move
            for (int d = 0; d < 4; d++) {
                if (d == (prevDir ^ 1) || !VALID_MOVES[x][y][d]) continue; // Skip invalid or opposite direction
                int newX = x + DIRECTIONS[d][0];
                int newY = y + DIRECTIONS[d][1];
                if (isValid(newX, newY, visited)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visited, directions, step + 1, d);
                }
            }
        } else {
            // Move in the specified direction
            int dirIndex = DIRECTION_CHARS.indexOf(dir);
            if (dirIndex != -1 && VALID_MOVES[x][y][dirIndex]) {
                int newX = x + DIRECTIONS[dirIndex][0];
                int newY = y + DIRECTIONS[dirIndex][1];
                if (isValid(newX, newY, visited)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visited, directions, step + 1, dirIndex);
                }
            }
        }

        // Unmark the current cell before backtracking
        visited &= ~(1L << (x * N + y));

        return pathCount;
    }

    public static void main(String[] args) {
        String input = "***D**************D**U****U*******R*****U**D*******************";

        // Normalize to uppercase
        input = input.toUpperCase();

        // Check valid characters
        if (!input.matches("[UDLR*]+")) {
            System.out.println("Error: Input string contains invalid characters. Only 'U', 'D', 'L', 'R', or '*' are allowed.");
            return;
        }

        // Check length
        if (input.length() != (N * N) - 1) {
            System.out.println("Error: Input string must be " + (N * N - 1) + " characters long. Current length: " + input.length());
            return;
        }

        char[] directions = input.toCharArray();

        // Start timer
        long startTime = System.currentTimeMillis();

        // Call the findPaths function starting from (0, 0) with no previous direction (-1)
        long totalPaths = findPaths(0, 0, N - 1, 0, 0L, directions, 0, -1);

        // End timer
        long endTime = System.currentTimeMillis();

        // Output results and elapsed time
        System.out.println("Total paths: " + totalPaths);
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
    }
}
