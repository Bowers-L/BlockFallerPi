public enum Direction {
  DOWN(0),
  LEFT(1),
  RIGHT(2),
  UP(3)
  ;
  
   private int directionIndex;

   private Direction(int index) { this.directionIndex = index; }

   public static Direction get(int index) {
      for (Direction l : Direction.values()) {
          if (l.directionIndex == index) return l;
      }
      throw new IllegalArgumentException("Direction not found");
   }
   
   public int getIndex() {
     return directionIndex;
   }
}
