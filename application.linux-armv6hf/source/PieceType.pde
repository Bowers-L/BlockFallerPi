public enum PieceType {
  I(0),
  O(1),
  L(2),
  J(3),
  S(4),
  Z(5),
  T(6);
  
   private int pieceIndex;

   private PieceType(int index) { this.pieceIndex = index; }

   public static PieceType get(int index) {
      for (PieceType l : PieceType.values()) {
          if (l.pieceIndex == index) return l;
      }
      throw new IllegalArgumentException("Piece not found");
   }
   
   public int getIndex() {
     return pieceIndex;
   }
}
