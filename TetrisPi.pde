import java.util.*;
import ddf.minim.*;

GameManager gameManager;

void setup() {
  size(640, 960, P2D);
  gameManager = new GameManager(this, false);

  gameManager.startGame(0);
}

void draw() { 
  gameManager.run();
}  

void keyPressed(){
  gameManager.inputManager.setPressInput();
}

void keyReleased(){
  gameManager.inputManager.setReleaseInput();
}

void buttonEvent(int pin) {
  if(GPIO.digitalRead(pin) == GPIO.LOW) {
    gameManager.inputManager.setControllerInput(pin, true);
  } else {
    gameManager.inputManager.setControllerInput(pin, false);
  }
}
