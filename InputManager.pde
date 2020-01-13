import processing.io.*;

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
