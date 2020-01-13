import java.util.Random;

class Piece implements IDisplay {

  private Block[] blocks;
  private float[][] relPos;  //Relative positions of blocks
  private float x;
  private float y;
  private PieceType type;
  private float size;
  private int[] pieceColor;
  private int orientation;
  private LinkedList<PointXY> trail;

  private boolean locked;

  public Piece() {

    this.relPos = new float[blocks.length][2];
    this.x = 5;
    this.y = 0;
    this.type = PieceType.I;
    this.size = 10;
    this.pieceColor = new int[2];
    this.pieceColor[0] = 255;
    this.pieceColor[1] = 255;
    this.orientation = 0;
    this.trail = new LinkedList<PointXY>();

    this.locked = false;
    this.blocks = this.generateBlocks();
  }

  public Piece(float x, float y, PieceType type, float size, int[] pieceColor, boolean locked) {
    this.x = x;
    this.y = y;
    this.type = type;
    this.size = size;
    this.pieceColor = pieceColor;
    this.orientation = 0;
    this.trail = new LinkedList<PointXY>();

    this.locked = locked;
    this.blocks = this.generateBlocks();
  }

  private Block[] generateBlocks() {
    Block[] temp = new Block[4];
    switch(this.type) {
    case I:
      this.relPos = new float[][]{{-2, 0}, {-1, 0}, {0, 0}, {1, 0}};

      break;
    case O:
      this.relPos = new float[][]{{-0.5, -0.5}, {0.5, -0.5}, {0.5, 0.5}, {-0.5, 0.5}};

      break;
    case L:
      this.relPos = new float[][]{{-1, 0}, {0, 0}, {1, 0}, {-1, 1}};

      break;
    case J:
      this.relPos = new float[][]{{-1, 0}, {0, 0}, {1, 0}, {1, 1}};

      break;
    case S:
      this.relPos = new float[][]{{0.5, -0.5}, {-0.5, -0.5}, {-0.5, 0.5}, {-1.5, 0.5}};    

      break;
    case Z:
      this.relPos = new float[][]{{-1.5, -0.5}, {-0.5, -0.5}, {-0.5, 0.5}, {0.5, 0.5}};  

      break;
    case T:
      this.relPos = new float[][]{{-1, 0}, {0, 0}, {1, 0}, {0, 1}};

      break;
    default:
      println("invalid type of block");
    }

    for (int i = 0; i < temp.length; i++) {
      temp[i] = new Block(Math.round(this.x + this.relPos[i][0]), Math.round(this.y + this.relPos[i][1]), this.size, this.pieceColor, this.type);
    }

    return temp;
  }

  public void updateBlocks() {
    //Updating each block's position relative to the piece's position
    for (int i = 0; i < this.blocks.length; i++) {
      //Using round to convert from float to int
      this.blocks[i].setPos(Math.round(this.x + this.relPos[i][0]), Math.round(this.y + this.relPos[i][1]));
      this.blocks[i].setColor(this.pieceColor);
    }
  }


  public void rotate(boolean cw) {
    //rewrite

    for (int i = 0; i < this.relPos.length; i++) {
      //swapping the coordinates
      float temp = this.relPos[i][0];
      this.relPos[i][0] = this.relPos[i][1];
      this.relPos[i][1] = temp;

      if (cw)
        this.relPos[i][0] = -this.relPos[i][0];
      else
        this.relPos[i][1] = -this.relPos[i][1];
    }

    if (cw)
      this.orientation = (this.orientation + 1) % 4;
    else
      this.orientation = (this.orientation + 3) % 4;

    this.updateBlocks();
  }

  public void display(boolean intenseMode) {
    if (intenseMode)
      drawTrail();
    for (Block block : this.blocks) {
      block.display(intenseMode);
    }
  }

  void drawTrail() {
    // Get the current point and add it to the front of the list
    float posX = x*size;
    float posY = y*size;

    if (trail.size() == 0 || (trail.getFirst().x != posX || trail.getFirst().y != posY)) {
      PointXY p = new PointXY(x*size, y*size);
      trail.addFirst(p);

      // If trail is too 'long' remove the oldest points
      while (trail.size () > 15)
        trail.removeLast();
    }

    // Draw trail if there are at least 2 points
    if (trail.size() >= 2) {
      PointXY currPoint;
      for (int i = 0; i < trail.size(); i++) {

        currPoint = trail.get(i);

        fill(40, 150-(10*i));
        rect(currPoint.x, currPoint.y, 15-i, 15-i);
      }
    }
  }

  public Piece clone() {
    Piece tempPiece = new Piece(x, y, type, size, pieceColor, locked);
    tempPiece.x = 5;

    return tempPiece;

  }

  //Getters/Setters

  public Block[] getBlocks() {
    return this.blocks;
  }

  public float[] getPos() {
    float[] pos = {this.x, this.y};
    return pos;
  }

  public void setPos(float x, float y) {
    this.x = x;
    this.y = y;
    this.updateBlocks();
  }

  public float[][] getRelPos() {
    return relPos;
  }

  public void setRelPos(float[][] relPos) {
    this.relPos = relPos;
    this.updateBlocks();
  }

  public PieceType getType() {
    return this.type;
  }

  public float getX() {
    return this.x;
  }

  public void setX(float x) {
    this.x = x;
    this.updateBlocks();
  }

  public float getY() {
    return this.y;
  }

  public void setY(float y) {
    this.y = y;
    this.updateBlocks();
  }

  public float getSize() {
    return this.size;
  }

  public void setSize(float size) {
    this.size = size;
  }

  public int[] getColor() {
    return this.pieceColor;
  }

  public void setColor(int[] pieceColor) {
    this.pieceColor = pieceColor;
    this.updateBlocks();
  }

  public boolean isLocked() {
    return this.locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }
}
