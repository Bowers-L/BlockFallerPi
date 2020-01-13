  
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
