package org.example;

public class Algo {
    static final int N = 8;
    static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1} // Up, Down, Left, Right
    };
    static final String DIRECTION_CHARS = "UDLR"; // Corresponding direction characters

    static boolean isValid(int x, int y, long visited) {
        return x >= 0 && x < N && y >= 0 && y < N && (visited & (1L << (x * N + y))) == 0;
    }

    static boolean isPrunable(int x, int y, int targetX, int targetY, int step, int totalSteps, long visited) {
        // Prune if remaining steps are insufficient to cover the Manhattan distance to the target
        int manhattanDistance = Math.abs(x - targetX) + Math.abs(y - targetY);
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
        // Perform a flood fill from the current position to see if all unvisited nodes are connected
        boolean[][] visitedNodes = new boolean[N][N];
        int unvisitedCount = 0;

        // Mark all visited nodes as true in visitedNodes array
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if ((visited & (1L << (i * N + j))) != 0) {
                    visitedNodes[i][j] = true;
                } else {
                    unvisitedCount++;
                }
            }
        }

        // Use a simple array-based queue for BFS
        int[][] queue = new int[N * N][2];
        int front = 0, rear = 0;

        // Add the starting node to the queue
        queue[rear][0] = startX;
        queue[rear][1] = startY;
        rear++;
        visitedNodes[startX][startY] = true;

        int reachableCount = 0;

        while (front < rear) {
            int[] current = queue[front++];
            int x = current[0];
            int y = current[1];

            reachableCount++;

            for (int[] d : DIRECTIONS) {
                int newX = x + d[0];
                int newY = y + d[1];
                if (newX >= 0 && newX < N && newY >= 0 && newY < N && !visitedNodes[newX][newY]) {
                    visitedNodes[newX][newY] = true;
                    queue[rear][0] = newX;
                    queue[rear][1] = newY;
                    rear++;
                }
            }
        }

        // Return true if all unvisited nodes are reachable
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
            // If '*', try all four possible directions except the direction we just came from
            for (int i = 0; i < DIRECTIONS.length; i++) {
                if (i == (prevDir ^ 1)) { // Skip the direction opposite to the previous move
                    continue;
                }
                int newX = x + DIRECTIONS[i][0];
                int newY = y + DIRECTIONS[i][1];
                if (isValid(newX, newY, visited)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visited, directions, step + 1, i);
                }
            }
        } else {
            // Move in the specified direction
            int dirIndex = DIRECTION_CHARS.indexOf(dir);
            if (dirIndex != -1) {
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
