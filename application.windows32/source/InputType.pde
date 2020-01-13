public enum InputType {
  LEFT(0),
  RIGHT(1), 
  UP(2), 
  DOWN(3), 
  A(4), 
  B(5),
  NULL(6),
  ;

  private int inputIndex;

  private InputType(int inputIndex) { this.inputIndex = inputIndex; }

  public static InputType get(int index) {
    for (InputType l : InputType.values()) {
      if (l.inputIndex == index) return l;
    }
    throw new IllegalArgumentException("Input not found");
  }
  
  public int getIndex() {
    return inputIndex;
  }
}
