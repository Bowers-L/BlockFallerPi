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
