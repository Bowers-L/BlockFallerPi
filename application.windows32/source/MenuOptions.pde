public enum MenuOptions {
  NAME(0, 10),
  LEVEL(1, 2),
  MUSIC(2, 2),
  SOUND(3, 2),
  ;

  private int optionIndex;
  private int optionLength;

  private MenuOptions(int optionIndex, int length) { 
    this.optionIndex = optionIndex;
    this.optionLength = length;
  }

  public static MenuOptions get(int index) {
    for (MenuOptions l : MenuOptions.values()) {
      if (l.optionIndex == index) return l;
    }
    throw new IllegalArgumentException("Option not found");
  }
  
  public int getIndex() {
    return optionIndex;
  }

  public int getLength() {
    return optionLength;
  }
}
