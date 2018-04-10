
import com.github.bhlangonijr.chesslib.game.Game;

public class Utils {

  public static boolean DEBUG = false;
  public static final boolean BUREAU = false;
  public static final String base = (Utils.BUREAU) ? "C:\\Users\\A100662\\PROJECTS\\REPOS\\PRIVATE\\CHE_BP\\" : "/Users/herve/DEVEL/nbChess1/";

  public static class Tuple<X, Y> {
    public final X x;
    public final Y y;

    public Tuple(X x, Y y) {
      this.x = x;
      this.y = y;
    }
  }

  public static class Triple<X, Y, Z> {
    public final X x;
    public final Y y;
    public final Z z;

    public Triple(X x, Y y, Z z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((x == null) ? 0 : x.hashCode());
      result = prime * result + ((y == null) ? 0 : y.hashCode());
      result = prime * result + ((z == null) ? 0 : z.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Triple other = (Triple) obj;
      if (x == null) {
        if (other.x != null)
          return false;
      } else if (!x.equals(other.x))
        return false;
      if (y == null) {
        if (other.y != null)
          return false;
      } else if (!y.equals(other.y))
        return false;
      if (z == null) {
        if (other.z != null)
          return false;
      } else if (!z.equals(other.z))
        return false;
      return true;
    }
  }

  public static String nameShorten(String s) {
    if (s == null)
      return null;
    String t = s.trim();
    if (t.isEmpty())
      return null;
    //return t.split(" ")[0];
    return t.split("[\\W]+")[0];

  }




  public static void info(Object... msgs) {
    System.out.println("INFO:" + dumpArray(msgs));
  }

  public static void debug(Object... msgs) {
    if (DEBUG) {
      System.out.println("DEBUG: " + dumpArray(msgs));
    }
  }

  public static String dumpArray(Object[] a) {
    if (a == null)
      return "null";

    int iMax = a.length - 1;
    if (iMax == -1)
      return "[]";

    StringBuilder b = new StringBuilder();
    b.append('[');
    for (int i = 0;; i++) {
      b.append(">" + String.valueOf(a[i]) + "<");
      if (i == iMax)
        return b.append(']').toString();
      b.append(", ");
    }
  }



  public static boolean validatePlayers(Game g) {
    String s = g.getWhitePlayer();
    if (s == null || s.length() < 2 | s.startsWith("?")) {
      return false;
    }
    s = g.getBlackPlayer();
    if (s == null || s.length() < 2 | s.startsWith("?")) {
      return false;
    }
    return true;
  }


}