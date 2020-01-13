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
