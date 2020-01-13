import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.*; 
import ddf.minim.*; 
import ddf.minim.*; 
import ddf.minim.analysis.*; 
import java.io.BufferedReader; 
import java.io.FileReader; 
import java.io.IOException; 
import java.util.HashMap; 
import java.util.Map; 
import java.util.regex.Matcher; 
import java.util.regex.Pattern; 
import processing.io.*; 
import java.util.Random; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class TetrisPi extends PApplet {




GameManager gameManager;

public void setup() {
  
  gameManager = new GameManager(this, false);

  gameManager.startGame(0);
}

public void draw() { 
  gameManager.run();
}  

public void keyPressed(){
  gameManager.inputManager.setPressInput();
}

public void keyReleased(){
  gameManager.inputManager.setReleaseInput();
}

public void buttonEvent(int pin) {
  if(GPIO.digitalRead(pin) == GPIO.LOW) {
    gameManager.inputManager.setControllerInput(pin, true);
  } else {
    gameManager.inputManager.setControllerInput(pin, false);
  }
}
public class AI {

  private GameManager gameManager;

  //Height, lines, holes, roughness
  private double[] weights = {0.510066f, 0.760666f, 0.35663f, 0.184483f};
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
public class Block implements IDisplay {

  private int[] blockColor;
  private int x;
  private int y;
  private float size;
  private float intenseSize;
  private PieceType pieceType;

  public Block(int x, int y, float size, int[] blockColor, PieceType pieceType) {
    this.x = x;
    this.y = y;
    this.blockColor = blockColor;
    this.size = size;
    this.intenseSize = size*.9f;
    this.pieceType = pieceType;
  }

  public Block clone() {
    return new Block(this.x, this.y, this.size, this.blockColor, this.pieceType);
  }

@Override 
  public void display(boolean intenseMode) {
  rectMode(CORNER);
  if (intenseMode) {
   noStroke();
   fill(45);
   rect(x*size, y*size, intenseSize, intenseSize);
   if(intenseSize < size*.9f)
    intenseSize++;
  } else {
   fill(blockColor[0]);
   //stroke(180);
   //strokeWeight(2);
   noStroke();
   ellipseMode(CORNER);
   rect(x*size, y*size, size-2, size-2);
   fill(0, 50);
   rect(x*size, y*size, size-6, size-6);
   ellipseMode(CENTER);
  }
 }

  public int[] getColor() {
    return this.blockColor;
  }
  
  public void setColor(int[] blockColor) {
    this.blockColor = blockColor;
  }
  
  public void setPos(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public void setX(int x) {
    this.x = x;
  }

  public void setY(int y) {
    this.y = y;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }
  
  public PieceType getPieceType() {
    return this.pieceType;
  }
  
  public void setPieceType(PieceType pieceType) {
    this.pieceType = pieceType;
  }
}
public class Button implements IDisplay {

  //fields
  private String id;
  private float x;
  private float y;
  private float w;
  private float h;
  private int col;

  private boolean selected;
  private int selectedColor;

  Button() {
    //default values
    this.id = "button";
    this.x = 0;
    this.y = 0;
    this.w = 15;
    this.h = 10;
    this.col = 255;
    this.selected = false;
    this.selectedColor = 0xff89ffff;
  }

  Button(float x, float y, float w, float h, int col) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.col = col;
    this.selected = false;
    this.selectedColor = 0xffdddddd;
  }

  Button(float x, float y, float w, float h, int col, boolean selected, int selectedColor) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.col = col;
    this.selected = selected;
    this.selectedColor = selectedColor;
  }

  public void display(boolean intenseMode) {

    noStroke();
    if (selected) {
      fill(this.selectedColor);
    } else {
      fill(this.col);
    }

    rectMode(CENTER);
    rect(this.x, this.y, this.w, this.h);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean getSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }
}
public enum Direction {
  DOWN(0),
  LEFT(1),
  RIGHT(2),
  UP(3)
  ;
  
   private int directionIndex;

   private Direction(int index) { this.directionIndex = index; }

   public static Direction get(int index) {
      for (Direction l : Direction.values()) {
          if (l.directionIndex == index) return l;
      }
      throw new IllegalArgumentException("Direction not found");
   }
   
   public int getIndex() {
     return directionIndex;
   }
}
abstract class Effect implements IDisplay{
  
  protected float x;
  protected float y;
  protected int lifeTime;
  protected boolean onlyRunInIntense;
  
  public Effect(float x, float y, int lifeTime, boolean onlyRunInIntense){
    this.x = x;
    this.y = y;
    this.lifeTime = lifeTime;
    this.onlyRunInIntense = onlyRunInIntense;
  }
  
  public abstract void run();
  
  public boolean canOnlyRunIntense(){
    return this.onlyRunInIntense;
  }
  
  public int getLifeTime(){
    return this.lifeTime;
  }
}
class ExplodeEffect extends Effect {
  private float size;

  ExplodeEffect(float x, float y, int lifeTime, boolean runInIntense, float size) {
    super(x, y, lifeTime, runInIntense);
    this.size = size*2;
  }

  public void run() {
    lifeTime--;
    size *= 1.2f;
  }

  @Override 
    public void display(boolean intenseMode) {
    if (intenseMode) {
      strokeWeight(4);
      stroke(255, 255 - size*1.4f);
      noFill();
      rectMode(CENTER);
      rect(x, y, size, size);
    } else {
      println("Only runs in intense mode");
    }
  }
}



public class GameManager implements IDisplay {

  // Fields
  private int score;
  private Table highScores;
  private Table displayedScores;
  private int startLevel;
  private int oldStartLevel;
  private int level;
  private int beginningCountDown;
  private int countDown;
  private int startDelay;
  private boolean intenseMode;
  private String name;
  private String oldName;
  private boolean inMenu;
  private boolean usingController;

  private int inactiveTimer = 1200;

  //font
  private PFont classicFont;
  private PFont modernFont;

  //managers (basically subclasses of the game manager)
  private InputManager inputManager;
  private GridManager gridManager;
  private MenuManager menuManager;
  private SoundManager soundManager;

  AI ai;

  //Cheat Codes
  private boolean jeff;
  private boolean alwaysIntense;
  private boolean madeTop10;
  private boolean madeTop2;

  //Default Constructor
  public GameManager(PApplet application, boolean usingController) {

    this.score = 0;
    this.startLevel = 0;
    this.oldStartLevel = 0;
    this.level = 0;
    this.beginningCountDown = 20*60;
    this.countDown = this.beginningCountDown;
    this.startDelay = 20;
    this.intenseMode = false;
    this.name = "JEFF";
    this.oldName = "JEFF";
    this.inMenu = false;
    this.usingController = usingController;

    if(this.usingController) {
      this.setupController(application);
    }
    
    this.classicFont = createFont("tetrisFont.ttf", 64);
    this.modernFont = createFont("modernFont.ttf", 64);
    textFont(this.classicFont); 
    loadScores();
    this.displayedScores = this.getScoresNoDuplicates();

    this.inputManager = new InputManager();
    this.menuManager = new MenuManager(this);
    this.soundManager = new SoundManager(new Minim(application));
    this.gridManager = new GridManager(this);

    this.ai = new AI(this);

    //misc
    this.jeff = false;
    this.madeTop10 = false;
    this.madeTop2 = false;;
  }

  private void setupController(PApplet application) {
    int[] pins = gameManager.inputManager.inputPins;
    for(int i = 0; i < pins.length; i++) {
      GPIO.pinMode(pins[i], GPIO.INPUT_PULLUP);
      GPIO.attachInterrupt(pins[i], application, "buttonEvent", GPIO.CHANGE);
    }
  }

  private void loadScores() {
    File temp = new File(dataPath("scores.csv"));
    //initializing the high score table
    if (temp.exists()) {
      this.highScores = loadTable("scores.csv", "header, csv");
    } else {
      //create new table
      this.highScores = new Table();
      this.highScores.addColumn("name");
      this.highScores.addColumn("score");
      saveTable(this.highScores, "data/scores.csv", "csv");
    }
  }


  private void saveScore() {

    TableRow newRow = this.highScores.addRow();
    newRow.setString("name", this.name);
    newRow.setInt("score", score);

    //sort new score by insertion
    if (score > 0) {
      int i = this.highScores.getRowCount() - 2;  //second to last row
      while ((i >= 0) && (this.highScores.getRow(i+1).getInt("score") > this.highScores.getRow(i).getInt("score"))) {
        TableRow swappedRow1 = this.highScores.getRow(i);
        TableRow swappedRow2 = this.highScores.getRow(i+1);
        String tempName = swappedRow1.getString("name");
        int tempScore = swappedRow1.getInt("score");
        swappedRow1.setString("name", swappedRow2.getString("name"));
        swappedRow1.setInt("score", swappedRow2.getInt("score"));
        swappedRow2.setString("name", tempName);
        swappedRow2.setInt("score", tempScore);
        i--;
      }
    }

    saveTable(this.highScores, "data/scores.csv", "csv");
    this.displayedScores = this.getScoresNoDuplicates();
  }

  //Returns a table of high scores with no duplicate names
  public Table getScoresNoDuplicates() {
    //remove duplicate names and create a new table with this data
    Table noDuplicates = new Table();
    noDuplicates.addColumn("name");
    noDuplicates.addColumn("score");
    ArrayList<String> names = new ArrayList<String>();
    for(TableRow row : this.highScores.rows()) {
      if(!names.contains(row.getString("name"))) {
        noDuplicates.addRow(row);
        names.add(row.getString("name"));
      }
    }

    return noDuplicates;
  }


  public void startGame(int startLevel) {
    //Cheat Codes
    switch(this.name) {
    case "JEFF":
      this.jeff = true;
      this.madeTop10 = false;
      this.madeTop2 = false;
      break;
    case "JOSEPH":
      this.startLevel = 15;
      break;
    case "INTENSE":
      this.alwaysIntense = true;
      break;
    case "LOGAN":
    case "MARK":
      this.alwaysIntense = true;
      this.startLevel = 16;
      break;
    default:
      this.jeff = false;
      this.alwaysIntense = false;
      this.startLevel = startLevel;
      break;
    }
    
    //Deleting names
    if(this.name.startsWith("DEL")) {
      for(int i = 0; i < 10; i++) {
        if(this.name.equalsIgnoreCase("DEL" + i)) {
          deleteScoreByName(this.displayedScores.getRow(i).getString("name"), i);
        }
      }
    }

    //Setting start values
    this.score = 0;
    this.level = this.startLevel;
    this.gridManager = new GridManager(this);
    this.countDown = this.soundManager.gameMusic.length();

    this.startDelay = 20;

    if(this.alwaysIntense) {
      this.intenseMode = true;
      soundManager.startMusicFromLoop();
    } else {
      this.intenseMode = false;
      soundManager.startMusicFromBeginning();
    }

    this.oldName = this.name;
  }
  
  private void deleteScoreByName(String name, int rowIndex) {
    //Delete name from high scores
    for(int j = 0; j < this.highScores.getRowCount(); j++) {
      if(this.highScores.getRow(j).getString("name") == name) {
        this.highScores.removeRow(j);  
      }      
    }
  
    //delete name from displayed scores
    this.displayedScores.removeRow(rowIndex);
  }

  public void updateGame() {
    soundManager.rewindAllSoundEffects();
    if (!(countDown > 0)) {
      if(this.alwaysIntense) {
        this.soundManager.gameMusic.skip(soundManager.transitionPoints[0] - this.soundManager.gameMusic.position());
      } else {
        changeMode();
      }
    }

    this.countDown = soundManager.transitionPoints[soundManager.currentMusic] - soundManager.gameMusic.position();

    if (startDelay > 0) {
      startDelay--;
    } else {
      gridManager.update();
    }

    if (intenseMode)
      shakeScreen();
  }

  public void reset() {

    if (inactiveTimer > 0) {
      inactiveTimer = 1200;
    }

    soundManager.rewindMusic();
    if (score > 0)
      saveScore();
    startGame(this.startLevel);
  }

  private void run() {

    if(usingController) {
      inputManager.checkControllerInputs();
    }
    inputManager.updateInputTriggers();
    evaluateInputs();

    if (inMenu) {
    } else {
      updateGame();
      if (inactiveTimer == 0) {
        gridManager.moveToAITarget();
        this.name = "AI";
      }
    }

    gridManager.display(intenseMode);
    display(intenseMode);

    if (inactiveTimer > 0)
      inactiveTimer--;
  }

  private void evaluateInputs() {
    for (int i = 0; i < inputManager.triggers.length; i++) {
      if (inputManager.triggers[i] == true) {
        this.executeFunction(InputType.get(i));
      }
    }
  }

  private void executeFunction(InputType input) {
    switch(input) {

    case LEFT:
      if (inMenu) {
        menuManager.executeFunction(input);
      } else {
        gridManager.moveCurrentPiece(Direction.LEFT, true);
      }
      inputManager.inputs[1] = false;
      inputManager.triggers[1] = false;
      break;

    case RIGHT:
      if (inMenu) {
        menuManager.executeFunction(input);
      } else {
        gridManager.moveCurrentPiece(Direction.RIGHT, true);
      }
      inputManager.inputs[0] = false;
      inputManager.triggers[0] = false;
      break;

    case UP:
      if (inMenu) {
        menuManager.executeFunction(input);
      } else {
        soundManager.gameMusic.pause(); 
        this.oldStartLevel = this.startLevel;
        this.oldName = name;
        inputManager.changeDelays(true);
        inMenu ^= true;
      }
      break;

    case DOWN:
      if (inMenu) {
        menuManager.executeFunction(input);
      } else {
        gridManager.moveCurrentPiece(Direction.DOWN, true);
      }
      break;

    case A:
      if (inMenu) {
        menuManager.executeFunction(input);
      } else {
        gridManager.rotateCurrentPiece(false, false);
      }
      break;

    case B:
      if (inMenu) {
        menuManager.executeFunction(input);
      } else {
        gridManager.rotateCurrentPiece(true, false);
      }
      break;

    default:
      break;
    }

    //Stops the AI from playing if the player has been inactive
    if (inactiveTimer == 0) {
      reset();
      soundManager.gameMusic.pause(); 
      this.oldStartLevel = this.startLevel;
      this.oldName = name;
      inMenu = true;
    }

    inactiveTimer = 1200;
  }

  public void shakeScreen() {
    if (gridManager.shakeTimer > 0) {
      translate(random(-width/50, width/50), random(-height/50, height/50));
      gridManager.shakeTimer--;
    } else {
      translate(0, 0);
    }
  }

  private void changeMode() {
    intenseMode ^= true;
    if (intenseMode) {
      if (soundManager.currentMusic == 2)
        soundManager.gameMusic.skip(soundManager.transitionPoints[0] - soundManager.transitionPoints[2]);  //skips from the end of the song to the loop point

      textFont(this.modernFont);

      soundManager.currentMusic = 1;

      //Transition animation

      gridManager.setFallRate(gridManager.levelIntenseSpeeds[level]);
    } else {
      soundManager.currentMusic = 2;
      textFont(this.classicFont);
      gridManager.setFallRate(gridManager.levelSpeeds[level]);
    }
  }

  public void charge(InputType input) {
    inputManager.timers[input.getIndex()] = 0;
  }

  public void handleTetrisForJeff() {
    if(this.jeff) {
      if(this.gridManager.lastDrought >= 15) {
        soundManager.playSoundEffect("IntenseBoom", false);
      } else {
        Random rng = new Random();
        int x = rng.nextInt(5);
        soundManager.playSoundEffect("BoomTetrisForJeff" + x, false);
      }
    }
  }

  @Override public void display(boolean intenseMode) {
    pushMatrix();
    textAlign(CENTER, CENTER);

    // Setup position of UI
    if (intenseMode) {
      fill(0);
      stroke(0);
    } else {
      fill(gridManager.currentPiece.pieceColor[0]);
      stroke(gridManager.currentPiece.pieceColor[0]);
    }

    textSize(width*.09f);
    strokeWeight(2);

    line(gridManager.getGridManagerEndPos(), 0, gridManager.getGridManagerEndPos(), height);
    translate(gridManager.getGridManagerEndPos() + (width-gridManager.getGridManagerEndPos())/2, 0);

    // Draw UI text
    text("TETRIS", 0, height*.05f);

    textSize(width*.08f);
    text("score\n" + score, 0, height*.35f);

    text("level\n" + level, 0, height*.55f);

    text("lines\n" + gridManager.getTotalLinesCleared(), 0, height * 0.75f);

    textSize(width*.13f);
    text(countDown/1000, 0, height*.9f);
    popMatrix();

    if (inMenu) {
        menuManager.display(this.intenseMode);
    } else if (gameManager.inactiveTimer == 0) {
      //Display High Scores with AI playing

      noStroke();
      fill(0, 160);
      rectMode(CENTER);
      rect(width/2, height/2, width*1.2f, height*1.2f);

      if (intenseMode) {
        fill(255);
      } else {
        fill(gridManager.currentPiece.pieceColor[0]);
      }

      float ySpace = height*.04f;
      textSize(ySpace);

      for (int i = 0; i < Math.min(this.displayedScores.getRowCount(), 10); i++) {
        TableRow row = this.displayedScores.getRow(i);
        text(row.getString("name") + " " + row.getInt("score"), 100, height*.1f + (ySpace*i));
      }
    }
  }



  //getters/setters

  public boolean isUsingController() {
    return usingController;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public boolean isIntenseMode() {
    return intenseMode;
  }

  public void setIntenseMode(boolean intenseMode) {
    this.intenseMode = intenseMode;
  }

  public Table getHighScores() {
    return this.highScores;
  }


  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }
}
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
  private final int[][] colorScheme = {{0xfff44141, 0xfff4a941, 0xfff4e541, 0, 35}, 
    {0xffa3f441, 0xff4192f4, 0xff42f4c2, 100, 140}, 
    {0xff9141f4, 0xfff441d3, 0xfff44167, 190, 240}, 
    {0xffeff2f7, 0xffc43131, 0xffffa530, 5, 20}, 
    {0xffaff441, 0xffebf441, 0xfff49b42, 20, 50}
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
    this.nextBoxX = 12.5f;
    this.nextBoxY = 4;
    this.w = 10;
    this.h = 24;
    this.linesToLevelUp = 5;
    this.levelFactor = 0.8f;
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
      graphicsTimer += .03f;
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
        effects.add(new StreakEffect(random(0, spacing * 10), height * 1.4f, 200, true, 20));
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
      y = 0.5f;
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
interface IDisplay {
  public void display(boolean intenseMode);
}








//Note: This code is largely third party. However, all of the comments were made by me (Logan Bowers) in order to understand what I was using.

/*
 * An ini file is a file used to hold data/configuration settings by using key value pairs.
 * ini files are organized by sections starting with [ and ending with ]. For example: [my section]
 * Key value pairs are denoted "key = value".
 * In this class, values can be accessed as a string, int, float, or double
 */

public class IniFile {

   //These patterns are used by the matcher to search for specific
   private Pattern  _section  = Pattern.compile( "\\s*\\[([^]]*)\\]\\s*" );  //Regex expression used to find a section header in the file
   private Pattern  _keyValue = Pattern.compile( "\\s*([^=]*)=(.*)" );  //Regex expression used to find a key value pair in the file
   
   private Map<String, Map<String, String>>  _entries  = new HashMap<String, Map<String, String>>();  //Entries described by a Section and a Key, Value pair.

   public IniFile(String path) throws IOException {  //IniFile is instantiated by passing a path name
      parse(path);  //Parse an ini file with the specified path.
   }

   public void parse(String path) throws IOException {
     
      BufferedReader br = new BufferedReader(new FileReader(path));  //Buffered Reader pushes lines through the input stream when the read() function is called.
      try {
        
       //Strings used to hold information within the file
         String line;
         String section = null;
         
         /*
         *This while loop executes for every line in the file detected by the buffered reader.
         *Each line is matched against one of the patterns declared at the top of the class, 
         *which is then used to create a mapping entry in _entries.
         */
         while((line = br.readLine()) != null ) {  //While a line exists to read
           
            Matcher m = _section.matcher(line);  //Testing the line in the file against the _section pattern
            
            if(m.matches()) {  //Line is a section
               section = m.group(1).trim();
            } else if(section != null) {  //The file has a section (in other words: this method is useless if the .ini file does not have a header)
              
               m = _keyValue.matcher( line );
               
               if( m.matches()) {  //Line is a key/value pair
                  String k   = m.group(1).trim();  //Get the key
                  String value = m.group(2).trim();  //Get the value
                  
                  //Create the key value mapping
                  Map<String, String> kv = _entries.get(section);  //Index into the map (outer layer is <section, kv>)
                  if( kv == null ) {  //Only create a new section if it has not already been created.
                     _entries.put(section, kv = new HashMap<String, String>());   
                  }
                  kv.put(k, value);  //Put the new key value pair into the mapping specified by the section
               }
            }
         }
      } finally {}
      br.close();
   }

   //Gets a string value from the specified section and key.
   public String getString(String section, String key, String defaultvalue) {
      Map<String, String> kv = _entries.get(section);
      if(kv == null) {
         return defaultvalue;
      }
      return kv.get(key);
   }

   //Gets an int value from the specified section and key.
   public int getInt(String section, String key, int defaultvalue) {
      Map<String, String> kv = _entries.get(section);
      if(kv == null) {
         return defaultvalue;
      }
      return Integer.parseInt(kv.get( key ));
   }

   //Gets a float value from the specified section and key.
   public float getFloat(String section, String key, float defaultvalue) {
      Map<String, String> kv = _entries.get(section);
      if(kv == null) {
         return defaultvalue;
      }
      return Float.parseFloat(kv.get( key ));
   }

   //Gets a double value from the specified section and key.
   public double getDouble(String section, String key, double defaultvalue) {
      Map<String, String> kv = _entries.get(section);
      if(kv == null) {
         return defaultvalue;
      }
      return Double.parseDouble(kv.get( key ));
   }
}


public class InputManager {
  
  /*
  *Constants
  */
  
  private final int[] keyboardInputs = {'a', 'd', 'w', 's', 'j', 'k'};
  private final int[] inputPins = { 26, 19, 16, 20, 21, 13};
  
  /*
  *Fields
  */
  private int[] delayTimer = {13, 13, 18, 0, 18, 18};
  private int[] startInputTimer = {4, 4, 4, 2, 12, 12};
  public boolean usingController;
  public boolean[] inputs;
  public boolean[] triggers;
  public int[] timers;


  public InputManager() {
    
    //Setting fields
    this.inputs = new boolean[6];
    this.triggers = new boolean[6];
    this.timers = new int[6];
    for (int i = 0; i < inputs.length; i++) {
      inputs[i] = false;
      triggers[i] = false;
      timers[i] = 0;
    }
  }

  public void printTriggers() {
    for (int i = 0; i < triggers.length; i++) {
      println(triggers[i]);
    }
    println();
  }

  public void updateInputTriggers() {
    for (int i = 0; i < inputs.length; i++) {
      if (inputs[i]) {
        if (timers[i] == delayTimer[i]) {
          triggers[i] = true;
          timers[i]--;
        } else if (timers[i] > 0) {
          timers[i]--;
          triggers[i] = false;
        } else {
          timers[i] = startInputTimer[i];
          triggers[i] = true;
        }
      }
    }
  }

  public void changeDelays(boolean inMenu) {
    if(inMenu) {
      this.delayTimer[3] = 18;
      this.startInputTimer[3] = 4;
    } else {
      this.delayTimer[3] = 0;
      this.startInputTimer[3] = 2;
    }
  }

  //Called by an event
  public void setControllerInput(int pin, boolean pressed) {
    for (int i = 0; i < inputs.length; i++) {
      if (pin == inputPins[i]) {
        if(pressed) {
          inputs[i] = true;
          triggers[i] = true;
          timers[i] = delayTimer[i];
        } else {
          inputs[i] = false;
          triggers[i] = false;
          timers[i] = 0;
        }

      }
    }
  }

  //This should be called every frame
  public void checkControllerInputs() {
    for(int i = 0; i < inputs.length; i++) {
      if(GPIO.digitalRead(inputPins[i]) == GPIO.HIGH) {
        inputs[i] = false;
        triggers[i] = false;
        timers[i] = 0;
      }
    }
  }

  public void setPressInput() {
    for (int i = 0; i < inputs.length; i++) {
      //not using the controller
      if (key == keyboardInputs[i]) {
        inputs[i] = true;
        triggers[i] = true;
        timers[i] = delayTimer[i];
      }
    }
  }

  public void setReleaseInput() {
    for (int i = 0; i < inputs.length; i++) {
      if (key == keyboardInputs[i]) {
        inputs[i] = false;
        triggers[i] = false;
        timers[i] = 0;
      }
    }
  }
}
public enum InputType {
  LEFT(0),
  RIGHT(1), 
  UP(2), 
  DOWN(3), 
  A(4), 
  B(5),
  NULL(6),
  ;

  private int inputIndex;

  private InputType(int inputIndex) { this.inputIndex = inputIndex; }

  public static InputType get(int index) {
    for (InputType l : InputType.values()) {
      if (l.inputIndex == index) return l;
    }
    throw new IllegalArgumentException("Input not found");
  }
  
  public int getIndex() {
    return inputIndex;
  }
}
class LineClearEffect extends Effect {

  private int flashTimer;
  private float lineSize;
  private int growTimer;

  LineClearEffect(float x, float y, int lifeTime, boolean runInIntense, float lineSize) {
    super(x, y, lifeTime, runInIntense);
    this.lineSize = lineSize;
    this.growTimer = 255;
    this.flashTimer = 0;
  }

  public void run() {
    lifeTime--;
  }

  @Override 
    public void display(boolean intenseMode) {
    
    rectMode(CORNER);
    noStroke();
    
    if (intenseMode) {
      fill(map(lifeTime, 10, 0, 255, 0));
      rect(x, y, lineSize*10, lineSize);
    } else {
      if (flashTimer <= 0) {
        flashTimer = 10;
      } else if (flashTimer <= 5) {
        fill(255);
      } else if (flashTimer <= 10) {
        fill(0);
      }

      rect(x, y, lineSize*10, lineSize);

      flashTimer -= 2;
    }
  }
}
class LineExplodeEffect extends Effect {
  
  float vel;
  float dist;
  
  LineExplodeEffect(float x, float y, int lifeTime, boolean runInIntense, float vel) {
    super(x, y, lifeTime, runInIntense);
    this.vel = vel;
    this.dist = 0;
  }

  public void run() {
    lifeTime--;
    dist += vel;
  }

  @Override 
    public void display(boolean intenseMode) {
    if (intenseMode) {
     float deltaTheta = PI/12;
     fill(45, 160);
     noStroke();
     pushMatrix();
     translate(x, y);
     
     for(int i = 0; i < 24; i++){
       rotate(deltaTheta);
       rect(0, dist, 16, 16);
     }

     popMatrix();
    } else {
      println("Only runs in intense mode");
    }
  }
}

//This class consists of code taken out of the Game Manager due to complexity used to deal with logic specific to the menu.
public class MenuManager implements IDisplay {
  private GameManager gameManager;
  
  private MenuOptions selectedOption;
  private int selectedOptionX;

  private Character[] nameCharacters;
  private Button[][] menuButtons;

  public MenuManager(GameManager gameManager) {
    this.gameManager = gameManager;
    
    this.selectedOption = MenuOptions.NAME;
    this.selectedOptionX = 0;

    //name
    this.nameCharacters = new Character[10];
    for (int i = 0; i < 10; i++) {
      if (i < gameManager.name.length()) {
        nameCharacters[i] = gameManager.name.charAt(i);	//add all of the characters in the name
      } else {
        nameCharacters[i] = ' ';	//add spaces for the rest of the characters
      }
    }

    //Menu Buttons
    this.menuButtons = new Button[3][2];
    for (int row = 0; row < menuButtons.length; row++) {
      for (int col = 0; col < menuButtons[0].length; col++) {
        this.menuButtons[row][col] = new Button(width / 4 + col * width / 2, height * (0.5f + row * 0.2f), 25, 25, color(255, 70));
      }
    }
    updateButtons();
  }

  public void executeFunction(InputType input) {
    //There are 6 inputs. 2 inputs have different functions for 3 menu options. 1 input has a different function for all 4 menu options
    //So there is a total of 13 cases to deal with
    
    SoundManager sm = this.gameManager.soundManager;  //shortens the code
    switch(input) {

    case LEFT:
      if (this.selectedOptionX > 0) {
        this.selectedOptionX--;
      } else {
        switch(this.selectedOption) {

        case LEVEL:
          if (gameManager.startLevel > 0) {
            gameManager.startLevel--;
          }
          break;

        case MUSIC:
          sm.musicVolume = Math.max(sm.musicVolume - 0.8f, sm.minVolume);
          sm.gameMusic.setGain(sm.musicVolume);
          if (sm.musicVolume == sm.minVolume) {
            sm.gameMusic.mute();
          }
          break;

        case SOUND:
          sm.soundVolume = Math.max(sm.soundVolume - 0.8f, sm.minVolume);
          for (AudioPlayer effect : sm.soundEffects.values()) {
            if (sm.soundVolume == sm.minVolume) {
              effect.mute();
            } else {
              effect.setGain(sm.soundVolume);
            }
          }

          break;
        default:
          break;
        }
      }
      updateButtons();
      break;

    case RIGHT:
      if (this.selectedOptionX < this.selectedOption.getLength() - 1) {
        this.selectedOptionX++;
      } else {

        switch(selectedOption) {

        case LEVEL:
          if (gameManager.startLevel < 16) {
            gameManager.startLevel++;
          }
          break;

        case MUSIC:
          sm.musicVolume = Math.min(sm.musicVolume + 0.8f, sm.maxVolume);
          sm.gameMusic.setGain(sm.musicVolume);
          if (sm.musicVolume > sm.minVolume) {
            sm.gameMusic.mute();
          }
          break;

        case SOUND:
          sm.soundVolume = Math.min(sm.soundVolume + 0.8f, sm.maxVolume);
          for (AudioPlayer effect : sm.soundEffects.values()) {
            if (sm.soundVolume <= sm.minVolume + 0.8f) {
              effect.unmute();
            } else {
              effect.setGain(sm.soundVolume);
            }
          }
          break;

        default:
          break;
        }
      }
      updateButtons();
      break;

    case UP:
      if (this.selectedOption.getIndex() > 0) {
        this.selectedOption = MenuOptions.get(this.selectedOption.getIndex() - 1);
      }

      updateButtons();
      break;

    case DOWN:
      if (this.selectedOption == MenuOptions.NAME) {
        this.selectedOptionX = 0;
      }
      if (this.selectedOption.getIndex() < MenuOptions.values().length - 1) {
        this.selectedOption = MenuOptions.get(this.selectedOption.getIndex() + 1);
      } 

      updateButtons();
      break;

    case A:
      switch(this.selectedOption) {

      case NAME:
        char c = this.nameCharacters[this.selectedOptionX];
        switch(c) {
          //capital letters
        case 32:
          c = 65;
          break;
          //numbers
        case 90:
          c = 48;
          break;
        case 57:
          c = 32;
          break;
        default:
          c += 1;
          break;
        }
        this.nameCharacters[this.selectedOptionX] = c;

        gameManager.name = this.getNameFromChars();
        break;
      case LEVEL:
        if (this.selectedOptionX == 0) {
          if (gameManager.startLevel > 0) {
            gameManager.startLevel--;
          }
        } else {
          if (gameManager.startLevel < 15) {
            gameManager.startLevel++;
          }
        }
        break;

      case MUSIC:
        if (this.selectedOptionX == 0) {
          sm.musicVolume = Math.max(sm.musicVolume - 0.8f, sm.minVolume);
          if (sm.musicVolume == sm.minVolume) {
            sm.gameMusic.mute();
          }
        } else {
          if (sm.musicVolume == sm.minVolume) {
            sm.gameMusic.unmute();
          }
          sm.musicVolume = Math.min(sm.musicVolume + 0.8f, sm.maxVolume);
        }

        sm.gameMusic.setGain(sm.musicVolume);

        break;

      case SOUND:
        if (this.selectedOptionX == 0) {
          sm.soundVolume = Math.max(sm.soundVolume - 0.8f, sm.minVolume);
          if (sm.soundVolume == sm.minVolume) {
            for (AudioPlayer effect : sm.soundEffects.values()) {
              effect.mute();
            }
          }
        } else {
          sm.soundVolume = Math.min(sm.soundVolume + 0.8f, sm.maxVolume);
          if (sm.soundVolume == sm.minVolume) {
            for (AudioPlayer effect : sm.soundEffects.values()) {
              effect.unmute();
            }
          }
        }

        for (AudioPlayer effect : sm.soundEffects.values()) {
          effect.setGain(sm.soundVolume);
        }

        break;

      default:
        break;
      }
      
      break;
    case B:
      close();
      break;

    default:
      break;
    }
  }

  public void close() {
    gameManager.inputManager.changeDelays(false);
    gameManager.inMenu = false;
    if ((gameManager.startLevel != gameManager.oldStartLevel) || (gameManager.name != gameManager.oldName)) {
      gameManager.reset();
    } else {
      gameManager.soundManager.gameMusic.play();
    }
  }

  public String getNameFromChars() {

    String tempName = "";

    for (Character nameChar : this.nameCharacters) {
      tempName = tempName.concat(nameChar.toString());
    }

    tempName = tempName.trim();

    return tempName;
  }

  private void updateButtons() {
    for (int row = 0; row < this.menuButtons.length; row++) {
      for (int col = 0; col < this.menuButtons[0].length; col++) {
        if ((selectedOption.getIndex() - 1 == row) && (selectedOptionX == col)) {
          this.menuButtons[row][col].setSelected(true);
        } else {
          this.menuButtons[row][col].setSelected(false);
        }
      }
    }
  }

  @Override
  public void display(boolean intenseMode) {
    SoundManager sm = this.gameManager.soundManager;

    //background
    fill(0, 230);
    noStroke();
    rect(0, 0, width, height);

    //name
    float spacing = 3 * width / (4 * 10);
    textSize(width * 0.075f);
    for (int i = 0; i < this.nameCharacters.length; i++) {
      if ((this.selectedOption == MenuOptions.NAME) && (i == selectedOptionX)) {
        fill(0xff00eeff, 50);
        rectMode(CENTER);
        rect(width / 4 + i * spacing, height * 0.1f, width * 0.05f, height * 0.075f);
      }
      fill(255);
      text(this.nameCharacters[i], width / 4 + i * spacing, height * 0.1f);
    }

    //Level and Volume buttons
    for (int row = 0; row < this.menuButtons.length; row++) {
      for (int col = 0; col < this.menuButtons[0].length; col++) {
        this.menuButtons[row][col].display(intenseMode);
      }
    }

    //Level Header
    fill(255);
    textSize(width * 0.1f);
    text("Level ", width / 2, height * 0.35f);

    //Carots for level select
    fill(0);
    textSize(width * 0.05f);
    text("<", width / 4, height * 0.5f);
    text(">", 3 * width / 4, height * 0.5f);


    //Displaying the level number
    fill(255);
    textSize(width * 0.2f);
    text(gameManager.startLevel, width / 2, height * 0.5f);

    //Volume Header

    textSize(width * 0.1f);
    text("Music ", width / 2, height * 0.6f);


    //Slider for volume

    textSize(width * 0.05f);
    text("<", width / 4, height * 0.7f);
    text(">", 3 * width / 4, height * 0.7f);

    //line
    stroke(0xffdddddd, 150);
    strokeWeight(4);
    line(width / 3, height * 0.7f, 2 * width / 3, height * 0.7f);
    float x = map(sm.musicVolume, sm.minVolume, sm.maxVolume, width / 3, 2 * width / 3);

    //position
    noStroke();
    fill(255, 255);
    ellipse(x, height * 0.7f, 10, 10);

    //Sound Header

    textSize(width * 0.1f);
    text("Sound ", width / 2, height * 0.8f);

    //Slider for sound

    textSize(width * 0.05f);
    text("<", width / 4, height * 0.9f);
    text(">", 3 * width / 4, height * 0.9f);

    //line
    stroke(0xffdddddd, 150);
    strokeWeight(4);
    line(width / 3, height * 0.9f, 2 * width / 3, height * 0.9f);
    x = map(sm.soundVolume, sm.minVolume, sm.maxVolume, width / 3, 2 * width / 3);

    //position
    noStroke();
    fill(255, 255);
    ellipse(x, height * 0.9f, 10, 10);

    //Authors
    fill(255, 80);
    textSize(width * 0.04f);
    text("Mark Sabbagh\nLogan Bowers", width/2, height * .94f);
  }
}
public enum MenuOptions {
  NAME(0, 10),
  LEVEL(1, 2),
  MUSIC(2, 2),
  SOUND(3, 2),
  ;

  private int optionIndex;
  private int optionLength;

  private MenuOptions(int optionIndex, int length) { 
    this.optionIndex = optionIndex;
    this.optionLength = length;
  }

  public static MenuOptions get(int index) {
    for (MenuOptions l : MenuOptions.values()) {
      if (l.optionIndex == index) return l;
    }
    throw new IllegalArgumentException("Option not found");
  }
  
  public int getIndex() {
    return optionIndex;
  }

  public int getLength() {
    return optionLength;
  }
}


class Piece implements IDisplay {

  private Block[] blocks;
  private float[][] relPos;  //Relative positions of blocks
  private float x;
  private float y;
  private PieceType type;
  private float size;
  private int[] pieceColor;
  private int orientation;
  private LinkedList<PointXY> trail;

  private boolean locked;

  public Piece() {

    this.relPos = new float[blocks.length][2];
    this.x = 5;
    this.y = 0;
    this.type = PieceType.I;
    this.size = 10;
    this.pieceColor = new int[2];
    this.pieceColor[0] = 255;
    this.pieceColor[1] = 255;
    this.orientation = 0;
    this.trail = new LinkedList<PointXY>();

    this.locked = false;
    this.blocks = this.generateBlocks();
  }

  public Piece(float x, float y, PieceType type, float size, int[] pieceColor, boolean locked) {
    this.x = x;
    this.y = y;
    this.type = type;
    this.size = size;
    this.pieceColor = pieceColor;
    this.orientation = 0;
    this.trail = new LinkedList<PointXY>();

    this.locked = locked;
    this.blocks = this.generateBlocks();
  }

  private Block[] generateBlocks() {
    Block[] temp = new Block[4];
    switch(this.type) {
    case I:
      this.relPos = new float[][]{{-2, 0}, {-1, 0}, {0, 0}, {1, 0}};

      break;
    case O:
      this.relPos = new float[][]{{-0.5f, -0.5f}, {0.5f, -0.5f}, {0.5f, 0.5f}, {-0.5f, 0.5f}};

      break;
    case L:
      this.relPos = new float[][]{{-1, 0}, {0, 0}, {1, 0}, {-1, 1}};

      break;
    case J:
      this.relPos = new float[][]{{-1, 0}, {0, 0}, {1, 0}, {1, 1}};

      break;
    case S:
      this.relPos = new float[][]{{0.5f, -0.5f}, {-0.5f, -0.5f}, {-0.5f, 0.5f}, {-1.5f, 0.5f}};    

      break;
    case Z:
      this.relPos = new float[][]{{-1.5f, -0.5f}, {-0.5f, -0.5f}, {-0.5f, 0.5f}, {0.5f, 0.5f}};  

      break;
    case T:
      this.relPos = new float[][]{{-1, 0}, {0, 0}, {1, 0}, {0, 1}};

      break;
    default:
      println("invalid type of block");
    }

    for (int i = 0; i < temp.length; i++) {
      temp[i] = new Block(Math.round(this.x + this.relPos[i][0]), Math.round(this.y + this.relPos[i][1]), this.size, this.pieceColor, this.type);
    }

    return temp;
  }

  public void updateBlocks() {
    //Updating each block's position relative to the piece's position
    for (int i = 0; i < this.blocks.length; i++) {
      //Using round to convert from float to int
      this.blocks[i].setPos(Math.round(this.x + this.relPos[i][0]), Math.round(this.y + this.relPos[i][1]));
      this.blocks[i].setColor(this.pieceColor);
    }
  }


  public void rotate(boolean cw) {
    //rewrite

    for (int i = 0; i < this.relPos.length; i++) {
      //swapping the coordinates
      float temp = this.relPos[i][0];
      this.relPos[i][0] = this.relPos[i][1];
      this.relPos[i][1] = temp;

      if (cw)
        this.relPos[i][0] = -this.relPos[i][0];
      else
        this.relPos[i][1] = -this.relPos[i][1];
    }

    if (cw)
      this.orientation = (this.orientation + 1) % 4;
    else
      this.orientation = (this.orientation + 3) % 4;

    this.updateBlocks();
  }

  public void display(boolean intenseMode) {
    if (intenseMode)
      drawTrail();
    for (Block block : this.blocks) {
      block.display(intenseMode);
    }
  }

  public void drawTrail() {
    // Get the current point and add it to the front of the list
    float posX = x*size;
    float posY = y*size;

    if (trail.size() == 0 || (trail.getFirst().x != posX || trail.getFirst().y != posY)) {
      PointXY p = new PointXY(x*size, y*size);
      trail.addFirst(p);

      // If trail is too 'long' remove the oldest points
      while (trail.size () > 15)
        trail.removeLast();
    }

    // Draw trail if there are at least 2 points
    if (trail.size() >= 2) {
      PointXY currPoint;
      for (int i = 0; i < trail.size(); i++) {

        currPoint = trail.get(i);

        fill(40, 150-(10*i));
        rect(currPoint.x, currPoint.y, 15-i, 15-i);
      }
    }
  }

  public Piece clone() {
    Piece tempPiece = new Piece(x, y, type, size, pieceColor, locked);
    tempPiece.x = 5;

    return tempPiece;

  }

  //Getters/Setters

  public Block[] getBlocks() {
    return this.blocks;
  }

  public float[] getPos() {
    float[] pos = {this.x, this.y};
    return pos;
  }

  public void setPos(float x, float y) {
    this.x = x;
    this.y = y;
    this.updateBlocks();
  }

  public float[][] getRelPos() {
    return relPos;
  }

  public void setRelPos(float[][] relPos) {
    this.relPos = relPos;
    this.updateBlocks();
  }

  public PieceType getType() {
    return this.type;
  }

  public float getX() {
    return this.x;
  }

  public void setX(float x) {
    this.x = x;
    this.updateBlocks();
  }

  public float getY() {
    return this.y;
  }

  public void setY(float y) {
    this.y = y;
    this.updateBlocks();
  }

  public float getSize() {
    return this.size;
  }

  public void setSize(float size) {
    this.size = size;
  }

  public int[] getColor() {
    return this.pieceColor;
  }

  public void setColor(int[] pieceColor) {
    this.pieceColor = pieceColor;
    this.updateBlocks();
  }

  public boolean isLocked() {
    return this.locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }
}
public enum PieceType {
  I(0),
  O(1),
  L(2),
  J(3),
  S(4),
  Z(5),
  T(6);
  
   private int pieceIndex;

   private PieceType(int index) { this.pieceIndex = index; }

   public static PieceType get(int index) {
      for (PieceType l : PieceType.values()) {
          if (l.pieceIndex == index) return l;
      }
      throw new IllegalArgumentException("Piece not found");
   }
   
   public int getIndex() {
     return pieceIndex;
   }
}
public class PointXY {
  public float x, y;
  public PointXY(float px, float py) {
    x = px;
    y = py;
  }
}
public class ScoreData {
  private String name;
  private int score;

  public ScoreData() {
    this.name = "JEFF";
    this.score = 0;
  }
  public ScoreData(String name, int score) {
    this.name = name;
    this.score = score;
  }
  
  public String getName() {
    return name;
  }
  
  public int getScore() {
    return score;
  }
}
  
public class SoundManager {
  /*
  *Constants
  */

  private final int minVolume = -20; 
  private final int maxVolume = 3;

  /*
  *Fields
  */

  private Minim minim;  //This needs to come from the application
  private Map<String, AudioPlayer> soundEffects;
  private AudioPlayer gameMusic;
  private int currentMusic;
  private int[] transitionPoints = {26975, 51000, 86959}; 
  private float musicVolume;
  private float soundVolume;

  public SoundManager(Minim minim) {
    this.minim = minim;

    //adding all of the sound effects
    this.soundEffects = new HashMap<String, AudioPlayer>();

    //Jeff Mode
    for(int i = 0; i < 5; i++) {
      this.soundEffects.put("BoomTetrisForJeff" + i, this.minim.loadFile("BoomTetrisForJeff" + i + ".mp3"));
    }
    this.soundEffects.put("NeckandNeck", this.minim.loadFile("NeckandNeck.mp3"));
    this.soundEffects.put("Top2", this.minim.loadFile("Top2.mp3"));
    this.soundEffects.put("Drought", this.minim.loadFile("Drought.mp3"));
    this.soundEffects.put("LongBar", this.minim.loadFile("LongBar.mp3"));
    this.soundEffects.put("Lost", this.minim.loadFile("NoLongBar.mp3"));
    this.soundEffects.put("IntenseBoom", this.minim.loadFile("IntenseBoom.mp3"));

    //Not Jeff Mode
    this.soundEffects.put("Drop", this.minim.loadFile("ClassicDrop.mp3"));
    this.soundEffects.put("IntenseDrop", this.minim.loadFile("ModernDrop.mp3"));
    this.soundEffects.put("Clear", this.minim.loadFile("ClassicClear.mp3"));
    this.soundEffects.put("IntenseClear", this.minim.loadFile("ModernClear.mp3"));
    this.soundEffects.put("Tetris", this.minim.loadFile("ClassicTetris.mp3"));
    this.soundEffects.put("IntenseTetris", this.minim.loadFile("ModernTetris.mp3"));

    //sound effects volume
    this.soundVolume = maxVolume;
    for(AudioPlayer effect : this.soundEffects.values()) {
      effect.setGain(this.soundVolume);
    }

    //music
    this.gameMusic = this.minim.loadFile("TetrisGame.wav");
    this.currentMusic = 0;

    //music volume
    this.musicVolume = maxVolume;
    this.gameMusic.setGain(this.musicVolume);

  }

  public void startMusicFromBeginning() {
    this.rewindMusic();
    this.currentMusic = 0;
    this.gameMusic.play();
  }

  public void startMusicFromLoop() {
    this.rewindMusic();
    this.gameMusic.skip(this.transitionPoints[0]);
    this.currentMusic = 1;
    this.gameMusic.play();
  }

  public void rewindMusic() {
    this.gameMusic.pause();
    this.gameMusic.rewind();
  }
  
  public void playSoundEffect(String effectName, boolean intenseMode) {
    if(this.soundEffects.get(effectName) != null) {
      if(intenseMode) {
        rewindSoundEffect("Intense" + effectName);
        this.soundEffects.get("Intense" + effectName).play();
      } else {
        rewindSoundEffect(effectName);
        this.soundEffects.get(effectName).play();
      }
    } else {
      println("Error playing sound effect");
    }
  }

  public void rewindSoundEffect(String effectName) {
    this.soundEffects.get(effectName).rewind();
    this.soundEffects.get(effectName).rewind();
  }

  public void rewindAllSoundEffects() {
    for (AudioPlayer effect : this.soundEffects.values()) {
      if ((effect != null) && (effect.position() > effect.length() - 10)) {
        effect.rewind();
        effect.pause();
      }
    }
  }
}
class StreakEffect extends Effect {

  float vel;

  StreakEffect(float x, float y, int lifeTime, boolean runInIntense, float vel) {
    super(x, y, lifeTime, runInIntense);
    this.vel = vel;
  }

  public void run() {
    lifeTime--;
    y -= vel;
  }

  @Override public void display(boolean intenseMode) {
    if (intenseMode) {
      fill(45, 120);
      ellipse(x, y, 5, 5);
    } else {
      println("Only runs in intense mode");
    }
  }
}
static class Utility {
}
  public void settings() {  size(640, 960, P2D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "TetrisPi" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
