import ddf.minim.*;
import ddf.minim.analysis.*;

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

    textSize(width*.09);
    strokeWeight(2);

    line(gridManager.getGridManagerEndPos(), 0, gridManager.getGridManagerEndPos(), height);
    translate(gridManager.getGridManagerEndPos() + (width-gridManager.getGridManagerEndPos())/2, 0);

    // Draw UI text
    text("TETRIS", 0, height*.05);

    textSize(width*.08);
    text("score\n" + score, 0, height*.35);

    text("level\n" + level, 0, height*.55);

    text("lines\n" + gridManager.getTotalLinesCleared(), 0, height * 0.75);

    textSize(width*.13);
    text(countDown/1000, 0, height*.9);
    popMatrix();

    if (inMenu) {
        menuManager.display(this.intenseMode);
    } else if (gameManager.inactiveTimer == 0) {
      //Display High Scores with AI playing

      noStroke();
      fill(0, 160);
      rectMode(CENTER);
      rect(width/2, height/2, width*1.2, height*1.2);

      if (intenseMode) {
        fill(255);
      } else {
        fill(gridManager.currentPiece.pieceColor[0]);
      }

      float ySpace = height*.04;
      textSize(ySpace);

      for (int i = 0; i < Math.min(this.displayedScores.getRowCount(), 10); i++) {
        TableRow row = this.displayedScores.getRow(i);
        text(row.getString("name") + " " + row.getInt("score"), 100, height*.1 + (ySpace*i));
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
