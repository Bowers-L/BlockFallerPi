public class Block implements IDisplay {

  private int[] blockColor;
  private int x;
  private int y;
  private float size;
  private float intenseSize;
  private PieceType pieceType;

  public Block(int x, int y, float size, int[] blockColor, PieceType pieceType) {
    this.x = x;
    this.y = y;
    this.blockColor = blockColor;
    this.size = size;
    this.intenseSize = size*.9;
    this.pieceType = pieceType;
  }

  public Block clone() {
    return new Block(this.x, this.y, this.size, this.blockColor, this.pieceType);
  }

@Override 
  public void display(boolean intenseMode) {
  rectMode(CORNER);
  if (intenseMode) {
   noStroke();
   fill(45);
   rect(x*size, y*size, intenseSize, intenseSize);
   if(intenseSize < size*.9)
    intenseSize++;
  } else {
   fill(blockColor[0]);
   //stroke(180);
   //strokeWeight(2);
   noStroke();
   ellipseMode(CORNER);
   rect(x*size, y*size, size-2, size-2);
   fill(0, 50);
   rect(x*size, y*size, size-6, size-6);
   ellipseMode(CENTER);
  }
 }

  public int[] getColor() {
    return this.blockColor;
  }
  
  public void setColor(int[] blockColor) {
    this.blockColor = blockColor;
  }
  
  public void setPos(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public void setX(int x) {
    this.x = x;
  }

  public void setY(int y) {
    this.y = y;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }
  
  public PieceType getPieceType() {
    return this.pieceType;
  }
  
  public void setPieceType(PieceType pieceType) {
    this.pieceType = pieceType;
  }
}
