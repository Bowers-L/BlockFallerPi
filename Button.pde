public class Button implements IDisplay {

  //fields
  private String id;
  private float x;
  private float y;
  private float w;
  private float h;
  private int col;

  private boolean selected;
  private int selectedColor;

  Button() {
    //default values
    this.id = "button";
    this.x = 0;
    this.y = 0;
    this.w = 15;
    this.h = 10;
    this.col = 255;
    this.selected = false;
    this.selectedColor = #89ffff;
  }

  Button(float x, float y, float w, float h, int col) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.col = col;
    this.selected = false;
    this.selectedColor = #dddddd;
  }

  Button(float x, float y, float w, float h, int col, boolean selected, int selectedColor) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.col = col;
    this.selected = selected;
    this.selectedColor = selectedColor;
  }

  public void display(boolean intenseMode) {

    noStroke();
    if (selected) {
      fill(this.selectedColor);
    } else {
      fill(this.col);
    }

    rectMode(CENTER);
    rect(this.x, this.y, this.w, this.h);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean getSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }
}