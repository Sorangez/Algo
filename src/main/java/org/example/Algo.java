package org.example;

public class Algo {
    static final int N = 8;

    // Directions
    static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1} // UP, DOWN, LEFT, RIGHT
    };
    static final String DIRECTION_CHARS = "UDLR";

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

    // Precomputed valid moves for each cell
    static final boolean[][][] VALID_MOVES = new boolean[N][N][4];

    static {
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                VALID_MOVES[x][y][0] = x > 0;         // UP
                VALID_MOVES[x][y][1] = x < N - 1;     // DOWN
                VALID_MOVES[x][y][2] = y > 0;         // LEFT
                VALID_MOVES[x][y][3] = y < N - 1;     // RIGHT
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

        // Check degree of neighbors directly (degree ≤ 2 logic)
        int degree = 0;
        for (int d = 0; d < 4; d++) {
            if (!VALID_MOVES[x][y][d]) continue;
            int newX = x + DIRECTIONS[d][0];
            int newY = y + DIRECTIONS[d][1];
            int cellIndex = newX * N + newY;
            if ((visited & (1L << cellIndex)) == 0) {
                degree++;
                // Early exit: If degree > 2, skip further checks
                if (degree > 2) break;
            }
        }
        // Prune if degree ≤ 2 and connectivity check fails
        if (degree <= 2 && !nodesConnected(x, y, visited)) {
            return true;
        }

        return false;
    }

    static boolean nodesConnected(int startX, int startY, long visited) {
        boolean[][] visitedNodes = new boolean[N][N];
        int[][] queue = new int[N * N][2];
        int front = 0, rear = 0;

        queue[rear][0] = startX;
        queue[rear][1] = startY;
        rear++;
        visitedNodes[startX][startY] = true;

        int reachableCount = 0;
        int unvisitedCount = N * N - Long.bitCount(visited);

        while (front < rear) {
            int[] current = queue[front++];
            int x = current[0];
            int y = current[1];

            reachableCount++;

            if (reachableCount == unvisitedCount) {
                return true;
            }

            for (int d = 0; d < 4; d++) {
                if (!VALID_MOVES[x][y][d]) continue;
                int newX = x + DIRECTIONS[d][0];
                int newY = y + DIRECTIONS[d][1];
                if (newX >= 0 && newX < N && newY >= 0 && newY < N && !visitedNodes[newX][newY]) {
                    int cellIndex = newX * N + newY;
                    if ((visited & (1L << cellIndex)) == 0) {
                        visitedNodes[newX][newY] = true;
                        queue[rear][0] = newX;
                        queue[rear][1] = newY;
                        rear++;
                    }
                }
            }
        }

        return reachableCount == unvisitedCount;
    }

    static long findPaths(int x, int y, int targetX, int targetY, long visited, char[] directions, int step, int prevDir) {
        if (step == directions.length) {
            return (x == targetX && y == targetY) ? 1 : 0;
        }

        if (isPrunable(x, y, targetX, targetY, step, directions.length, visited)) {
            return 0;
        }

        visited |= (1L << (x * N + y));
        long pathCount = 0;

        char dir = directions[step];
        if (dir == '*') {
            for (int d = 0; d < 4; d++) {
                if (d == (prevDir ^ 1) || !VALID_MOVES[x][y][d]) continue;
                int newX = x + DIRECTIONS[d][0];
                int newY = y + DIRECTIONS[d][1];
                if (isValid(newX, newY, visited)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visited, directions, step + 1, d);
                }
            }
        } else {
            int dirIndex = DIRECTION_CHARS.indexOf(dir);
            if (dirIndex != -1 && VALID_MOVES[x][y][dirIndex]) {
                int newX = x + DIRECTIONS[dirIndex][0];
                int newY = y + DIRECTIONS[dirIndex][1];
                if (isValid(newX, newY, visited)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visited, directions, step + 1, dirIndex);
                }
            }
        }

        visited &= ~(1L << (x * N + y));
        return pathCount;
    }

    public static void main(String[] args) {
        String input = "*****DR******R******R********************R*D************L******";

        input = input.toUpperCase();

        if (!input.matches("[UDLR*]+")) {
            System.out.println("Error: Invalid characters.");
            return;
        }

        if (input.length() != (N * N) - 1) {
            System.out.println("Error: Input length must be " + (N * N - 1) + ".");
            return;
        }

        char[] directions = input.toCharArray();

        long startTime = System.currentTimeMillis();

        long totalPaths = findPaths(0, 0, N - 1, 0, 0L, directions, 0, -1);

        long endTime = System.currentTimeMillis();

        System.out.println("Total paths: " + totalPaths);
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
    }
}
