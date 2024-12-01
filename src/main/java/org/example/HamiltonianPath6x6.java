package org.example;

public class HamiltonianPath6x6 {
    // Grid dimensions
    static final int N = 6;

    // Directions: up, down, left, right
    static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    // Utility method to check if a cell is valid and unvisited
    static boolean isValid(int x, int y, long visited) {
        // Check if (x, y) is within the bounds of the grid and if the cell is not visited
        return x >= 0 && x < N && y >= 0 && y < N && (visited & (1L << (x * N + y))) == 0;
    }

    // Recursive DFS method with pruning
    static int findPaths(int x, int y, int targetX, int targetY, long visited, char[] directions, int step) {
        // If we reached the target at the final step, return 1 path found
        if (x == targetX && y == targetY && step == directions.length) {
            return 1;
        }

        // If we have exceeded the number of steps, return 0
        if (step >= directions.length) {
            return 0;
        }

        // Mark the current cell as visited
        visited |= (1L << (x * N + y));
        int pathCount = 0;

        // Get the current direction or '*' indicating any direction
        char dir = directions[step];

        if (dir == '*') {
            // Explore all 4 possible directions
            for (int[] d : DIRECTIONS) {
                int newX = x + d[0];
                int newY = y + d[1];
                if (isValid(newX, newY, visited)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visited, directions, step + 1);
                }
            }
        } else {
            // Explore only the specified direction
            int dirIndex = "UDLR".indexOf(dir);
            if (dirIndex != -1) {
                int newX = x + DIRECTIONS[dirIndex][0];
                int newY = y + DIRECTIONS[dirIndex][1];
                // Add pruning to avoid invalid moves beyond grid boundaries
                if (isValid(newX, newY, visited)) {
                    pathCount += findPaths(newX, newY, targetX, targetY, visited, directions, step + 1);
                }
            }
        }

        // Unmark the current cell (backtrack)
        visited &= ~(1L << (x * N + y));

        return pathCount;
    }

    public static void main(String[] args) {
        // Example input
        String input = "***********************************"; // Adjust the length as per your requirement

        // User input verification for length
        if (input.length() != 35) {
            System.out.println("Error: Input string must be 35 characters long. Current length: " + input.length());
            return;
        }

        char[] directions = input.toCharArray();

        // Start from (0, 0), target is (0, 5) for 6x6 grid
        int totalPaths = findPaths(0, 0, 0, 5, 0L, directions, 0);
        System.out.println("Total paths: " + totalPaths);
    }
}
