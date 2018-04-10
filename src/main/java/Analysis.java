
import com.github.bhlangonijr.chesslib.Board;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Analysis {

  private static boolean DEBUG = true;

  public static void main(String[] args) throws Exception {
    System.out.println("START");
    Utils.DEBUG = DEBUG;

    Analysis A = new Analysis();
    A.test();

    System.out.println("END");
  }

  // B12 2.d4

  //exchangeVar SELECT count(*) FROM POS where Fen = 'rnbqkbnr/pp2pppp/8/3p4/3P4/8/PPP2PPP/RNBQKBNR w KQkq -';   B13

  //fantasy rnbqkbnr/pp2pppp/2p5/3p4/3PP3/5P2/PPP3PP/RNBQKBNR b KQkq -

  //Two knights  rnbqkbnr/pp2pppp/2p5/3p4/4P3/2N2N2/PPPP1PPP/R1BQKB1R b KQkq -  B11

  void test() throws SQLException {
    ///-2147483610 rnbqkbnr/pp2pppp/2p5/8/3Pp3/2N5/PPP2PPP/R1BQKBNR w KQkq - 6 15  4
    Board b = new Board();
    String dbFen = "rnbqkbnr/pp2pppp/2p5/8/3Pp3/2N5/PPP2PPP/R1BQKBNR w KQkq -";
    b.loadFromFEN(dbFen);
    //System.out.println(b.toStringBlackDown());
    Connection c = OpeningHandler.CK.getDAO().getConnection();
    DB.tablesInfo(c, "CK");
    POS startPos = findByFen(dbFen);
    Utils.info("startPos : ", startPos);

    List<POS> mostPlayedMoves = selectMostPlayedMoves(c, startPos);
    Utils.info(mostPlayedMoves);
    b.loadFromFEN(mostPlayedMoves.get(0).fen);
    //System.out.println(b.toStringBlackDown());

  }

  void advancedCK() throws SQLException {

    Connection c = OpeningHandler.CK.getDAO().getConnection();
    DB.tablesInfo(c, "CK");

    String pgn = "1. e4 c6 2. d4 d5 3. e5 c5 4. dxc5 *";
    String fullFen = "rnbqkbnr/pp2pppp/8/2ppP3/3P4/8/PPP2PPP/RNBQKBNR w KQkq - 0 4";
    String dbFen = "rnbqkbnr/pp2pppp/8/2ppP3/3P4/8/PPP2PPP/RNBQKBNR w KQkq -";

    Board b = new Board();
    b.loadFromFEN(fullFen);
    DB.tablesInfo(c, "CK");

    //System.out.println(b.toStringBlackDown());

    POS startPos = findByFen(dbFen);
    Utils.info(startPos);

    //selectMostPlayedMoves(c,dbFen,1);

    //select most played moves for gegner    
    // for each select acceptable move for me    
    //look if if there is a common acceptable move.


  }


  private POS findByFen(String fen) throws SQLException {
    Connection c = OpeningHandler.CK.getDAO().getConnection();
    String sql = "SELECT Pid,W,Draw,B FROM POS where Fen='" + fen + "';";
    ResultSet rs = c.createStatement().executeQuery(sql);
    if (rs.next()) {
      int id = rs.getInt(1);
      int w = rs.getInt(2);
      int d = rs.getInt(3);
      int b = rs.getInt(4);
      POS pos = new POS(id, fen, w, d, b, "DB", null);
      return pos;
    }
    return null;
  }

  private List<POS> selectMostPlayedMoves(Connection c, POS pos) throws SQLException {
    List<POS> rep = new ArrayList<POS>();

    int playedGames = pos.W+pos.Draw+pos.B;
    String sql = "select top 10  POS.*,Move from POS,POS_MOVES where "
        + " POS_MOVES.PFen = " + pos.fenId 
        + "AND POS.Pid=POS_MOVES.CFen "      
        + " AND W+B+DRAW>=" + playedGames + "/10" + " order by W+B+DRAW desc";

    Utils.info("selectMostPlayedMoves sql", sql);

    ResultSet rs = c.createStatement().executeQuery(sql);

    for (; rs.next();) {
      int fenId = rs.getInt(1);
      String fen = rs.getString(2);
      int w = rs.getInt(3);
      int draw = rs.getInt(4);
      int b = rs.getInt(5);
      String move = rs.getString(6);

      POS tmp = new POS(fenId,fen, w, draw, b, "DB", move);
      rep.add(tmp);
    }
    return rep;
  }

  class POS {

    int fenId;
    String fen;
    int W;
    int B;
    int Draw;
    String src;
    String move;

    public POS(int fenId, String fen, int w, int draw, int b, String src, String move) {
      super();
      this.fenId =fenId;
      this.fen = fen;
      W = w;
      B = b;
      Draw = draw;
      this.src = src;
      this.move = move;
    }

    @Override
    public String toString() {
      if (DEBUG)
        return "POS ["+fenId+ ", move=" + move + ", W=" + W + ", B=" + B + ", Draw=" + Draw + ", fen=" + fen + ", src=" + src + "]";

      return move;
    }

  }

}