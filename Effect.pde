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
