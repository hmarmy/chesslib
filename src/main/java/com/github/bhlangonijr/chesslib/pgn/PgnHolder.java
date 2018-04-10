package com.github.bhlangonijr.chesslib.pgn;

import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.game.GameResult;
import com.github.bhlangonijr.chesslib.util.LargeFile;
import com.github.bhlangonijr.chesslib.util.StringUtil;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;


public class PgnHolder {

    private final static Pattern propertyPattern = Pattern.compile("\\[.* \".*\"\\]");
    private final static String UTF8_BOM = "\uFEFF";
    
    private final List<PGNLoadListener> listener = new ArrayList<PGNLoadListener>();
    private String fileName;
    private Integer size;
    private boolean lazyLoad;

    public PgnHolder(String filename) throws FileNotFoundException {
        setFileName(filename);
        setLazyLoad(true);
    }

    private static boolean isProperty(String line) {
        return propertyPattern.matcher(line).matches();
    }

    private static PGNProperty parsePGNProperty(String line) {
        try {

            String l = line.replace("[", "");
            l = l.replace("]", "");
            l = l.replace("\"", "");

            return new PGNProperty(StringUtil.beforeSequence(l, " "),
                    StringUtil.afterSequence(l, " "));
        } catch (Exception e) {
            // do nothing
        }

        return null;
    }

    public void cleanUp() {                
        listener.clear();
        size = 0;
    }


    public String getFileName() {
        return fileName;
    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }




    public void loadPgn() throws Exception {
        LargeFile file = new LargeFile(getFileName());
        size = 0;
        Game game = null;

        StringBuilder moveText = null;
        String date = null;
        boolean moveTextParsing = false;
        try {
            for (String line : file) {
            	//System.out.println(line);
                try {
                    line = line.trim();
                    if (line.startsWith(UTF8_BOM)) {
                        line = line.substring(1);
                    }
                    if (isProperty(line)) {
                    	 
                        PGNProperty p = parsePGNProperty(line);
                       
                        if (p != null) {
                            String tag = p.name.toLowerCase().trim();
                            //begin
                            if (tag.equals("event")) {
                                if (moveTextParsing && moveText != null && game != null &&
                                        game.getHalfMoves().size() == 0) {
                                    setMoveText(game, moveText);
                                }
                                size++;
                                for (PGNLoadListener l : getListener()) {
                                  if (game!=null){
                                    l.notifyProgress(game);
                                    if (game.getBoard()!=null) game.getBoard().clear();                                  
                                  }
                                }   
                                
                                game = null;                                                                
                                moveText = new StringBuilder();
                         
                            } else if (tag.equals("date")) {                               
                                    date = p.value;
                                    if (game != null) {
                                      game.setDate(date);    
                                    }
                                                            
                            } else if (tag.equals("white")) {
                                if (game == null) {
                                    game = new Game((size - 1) + "");
                                    game.setDate(date);                                                                       
                                }                               
                                game.setWhitePlayer(p.value);                               

                            } else if (tag.equals("black")) {
                                
                                if (game == null) {
                                    game = new Game((size - 1) + "");
                                    game.setDate(date);                                                                        
                                }

                                game.setBlackPlayer(p.value);
                            } else if (tag.equals("result")) {
                                if (game != null) {
                                    GameResult r = GameResult.fromNotation(p.value);
                                    game.setResult(r);
                                }
                            } else if (tag.equals("plycount")) {
                                if (game != null) {
                                    game.setPlyCount(p.value);
                                }                           
                            } else if (tag.equals("fen")) {
                                if (game != null) {
                                    game.setFen(p.value);
                                }
                            } else if (tag.equals("eco")) {
                                if (game != null) {
                                    game.setEco(p.value);
                                }
                            } else if (tag.equals("opening")) {
                                if (game != null) {
                                    game.setOpening(p.value);
                                }                           
                            } else if (tag.equals("whiteelo")) {
                              try {
                               game.setWhiteElo(Integer.parseInt(p.value));
                              } catch (NumberFormatException e) {}
                                
                            } else if (tag.equals("blackelo")) {                                
                              try {
                                game.setBlackElo(Integer.parseInt(p.value));
                               } catch (NumberFormatException e) {}
                            } else {
                                if (game != null) {
                                    if (game.getProperty() == null) {
                                        game.setProperty(new HashMap<String, String>());
                                    }
                                    game.getProperty().put(p.name, p.value);
                                }
                            }
                        }
                    } else if (!line.trim().equals("") && moveText != null) {
                        moveText.append(line);
                        moveText.append('\n');
                        moveTextParsing = true;
                        if (line.endsWith("1-0") ||
                                line.endsWith("0-1") ||
                                line.endsWith("1/2-1/2") ||
                                line.endsWith("*")) {
                            //end of PGN
                            if (game != null) {
                                setMoveText(game, moveText);
                            }
                            moveText = null;
                            moveTextParsing = false;
                        }
                    }

                } catch (Exception e) {
                	e.printStackTrace();
                	throw new PgnException("Error parsing PGN", e);  
                }

            }
            for (PGNLoadListener l : getListener()) {
              if (game!=null){
                l.notifyProgress(game);
                if (game.getBoard()!=null) game.getBoard().clear();
              }
            }
        } finally {
            file.close();
        }
    }

    private void setMoveText(Game game, StringBuilder moveText) throws Exception {

        //clear game result
        StringUtil.replaceAll(moveText, "1-0", "");
        StringUtil.replaceAll(moveText, "0-1", "");
        StringUtil.replaceAll(moveText, "1/2-1/2", "");
        StringUtil.replaceAll(moveText, "*", "");

        game.setMoveText(moveText);
        if (!isLazyLoad()) {
          game.loadMoveText();
        } 
        game.setPlyCount(game.getHalfMoves().size() + "");

    }


    public Integer getSize() {
        return size;
    }


    public boolean isLazyLoad() {
        return lazyLoad;
    }


    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }


    public List<PGNLoadListener> getListener() {
        return listener;
    }


    static class PGNProperty {
        public String name;

        ;
        public String value;

        public PGNProperty() {
        }
        public PGNProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }


}