
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import java.sql.SQLException;
import java.sql.*;
import java.util.HashMap;

public class GlobalDAO {

  static final String createGameTableSql = "create CACHED table GAME ("
      + " gid INTEGER IDENTITY PRIMARY KEY"
      + ",White varchar(64)"
      + ",Black varchar(64)"
      + ",Result varchar(16)"
      + ",Moves varchar(4096)"
      + ",SanId INTEGER"
      + ",EventDate varchar(16)"
      + ",WhiteElo SMALLINT"
      + ",BlackElo SMALLINT"
      + ",ECO varchar(8)"    
      //, FOREIGN KEY (SAN) REFERENCES SAN(sid) " 
      + ",UNIQUE (Sanid,White,Black,Result)"
      + ");";

  static final String createSanTableSql = "create CACHED table SAN ("
      + "sid INTEGER IDENTITY PRIMARY KEY"
      + ",san varchar(3072) UNIQUE "
      + ");";

  static final String createMoveTableSql = "create CACHED table MOVE ("
      + "mid SMALLINT GENERATED ALWAYS AS IDENTITY "
      //+ "(START WITH " + Short.MIN_VALUE + ") "
      + "PRIMARY KEY, Move varchar(8) UNIQUE"
      + ");";

  static final String mergeMoveSql = "MERGE INTO MOVE as t USING (VALUES( ? ))" + " AS v(move) on t.Move=v.Move "
      + " WHEN NOT MATCHED THEN INSERT VALUES DEFAULT,v.Move "
      + " WHEN MATCHED THEN UPDATE SET t.Move = v.Move";

  private static boolean DROP = false;

  HashMap<String, Short> CACHED_MOVE = new HashMap<String, Short>();
  HashMap<Short, String> REVERSE_CACHED_MOVE = new HashMap<Short, String>();

  private int cacheHit = 0;
  private int cacheMissed = 0;
  private int reverseCacheHit = 0;
  private int reverseCacheMissed = 0;
  private Connection _c;

  private PreparedStatement pstmtMergeMove = null;
  private PreparedStatement pstmtInsertGame = null;
  private PreparedStatement pstmtInsertSAN = null;

  public GlobalDAO() {
  }

  public Connection getC() throws SQLException {
    if (_c == null) {
      _c = DB.connect(this.getClass().getSimpleName(), false, false);
      DB.createTable(_c, "MOVE", createMoveTableSql, DROP);
      DB.createTable(_c, "GAME", createGameTableSql, DROP);
      DB.createTable(_c, "SAN", createSanTableSql, DROP);
    }
    return _c;
  }

  public String lookupMove(short moveId) throws SQLException {
    String s = REVERSE_CACHED_MOVE.get(moveId);
    if (s == null) {
      reverseCacheMissed++;
      String sqlSelect = "SELECT Move FROM MOVE WHERE MID = " + moveId;
      ResultSet rs = getC().createStatement().executeQuery(sqlSelect);
      if (!rs.next()) {
        return null;
      }
      s = rs.getString(1);
      REVERSE_CACHED_MOVE.put(moveId, s);      
    } else reverseCacheHit++;
    return s;
  }

  public short getOrCreateMove(String move) throws SQLException {
    Connection c = getC();

    Short s = CACHED_MOVE.get(move);
    if (s == null) {
      cacheMissed++;
      if (pstmtMergeMove == null) {
        pstmtMergeMove = c.prepareStatement(mergeMoveSql, Statement.RETURN_GENERATED_KEYS);
      }
      pstmtMergeMove.setString(1, move);
      int res = pstmtMergeMove.executeUpdate();

      if (res == 0)
        System.out.println(move + " !!!!!!getOrCreateMove!!!!!!!!!ERROR res=0");
      ResultSet rs = pstmtMergeMove.getGeneratedKeys();
      rs.next();
      short mid = rs.getShort(1);
      CACHED_MOVE.put(move, mid);
      return mid;
    } else {
      cacheHit++;
      return s;
    }
  }

  public boolean _storeGame(Game g) throws SQLException, MoveConversionException {

    String san = g.getHalfMoves().toSAN(); //TODO: make sure that san is cached @home

    if (pstmtInsertSAN == null) {
      String sanUpsert = "MERGE INTO SAN as t USING (VALUES(?)) " + " AS vals(san) on t.san=vals.san "
          + " WHEN NOT MATCHED THEN INSERT VALUES DEFAULT ,vals.san";
      pstmtInsertSAN = getC().prepareStatement(sanUpsert, Statement.RETURN_GENERATED_KEYS);
    }
    pstmtInsertSAN.setString(1, san);

    int res = pstmtInsertSAN.executeUpdate();

    if (res == 0) {
      return false; ///SAN already exist TODO: check if really duplicated

    }

    ResultSet rs = pstmtInsertSAN.getGeneratedKeys();
    rs.next();
    int sanId = rs.getInt(1);

    if (pstmtInsertGame == null) {
      String sql3 = "INSERT INTO GAME VALUES(DEFAULT,?,?,?,?,?,?,?,?,?)";
      pstmtInsertGame = getC().prepareStatement(sql3);
    }

    String moveText = g.getMoveText().toString();
    if (moveText.length() > 4096) {
      moveText = moveText.substring(0, 4096);
    }

    pstmtInsertGame.setString(1, g.getWhitePlayer());
    pstmtInsertGame.setString(2, g.getBlackPlayer());
    pstmtInsertGame.setString(3, g.getResult().toString());
    pstmtInsertGame.setString(4, moveText);
    pstmtInsertGame.setInt(5, sanId);
    pstmtInsertGame.setString(6, g.getDate());
    pstmtInsertGame.setString(7, g.getWhiteElo() + "");
    pstmtInsertGame.setString(8, g.getBlackElo() + "");
    pstmtInsertGame.setString(9, g.getEco());
   
    pstmtInsertGame.executeUpdate();
    return true;

  }

  
  
  public void dumpCacheInfo() {
    Utils.info("[MOVES Cache]", "cache size :" + CACHED_MOVE.size(), "Hits : " + cacheHit, "Missed : " + cacheMissed);
    Utils.info("[MOVES ReverseCache]", "cache size :" + REVERSE_CACHED_MOVE.size(), "Hits : " + reverseCacheHit, "Missed : " + reverseCacheMissed);
  }
}