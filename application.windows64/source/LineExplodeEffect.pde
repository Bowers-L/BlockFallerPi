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
