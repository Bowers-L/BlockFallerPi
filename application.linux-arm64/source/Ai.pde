public class AI {

  private GameManager gameManager;

  //Height, lines, holes, roughness
  private double[] weights = {0.510066, 0.760666, 0.35663, 0.184483};
  //private double[] weights = {0, 0, 1, 0};


  public AI(GameManager gameManager) {

    this.gameManager = gameManager;
  }


  private float[] getAIResponse(GridManager gridManager) {
    double bestScore = -99999999;
    float[] moves = {-1, -1};


    // Loop through every situation and find best score
    for (int r = 0; r < 4; r++) {
      for (int p = 0; p < 10; p++) {
        // Clone original GridManager
        GridManager tempGridManager = gridManager.clone();

        // Rotate current piece
        for (int i = 0; i < r; i++)
          tempGridManager.rotateCurrentPiece(true, false);

        // Move to very left
        for(int i = 0; i < 5; i++)
          tempGridManager.moveCurrentPiece(Direction.LEFT, false);

        // Move it to pos
        for (int i = 0; i < p; i++)
          tempGridManager.moveCurrentPiece(Direction.RIGHT, false);

        // Fall until collision
        boolean collided = false;

        while (!collided) {
          collided = tempGridManager.moveCurrentPiece(Direction.DOWN, false);
        }

        // Add blocks to grid
        Block[] pieceBlocks = tempGridManager.currentPiece.getBlocks().clone();
        for (int z = 0; z < pieceBlocks.length; z++) {
          int x = pieceBlocks[z].getX();
          int y = pieceBlocks[z].getY();
          if (y >= 0)
            tempGridManager.blocks[x][y] = pieceBlocks[z].clone();
        }

        // Calc score
        double score = -(weights[0]*calcTotalHieght(tempGridManager.blocks)) + 
        (weights[1]*calcLines(tempGridManager.blocks)) - 
        (weights[2]*calcHoles(tempGridManager.blocks)) - 
        (weights[3]*calcRoughness(tempGridManager.blocks));

        // Update moves if found better score
        if (score > bestScore)  {
          bestScore = score;
          moves[0] = tempGridManager.currentPiece.x;
           moves[1] = tempGridManager.currentPiece.orientation;
        }
      }
    }

    return moves;
  }

  private int calcTotalHieght(Block[][] grid) {
    int total = 0;
    for (int x = 0; x < grid.length; x++) {
      total += getColHeight(grid, x, false);
    }
    return total;
  }

  private int calcLines(Block[][] grid) {
    int lines = 0;

    for (int y = 0; y < grid[0].length; y++) {
      int counter = 0;
      for (int x = 0; x < grid.length; x++) {
        if (grid[x][y] != null)
          counter++;
      }

      if (counter == 10)
        lines++;
    }

    return lines;
  }

  private int calcHoles(Block[][] grid) {
    
    int holes = 0;

    
    for (int x = 0; x < grid.length; x++) {
      int yStart = getColHeight(grid, x, true) + 1;
      for (int y = yStart; y < grid[0].length; y++) {
        if (grid[x][y] == null)
          holes++;
      }
    }
    
    return holes;
  }

  private double calcRoughness(Block[][] grid) {
    double total = 0;

    for (int x = 0; x < grid.length-1; x++) {
      total += Math.abs(getColHeight(grid, x, false) - getColHeight(grid, x+1, false));
    }

    return total;
  }

  private int getColHeight(Block[][] grid, int col, boolean returnGridHeight) {
    for (int y = 0; y < grid[0].length; y++) {
      if (grid[col][y] != null) {
        if (returnGridHeight)
          return y;
        else
          return grid[0].length - y;
      }
    }
    if(returnGridHeight)
      return 24;
    else
      return 0;
  }


  //Generates the input queue given a piece position and rotation
  private void generateInputQueue(GridManager GridManager) {
    float[] aiOutput = getAIResponse(GridManager.clone());
    GridManager.AITargetPos = aiOutput[0];
    GridManager.AITargetRot = round(aiOutput[1]);
  }
}
