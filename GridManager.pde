public class GridManager implements IDisplay {

  /*
  *Constants
   */

  //General settings
  private final int pieceSpawnX;
  private final float nextBoxX;
  private final float nextBoxY;
  private final int w;
  private final int h;
  private final int linesToLevelUp;
  private final float levelFactor;
  private final int intenseColor;
  private final int pieceSpawnDelay;

  //level speeds
  private final int[] levelSpeeds =        {45, 40, 35, 30, 26, 22, 18, 14, 11, 8, 6, 5, 4, 3, 2, 1, 0};
  private final int[] levelIntenseSpeeds = {35, 30, 25, 22, 18, 15, 13, 11, 8, 6, 5, 4, 3, 2, 1, 0, 0}; 

  //Color Schemes
  private final int[][] colorScheme = {{#f44141, #f4a941, #f4e541, 0, 35}, 
    {#a3f441, #4192f4, #42f4c2, 100, 140}, 
    {#9141f4, #f441d3, #f44167, 190, 240}, 
    {#eff2f7, #c43131, #ffa530, 5, 20}, 
    {#aff441, #ebf441, #f49b42, 20, 50}
  };


  /*
  *Fields
   */

  private GameManager gameManager;

  private Block[][] blocks;
  private float spacing;
  private float GridManagerEndPos;

  private Piece currentPiece;
  private Piece nextPiece;
  private int nextPieceTimer;
  private float AITargetPos;
  private int AITargetRot;

  private int topLine;
  private int totalLinesCleared;
  private ArrayList<Integer> linesToClear;
  private int drought;
  private int lastDrought;

  private int fallRate;
  private int fallTimer;
  private int shakeTimer;
  private float graphicsTimer;
  private int streakEffectTimer;

  private ArrayList<Effect> effects;

  private PImage gridImage;

  // Default Constructor
  public GridManager(GameManager gameManager) {

    //Setting constants
    this.pieceSpawnX = 5;
    this.nextBoxX = 12.5;
    this.nextBoxY = 4;
    this.w = 10;
    this.h = 24;
    this.linesToLevelUp = 5;
    this.levelFactor = 0.8;
    this.intenseColor = 45;
    this.pieceSpawnDelay = 10;

    //setting fields
    this.gameManager = gameManager;

    this.blocks = new Block[10][24];
    this.spacing = height/24f;
    this.GridManagerEndPos = this.spacing*this.blocks.length;

    this.currentPiece = generateRandomPiece();
    this.nextPiece = generateRandomPiece();
    this.nextPiece.setPos(nextBoxX, nextBoxY);
    this.nextPieceTimer = 0;
    this.AITargetPos = 0;
    this.AITargetRot = 0;
    
    this.topLine = h-1;
    this.totalLinesCleared = 0;
    this.linesToClear = new ArrayList<Integer>();
    this.drought = 0;
    this.lastDrought = 0;

    this.fallRate =  this.levelSpeeds[gameManager.getLevel()];
    this.fallTimer = fallRate;
    this.shakeTimer = 0;
    this.graphicsTimer = 0;
    this.streakEffectTimer = 0;

    this.effects = new ArrayList<Effect>();

    this.gridImage = loadImage("gridImage.tif");
  }

  public GridManager clone() {
    GridManager tempGridManager = new GridManager(gameManager);

    for (int i = 0; i < this.blocks.length; i++) {
      for (int j = 0; j < this.blocks[1].length; j++) {
        if (this.blocks[i][j] != null) {
          tempGridManager.blocks[i][j] = this.blocks[i][j].clone();
        }
      }
    }

    tempGridManager.currentPiece = this.currentPiece.clone();

    return tempGridManager;
  }
  
  public void moveToAITarget(){
    if(currentPiece.orientation != AITargetRot)
      rotateCurrentPiece(true, false);
    else{
      if(currentPiece.x < AITargetPos)
        moveCurrentPiece(Direction.RIGHT, true);
      else if(currentPiece.x > AITargetPos)
        moveCurrentPiece(Direction.LEFT, true);
      else
        moveCurrentPiece(Direction.DOWN, true);
    }
  }

  public void update() {

    //move piece
    if (!currentPiece.isLocked()) {
      if (fallTimer > 0) {
        fallTimer--;
      } else {
        moveCurrentPiece(Direction.DOWN, true);
        fallTimer = fallRate;
      }
    } else if (nextPieceTimer > 0) {
      //Waiting for next piece to spawn
      nextPieceTimer--;
    } else {
      if (linesToClear.size() > 0) {
        clearLines();
      }
      pushPiece();
    }

    //Running all effects
    for (int i = effects.size()-1; i >= 0; i--) {
      Effect E = effects.get(i);
      if (E.getLifeTime() <= 0)
        effects.remove(i);
      else {
        if (E.canOnlyRunIntense()) {
          if (gameManager.isIntenseMode())
            E.run();
          else
            effects.remove(i);
        } else {
          E.run();
        }
      }
    }
  }

  @Override 
    public void display(boolean intenseMode) {

    if (!intenseMode) {
      background(0);
      //image(gridImage, 0, 0);
      graphicsTimer = 0;
    } else {
      colorMode(HSB);
      background(map(sin(graphicsTimer), -1, 1, colorScheme[gameManager.getLevel() % 5][3], colorScheme[gameManager.getLevel() % 5][4]), 255, 255);
      graphicsTimer += .03;
    }

    // Display blocks
    for (int i = 0; i < this.blocks.length; i++) {
      for (int j = 0; j < this.blocks[1].length; j++) {
        if (this.blocks[i][j] != null) {
          this.blocks[i][j].display(intenseMode);
        }
      }
    }

    nextPiece.display(intenseMode);
    currentPiece.display(intenseMode);


    if (intenseMode) {
      if (streakEffectTimer > 0) {
        streakEffectTimer--;
      } else {
        streakEffectTimer = 7;
        effects.add(new StreakEffect(random(0, spacing * 10), height * 1.4, 200, true, 20));
      }
    }

    for (Effect E : effects) {
      if (E.canOnlyRunIntense()) {
        if (intenseMode)
          E.display(intenseMode);
      } else {
        E.display(intenseMode);
      }
    }
  }

  //Piece functionality methods

  public void pushPiece() {
    
    this.currentPiece = this.nextPiece;
    this.currentPiece.setPos(pieceSpawnX, getPieceSpawnY(this.currentPiece.getType()));

    //Checking if the player lost
    for (Block b : this.currentPiece.getBlocks()) {
      if (this.blocks[b.getX()][b.getY()] != null) {
        //Jeff Mode
        if(gameManager.jeff) {
          gameManager.soundManager.playSoundEffect("Lost", false);
        }

        gameManager.reset();
        return;
      }
    }

    this.nextPiece = this.generateRandomPiece();
    this.nextPiece.setPos(nextBoxX, nextBoxY);
    
    if (gameManager.inactiveTimer == 0) {
      gameManager.ai.generateInputQueue(this);
    }
    
    //Handle Droughts
    if(this.currentPiece.type == PieceType.I) {
      this.lastDrought = this.drought;
      this.drought = 0;
    } else {
      this.drought++;
    }
    
    //Jeff Mode
    if(gameManager.jeff) {
      if((this.drought == 15) && (this.currentPiece.type != PieceType.I)) {
        gameManager.soundManager.playSoundEffect("Drought", false);
      }

      if((this.lastDrought >= 15) && (this.currentPiece.type == PieceType.I)) {
        gameManager.soundManager.playSoundEffect("LongBar", false);
      }
    }

  }

  public Piece generateRandomPiece() {

    Random rng = new Random();  //Creating a random number generator
    int x = rng.nextInt(7);

    float y = getPieceSpawnY(PieceType.get(x));
    Piece piece = new Piece(pieceSpawnX, y, PieceType.get(x), this.spacing, getColorOfPiece(PieceType.get(x)), false);  //Create a piece with random piece type

    return piece;
  }
  
  
  // Returns true if piece has collided
  private boolean moveCurrentPiece(Direction dir, boolean handleCollisions) {
    switch(dir) {
    case DOWN:
      if (!checkCollisions(dir, handleCollisions)) {
        currentPiece.setY(currentPiece.getY() + 1);
        return false;
      }
      return true;
    case LEFT:
      if (!checkCollisions(dir, handleCollisions)) {
        currentPiece.setX(currentPiece.getX() - 1);
        return false;
      } else {
        gameManager.charge(InputType.LEFT);
        return true;
      }
    case RIGHT:
      if (!checkCollisions(dir, handleCollisions)) {
        currentPiece.setX(currentPiece.getX() + 1);
        return false;
      } else {
        gameManager.charge(InputType.RIGHT);
        return true;
      }
    default:
      break;
    }
    currentPiece.updateBlocks();
    return false;
  }

  public void rotateCurrentPiece(boolean cw, boolean dontCheck) {
    switch(currentPiece.type) {
    case O:
      break;

    case S:
    case Z:
    case I:
      if (currentPiece.orientation == 1) {
        if (dontCheck || !checkRotationCollision(false))
          currentPiece.rotate(false);
      } else
        if (dontCheck || !checkRotationCollision(true))
          currentPiece.rotate(true);
      break;

    case L:
    case J:
    case T:
      if (dontCheck || !checkRotationCollision(cw))
        currentPiece.rotate(cw);
      break;
    default:
      break;
    }
  }


  //Calculation methods

  /* 
   Directions:
   1 = down
   2 = left
   3 = right
   */
private boolean checkCollisions(Direction dir, boolean handleCollisions) {
    if (!currentPiece.isLocked()) {
      Block[] pieceBlocks = new Block[currentPiece.getBlocks().length];

      //Clone piece blocks and move the piece by 1 unit
      for (int i = 0; i < pieceBlocks.length; i++) {
        pieceBlocks[i] = currentPiece.getBlocks()[i].clone();
        switch(dir) {
        case DOWN:
          pieceBlocks[i].setY(pieceBlocks[i].getY() + 1);
          break;
        case LEFT:
          pieceBlocks[i].setX(pieceBlocks[i].getX() - 1);
          break;
        case RIGHT:
          pieceBlocks[i].setX(pieceBlocks[i].getX() + 1);
          break;
        default:
          break;
        }
      }

      //Iterating through pieces
      for (int i = 0; i < pieceBlocks.length; i++) {
        if (pieceBlocks[i].y >= h) {
          //Collision with bottom
          if(handleCollisions)
            handleCollision();
          return true;
        } else if (pieceBlocks[i].x >= w || pieceBlocks[i].x < 0) {
          return true;
        } else {
          for (int x = 0; x < this.blocks.length; x++) {
            for (int y = 0; y < this.blocks[0].length; y++) {
              if (this.blocks[x][y] != null) {
                Block b = this.blocks[x][y].clone();
                if ((b.x == pieceBlocks[i].x) && (b.y == pieceBlocks[i].y)) {
                  if (dir == Direction.DOWN && handleCollisions)
                    handleCollision();
                  return true;
                }
              }
            }
          }
        }
      }

      return false;
    } else {
      return true;
    }
  }

  public boolean checkRotationCollision(boolean cw) {
    Block[] pieceBlocks = new Block[currentPiece.getBlocks().length];

    rotateCurrentPiece(cw, true);

    for (int i = 0; i < pieceBlocks.length; i++) {
      pieceBlocks[i] = currentPiece.getBlocks()[i].clone();
    }

    rotateCurrentPiece(!cw, true);

    for (int i = 0; i < pieceBlocks.length; i++) {
      if (pieceBlocks[i].y >= h || pieceBlocks[i].x >= w || pieceBlocks[i].x < 0) {
        return true;
      } else {
        for (int x = 0; x < this.blocks.length; x++) {
          for (int y = 0; y < this.blocks[0].length; y++) {
            if (this.blocks[x][y] != null) {
              Block b = this.blocks[x][y].clone();
              if ((b.x == pieceBlocks[i].x) && (b.y == pieceBlocks[i].y)) 
                return true;
            }
          }
        }
      }
    }
    return false;
  }

  private void handleCollision() {
    Block[] pieceBlocks = this.currentPiece.getBlocks().clone();

    for (int z = 0; z < pieceBlocks.length; z++) {
      int x = pieceBlocks[z].getX();
      int y = pieceBlocks[z].getY();
      if (y >= 0)
        this.blocks[x][y] = pieceBlocks[z].clone();
      else
        gameManager.reset();
    }

    checkLines();

    topLine = calculateTopLine();

    effects.add(new ExplodeEffect(this.currentPiece.getX()*spacing, this.currentPiece.getY()*spacing, 10, true, spacing));
    shakeTimer = 8;

    this.currentPiece.setLocked(true);
    nextPieceTimer = pieceSpawnDelay;
  }


  private int calculateTopLine() {
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if (this.blocks[x][y] != null)
          return y;
      }
    }

    return h-1;
  }

  private void checkLines() {
    this.linesToClear = new ArrayList<Integer>();

    for (int y = 0; y < this.blocks[0].length; y++) {
      int counter = 0;  //Number of squares that are filled

      for (int x = 0; x < this.blocks.length; x++) {
        if (this.blocks[x][y] != null) 
          counter++;
      }

      if (counter == w) {  //All squares are filled
        effects.add(new LineClearEffect(0, (y)*spacing, 10, false, spacing));
        this.linesToClear.add(y);
      }
    }

    if(linesToClear.size() == 0) {
      gameManager.soundManager.playSoundEffect("Drop", gameManager.intenseMode);
    } else if(linesToClear.size() == 4) {
      //add tetris visual effect
      float avgY = 0;

      for (int i = 0; i < 4; i++) {
        avgY += linesToClear.get(i);
      }

      avgY /= 4;

      effects.add(new LineExplodeEffect((spacing*10)/2, avgY*spacing, 200, true, 13));

      //play tetris sound effect
      gameManager.soundManager.playSoundEffect("Tetris", gameManager.intenseMode);
    } else {
      gameManager.soundManager.playSoundEffect("Clear", gameManager.intenseMode);
    }


    if (linesToClear.size() == 4) {

    }
  }

  private void clearLines() {
    for (int i = 0; i < this.linesToClear.size(); i++) {
      clearLine(this.linesToClear.get(i));
    }

    // Add to score
    float factor = 1 + levelFactor * gameManager.getLevel();
    if (linesToClear.size() == 4) {
      gameManager.setScore(round(gameManager.getScore() + (1000 * factor)));

      gameManager.handleTetrisForJeff();

      linesToClear = new ArrayList<Integer>();
    } else if (linesToClear.size() == 3) {
      gameManager.setScore(round(gameManager.getScore() + (600 * factor)));
      linesToClear = new ArrayList<Integer>();
    } else if (linesToClear.size() == 2) {
      gameManager.setScore(round(gameManager.getScore() + (300 * factor)));
      linesToClear = new ArrayList<Integer>();
    } else {
      gameManager.setScore(round(gameManager.getScore() + (100 * factor)));
      linesToClear = new ArrayList<Integer>();
    }

    //Add to level
    if ((totalLinesCleared >= linesToLevelUp * (gameManager.getLevel()+1)) && gameManager.level != 16) {
      levelUp();
    }

    //Jeff mode
    if(gameManager.jeff) {
      int top10Score = gameManager.displayedScores.getInt(gameManager.displayedScores.getRowCount()-1, "score");
      int top2Score = 0;
      if(gameManager.displayedScores.getRowCount() >= 2) {
        top2Score = gameManager.displayedScores.getInt(1, "score");
      }

      if((!gameManager.madeTop10) && (gameManager.score > top10Score)) {
        gameManager.soundManager.playSoundEffect("NeckandNeck", false);
        gameManager.madeTop10 = true;
      }

      if((!gameManager.madeTop2) && (gameManager.score > top2Score)) {
        gameManager.soundManager.playSoundEffect("Top2", false);
        gameManager.madeTop2 = true;
      }
    }
    
  }

  private void clearLine(int line) {

    for (int y = line - 1; y >= 0; y--) {
      for (int x = 0; x < w; x++) {
        Block b = this.blocks[x][y];
        if (b != null)
          this.blocks[x][y].setY(this.blocks[x][y].getY() + 1);

        this.blocks[x][y+1] = this.blocks[x][y];
      }
    }
    totalLinesCleared++;
  }

  private void levelUp() {

    gameManager.setLevel(gameManager.getLevel() + 1);

    //update block colors
    for (Block[] bArray : this.blocks) {
      for (Block b : bArray) {
        if (b != null) {
          b.setColor(getColorOfPiece(b.getPieceType()));
        }
      }
    }

    //update next piece color
    this.nextPiece.setColor(getColorOfPiece(this.nextPiece.getType()));


    //change fall rate
    if (gameManager.isIntenseMode())
      fallRate = levelIntenseSpeeds[Math.min(gameManager.getLevel(), levelSpeeds.length - 1)];
    else
      fallRate = levelSpeeds[Math.min(gameManager.getLevel(), levelSpeeds.length-1)];
  }

  private int[] getColorOfPiece(PieceType type) {
    int[] c = new int[2];
    switch(type) {
    case I:
    case O:
    case T:
      c[0] = colorScheme[gameManager.getLevel() % 5][0];
      break;
    case L:
    case Z:
      c[0] = colorScheme[gameManager.getLevel() % 5][1];
      break;
    case J:
    case S:
      c[0] = colorScheme[gameManager.getLevel() % 5][2];
      break;

    default:
      break;
    }
    c[1] = intenseColor;

    return c;
  }

  public float getPieceSpawnY(PieceType p) {
    float y;
    switch(p) {
    case S:
    case Z:
    case O:
      y = 0.5;
    default:
      y = 0;
      return y;
    }
  }

  // Getters and setters
  public float getGridManagerEndPos() {
    return this.GridManagerEndPos;
  }

  public Piece getCurrentPiece() {
    return this.currentPiece;
  }

  public void setFallRate(int fallRate) {
    this.fallRate = fallRate;
  }

  public int getFallRate() {
    return this.fallRate;
  }

  public int getFallTimer() {
    return this.fallTimer;
  }

  public int getTopLine() {
    return this.topLine;
  }

  public int getTotalLinesCleared() {
    return this.totalLinesCleared;
  }

  public void setFallTimer(int fallTimer) {
    this.fallTimer = fallTimer;
  }

  public void setCurrentPiece(Piece currentPiece) {
    this.currentPiece = currentPiece;
  }
}