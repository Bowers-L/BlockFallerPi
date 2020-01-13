
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
        this.menuButtons[row][col] = new Button(width / 4 + col * width / 2, height * (0.5 + row * 0.2), 25, 25, color(255, 70));
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
          sm.musicVolume = Math.max(sm.musicVolume - 0.8, sm.minVolume);
          sm.gameMusic.setGain(sm.musicVolume);
          if (sm.musicVolume == sm.minVolume) {
            sm.gameMusic.mute();
          }
          break;

        case SOUND:
          sm.soundVolume = Math.max(sm.soundVolume - 0.8, sm.minVolume);
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
          sm.musicVolume = Math.min(sm.musicVolume + 0.8, sm.maxVolume);
          sm.gameMusic.setGain(sm.musicVolume);
          if (sm.musicVolume > sm.minVolume) {
            sm.gameMusic.mute();
          }
          break;

        case SOUND:
          sm.soundVolume = Math.min(sm.soundVolume + 0.8, sm.maxVolume);
          for (AudioPlayer effect : sm.soundEffects.values()) {
            if (sm.soundVolume <= sm.minVolume + 0.8) {
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
          sm.musicVolume = Math.max(sm.musicVolume - 0.8, sm.minVolume);
          if (sm.musicVolume == sm.minVolume) {
            sm.gameMusic.mute();
          }
        } else {
          if (sm.musicVolume == sm.minVolume) {
            sm.gameMusic.unmute();
          }
          sm.musicVolume = Math.min(sm.musicVolume + 0.8, sm.maxVolume);
        }

        sm.gameMusic.setGain(sm.musicVolume);

        break;

      case SOUND:
        if (this.selectedOptionX == 0) {
          sm.soundVolume = Math.max(sm.soundVolume - 0.8, sm.minVolume);
          if (sm.soundVolume == sm.minVolume) {
            for (AudioPlayer effect : sm.soundEffects.values()) {
              effect.mute();
            }
          }
        } else {
          sm.soundVolume = Math.min(sm.soundVolume + 0.8, sm.maxVolume);
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
    textSize(width * 0.075);
    for (int i = 0; i < this.nameCharacters.length; i++) {
      if ((this.selectedOption == MenuOptions.NAME) && (i == selectedOptionX)) {
        fill(#00eeff, 50);
        rectMode(CENTER);
        rect(width / 4 + i * spacing, height * 0.1, width * 0.05, height * 0.075);
      }
      fill(255);
      text(this.nameCharacters[i], width / 4 + i * spacing, height * 0.1);
    }

    //Level and Volume buttons
    for (int row = 0; row < this.menuButtons.length; row++) {
      for (int col = 0; col < this.menuButtons[0].length; col++) {
        this.menuButtons[row][col].display(intenseMode);
      }
    }

    //Level Header
    fill(255);
    textSize(width * 0.1);
    text("Level ", width / 2, height * 0.35);

    //Carots for level select
    fill(0);
    textSize(width * 0.05);
    text("<", width / 4, height * 0.5);
    text(">", 3 * width / 4, height * 0.5);


    //Displaying the level number
    fill(255);
    textSize(width * 0.2);
    text(gameManager.startLevel, width / 2, height * 0.5);

    //Volume Header

    textSize(width * 0.1);
    text("Music ", width / 2, height * 0.6);


    //Slider for volume

    textSize(width * 0.05);
    text("<", width / 4, height * 0.7);
    text(">", 3 * width / 4, height * 0.7);

    //line
    stroke(#dddddd, 150);
    strokeWeight(4);
    line(width / 3, height * 0.7, 2 * width / 3, height * 0.7);
    float x = map(sm.musicVolume, sm.minVolume, sm.maxVolume, width / 3, 2 * width / 3);

    //position
    noStroke();
    fill(255, 255);
    ellipse(x, height * 0.7, 10, 10);

    //Sound Header

    textSize(width * 0.1);
    text("Sound ", width / 2, height * 0.8);

    //Slider for sound

    textSize(width * 0.05);
    text("<", width / 4, height * 0.9);
    text(">", 3 * width / 4, height * 0.9);

    //line
    stroke(#dddddd, 150);
    strokeWeight(4);
    line(width / 3, height * 0.9, 2 * width / 3, height * 0.9);
    x = map(sm.soundVolume, sm.minVolume, sm.maxVolume, width / 3, 2 * width / 3);

    //position
    noStroke();
    fill(255, 255);
    ellipse(x, height * 0.9, 10, 10);

    //Authors
    fill(255, 80);
    textSize(width * 0.04);
    text("Mark Sabbagh\nLogan Bowers", width/2, height * .94);
  }
}
