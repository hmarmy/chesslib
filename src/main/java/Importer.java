
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.game.GameResult;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.github.bhlangonijr.chesslib.pgn.PGNLoadListener;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Importer {
    
  GlobalDAO globalDAO = new GlobalDAO();
  
  enum STATS {
    INVALID_PLAYER, INVALID_PGN, INVALID_ECO, NO_HANDLER, DUPLICATE, IMPORTED, ONGOING;
    int counter = 0;

    public String toString() {
      return this.name() + " : " + counter;
    }
  }

  public static void main(String[] args) throws Exception {
    System.out.println("START");
    //System.in.read();
    String[] bfilenames = { "finalmast11.pgn", "polgar.pgn", "world_matches.pgn", "timman.pgn", "paolo.pgn","dortmund.pgn" };
    String[] mfilenames = { "KingBase2018-B00-B19.pgn" };
    
    if (args.length == 0) {
      System.out.println("No file(s) passed as parameter -> USING DEFAULTS");
      args = Utils.BUREAU ? bfilenames : mfilenames;
    }

    Importer m = new Importer();
    m.importPgnFiles(args);

    DB.tablesInfo(m.globalDAO.getC(),"GLOBAL");
    //DB.shutdownDb(m.globalDAO.getC());
    System.out.println("END");
  }

  
  void importGame(Game game) throws MoveConversionException, SQLException {
    if (GameResult.ONGOING.equals(game.getResult())) {
      Utils.info("SKIP ONGOING GAME", game.getWhitePlayer(), game.getBlackPlayer());
      STATS.DUPLICATE.counter++;
      return;
    }

    if (!Utils.validatePlayers(game)) {
      Utils.info("SKIP INVALID PLAYERS", game.getWhitePlayer(), game.getBlackPlayer());
      STATS.INVALID_PLAYER.counter++;
      return;
    }
    
    String eco = game.getEco();
    if (eco == null || eco.trim().length() < 3) {
      STATS.INVALID_ECO.counter++;
      return;
    }

    OpeningHandler h = OpeningHandler.findHandler(game);
    if (h == null) {
      STATS.NO_HANDLER.counter++;
      return;
    }

    try {
      game.loadMoveText();
    } catch (Exception e) {
      Utils.info("ERROR parsing move text -> IGNORE game", e.getMessage(),
          game.toString().substring(0, Math.min(100, game.toString().length())).replace("\n", "--"));
      if (Utils.DEBUG) {
        Utils.debug("************************************************************************************");
        e.printStackTrace();
        Utils.debug(game);
        Utils.debug("************************************************************************************");
      }
      STATS.INVALID_PGN.counter++;
      return;
    }

    boolean duplicated = !globalDAO._storeGame(game);

    if (duplicated) {
      STATS.DUPLICATE.counter++;
      return;
    }

    Board board = new Board();

    String oldFen = board.getFEN(false);

    List<Utils.Triple<String, Short, String>> movesToStore = new ArrayList<Utils.Triple<String, Short, String>>();

    movesToStore.add(new Utils.Triple<String, Short, String>(null, null, oldFen));

    MoveList moves = game.getHalfMoves();

    for (Move move : moves) {

      short moveId = globalDAO.getOrCreateMove(move.getSan());
      board.doMove(move);
      String newFen = board.getFEN(false);
      movesToStore.add(new Utils.Triple<String, Short, String>(oldFen, moveId, newFen));
      oldFen = newFen;
      if (movesToStore.size() > 16 * 2)
        break;
    }
    h.getDAO().storeMoves(movesToStore,game.getResult());
   
   
    counter++;
    
    if (counter>3000) {
      Utils.info("COMMIT ALL");
         
    globalDAO.getC().commit();
    OpeningHandler.commitAll();
    counter=0;
    }
  }
int counter=0;
  public void importPgnFiles(String... filenames) throws Exception {

    Utils.info("[START importPgnFiles]", filenames.length, Arrays.toString(filenames));
    long HstartTime = System.currentTimeMillis();
    int Tpgn = 0;
    for (String filename : filenames) {
      String f = filename;
      final long startTime = System.currentTimeMillis();
      
      if(!new File(f).isFile()) {
        f = Utils.base+File.separator+"pgn"+File.separator+filename;
      }
      if(!new File(f).isFile()) {
       Utils.info(filename + "not found, SKIP");
       continue;
      }
      Utils.info("Start importing " + f);

      PgnHolder pgn = new PgnHolder(f);
      pgn.setLazyLoad(true);
      
      pgn.getListener().add(new PGNLoadListener() {
      int i = 0;
        @Override
        public void notifyProgress(Game game) throws MoveConversionException, SQLException {
          importGame(game);
          i++;
          if (i%1000==0){
          long temp = (System.currentTimeMillis() - startTime) / 1000;
          Utils.info(i+" games imported in " + temp + " -- " + (((float)i)/temp));
          }
        }
      });
            
      pgn.loadPgn();
      Utils.info("COMMIT ALL AFTER PGN");
      
      globalDAO.getC().commit();
      OpeningHandler.commitAll();

      long durationSec = (System.currentTimeMillis() - startTime) / 1000;

      Utils.info("File: " + f + " imported", "duration sec:" + durationSec, "contain " + pgn.getSize());
      Tpgn+=pgn.getSize();
    }    
    long HdurationSec = (System.currentTimeMillis() - HstartTime) / 1000;
    Utils.info("[DONE importPgnFiles]", "duration sec:" + HdurationSec, Tpgn +" imported", Arrays.toString(STATS.values()));
  }

}
  
