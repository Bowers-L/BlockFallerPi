class ExplodeEffect extends Effect {
  private float size;

  ExplodeEffect(float x, float y, int lifeTime, boolean runInIntense, float size) {
    super(x, y, lifeTime, runInIntense);
    this.size = size*2;
  }

  public void run() {
    lifeTime--;
    size *= 1.2;
  }

  @Override 
    public void display(boolean intenseMode) {
    if (intenseMode) {
      strokeWeight(4);
      stroke(255, 255 - size*1.4);
      noFill();
      rectMode(CENTER);
      rect(x, y, size, size);
    } else {
      println("Only runs in intense mode");
    }
  }
}
