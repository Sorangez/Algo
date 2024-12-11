package org.example;

public class Algo {
    public static void main(String[] args) {
        int GRIDSIZE = 8;
        int MOVES = GRIDSIZE * GRIDSIZE - 1;

        String inputDirectionCommand = "*****DR******R******R********************R*D************L******";

        if (inputDirectionCommand.length() != MOVES) {
            System.out.print("Invalid input!\n");
            System.out.print("Please insert " + MOVES + " characters totally!\n");
            System.out.print("Insert " + (MOVES - inputDirectionCommand.length()) + " more characters\n");
            return;
        }

        grid myGrid = new grid(GRIDSIZE, inputDirectionCommand);
        long startTime = System.currentTimeMillis();
        myGrid.findTotalPath(0, 0, 0);
        long endTime = System.currentTimeMillis();

        System.out.print("Total paths: " + myGrid.totalPaths + "\n");
        System.out.print("Total time: " + (endTime - startTime) + "ms\n");
    }
}

class grid {
    int gridSize;
    boolean[][] visitedCell;
    long visitedMask = 0L;
    int[][] directionArray = {{-1,0},{1,0},{0,-1},{0,1}};
    char[] directionChar;
    long totalPaths = 0;
    int[] shortestDistance2Target;
    long[] neighbors;

    public grid(int size, String directionString) {
        this.gridSize = size;
        this.visitedCell = new boolean[gridSize][gridSize];
        this.directionChar = directionString.toCharArray();
        initShortestDistances();
        precomputeNeighbors();
    }

    private void initShortestDistances() {
        this.shortestDistance2Target = precomputeShortestDistance2Target();
    }

    private int[] precomputeShortestDistance2Target() {
        int[] distances = new int[gridSize * gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int index = i * gridSize + j;
                distances[index] = Math.abs(i - (gridSize - 1)) + j;
            }
        }
        return distances;
    }

    private void precomputeNeighbors() {
        int totalCells = gridSize*gridSize;
        neighbors = new long[totalCells];
        for (int r=0;r<gridSize;r++){
            for (int c=0;c<gridSize;c++){
                int idx = cellIndex(r,c);
                long mask=0L;
                for (int[] d: directionArray) {
                    int nr=r+d[0],nc=c+d[1];
                    if (nr>=0 && nr<gridSize && nc>=0 && nc<gridSize) {
                        int nidx=cellIndex(nr,nc);
                        mask|=(1L<<nidx);
                    }
                }
                neighbors[idx]=mask;
            }
        }
    }

    private int cellIndex(int r,int c){
        return r*gridSize+c;
    }

    private boolean isValidMove(int row,int col){
        return (row>=0 && row<gridSize && col>=0 && col<gridSize && !visitedCell[row][col]);
    }

    private boolean isPrune(int row,int col,int step){
        int remainingSteps=(gridSize*gridSize-1)-step;
        int distanceToTarget = shortestDistance2Target[cellIndex(row,col)];
        return remainingSteps<distanceToTarget;
    }

    private boolean isDeadEnd(int row,int col){
        boolean left=(col>0)&&visitedCell[row][col-1];
        boolean right=(col<gridSize-1)&&visitedCell[row][col+1];
        boolean up=(row>0)&&visitedCell[row-1][col];
        boolean down=(row<gridSize-1)&&visitedCell[row+1][col];

        return ((left&&right)&&(!up&&!down))||
                ((up&&down)&&(!left&&!right));
    }

    // Bitmask BFS for connectivity
    private boolean canVisitAllRemainingCellsBitmask(int row,int col,int step) {
        int needed=gridSize*gridSize-step;
        int startIndex=cellIndex(row,col);
        long startMask=1L<<startIndex;

        if ((visitedMask & startMask)!=0) {
            return false;
        }

        long reached=startMask;
        long frontier=startMask;
        long invVisited=~visitedMask;
        while(frontier!=0){
            long nextFrontier=0L;
            while(frontier!=0){
                int cell=Long.numberOfTrailingZeros(frontier);
                frontier&=(frontier-1);
                long candidates=neighbors[cell]&invVisited&~reached;
                if (candidates!=0) {
                    reached|=candidates;
                    nextFrontier|=candidates;
                }
            }
            frontier=nextFrontier;
        }

        return (Long.bitCount(reached)==needed);
    }

    public void findTotalPath(int startRow,int startCol,int step){
        if (step==gridSize*gridSize-1){
            if (startRow==gridSize-1 && startCol==0){
                totalPaths++;
            }
            return;
        }

        if (!isValidMove(startRow,startCol)) return;
        if (isDeadEnd(startRow,startCol)) return;
        if (isPrune(startRow,startCol,step)) return;

        char dir = directionChar[step];

        // Check connectivity only if this is a '*' step
        if (dir == '*') {
            if (!canVisitAllRemainingCellsBitmask(startRow,startCol,step)) return;
        }

        visitedCell[startRow][startCol]=true;
        visitedMask |= (1L<<cellIndex(startRow,startCol));

        if (dir=='*'){
            for (int[] d: directionArray){
                int nr=startRow+d[0],nc=startCol+d[1];
                if (isValidMove(nr,nc)){
                    findTotalPath(nr,nc,step+1);
                }
            }
        } else {
            int dirIndex="UDLR".indexOf(dir);
            int nr=startRow+directionArray[dirIndex][0];
            int nc=startCol+directionArray[dirIndex][1];
            if (isValidMove(nr,nc)){
                findTotalPath(nr,nc,step+1);
            }
        }

        // Backtrack
        visitedCell[startRow][startCol]=false;
        visitedMask &= ~(1L<<cellIndex(startRow,startCol));
    }
}
