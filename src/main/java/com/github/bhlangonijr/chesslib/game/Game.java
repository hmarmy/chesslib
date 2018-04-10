/*
 * Copyright 2016 Ben-Hur Carlos Vieira Langoni Junior
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.bhlangonijr.chesslib.game;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.github.bhlangonijr.chesslib.pgn.PgnException;
import com.github.bhlangonijr.chesslib.util.StringUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;


public class Game {

  private String gameId;
  private String date;
  private String time;
  private String whitePlayer;
  private String blackPlayer;
  private Integer whiteElo = 0;
  private Integer blackElo = 0;

  private String plyCount;
  private GameResult result;
  private MoveList halfMoves = new MoveList();
  private Map<Integer, MoveList> variations;
  private Map<Integer, String> commentary;
  private Map<Integer, String> nag;
  private Map<String, String> property;

  private String fen;
  private Board board;
  private int position;
  private int initialPosition; // when loaded from FEN
  private MoveList currentMoveList;
  private String eco;
  private StringBuilder moveText;
  private String opening;

  public Game(String gameId) {
    this.gameId = gameId;
    this.result = GameResult.ONGOING;
    this.initialPosition = 0;
    this.setPosition(0);
  }

  private static String makeProp(String name, String value) {
    return "[" + name + " \"" + value + "\"]\n";
  }

  private static String getMovesAt(String moves, int index) {
    StringBuilder b = new StringBuilder();
    int count = 0;
    for (String m : moves.split(" ")) {
      count++;
      if (count >= index) {
        break;
      }
      b.append(m);
      b.append(' ');
    }
    return b.toString();
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getWhitePlayer() {
    return whitePlayer;
  }

  public void setWhitePlayer(String whitePlayer) {
    this.whitePlayer = whitePlayer;
  }

  public String getBlackPlayer() {
    return blackPlayer;
  }

  public void setBlackPlayer(String blackPlayer) {
    this.blackPlayer = blackPlayer;
  }

  public String getPlyCount() {
    return plyCount;
  }

  public void setPlyCount(String plyCount) {
    this.plyCount = plyCount;
  }

  public GameResult getResult() {
    return result;
  }

  public void setResult(GameResult result) {
    this.result = result;
  }

  public Map<Integer, MoveList> getVariations() {
    return variations;
  }

  public void setVariations(Map<Integer, MoveList> variations) {
    this.variations = variations;
  }

  public Map<Integer, String> getCommentary() {
    return commentary;
  }

  public void setCommentary(Map<Integer, String> commentary) {
    this.commentary = commentary;
  }

  public Map<Integer, String> getNag() {
    return nag;
  }

  public void setNag(Map<Integer, String> nag) {
    this.nag = nag;
  }


  public MoveList getHalfMoves() {
    if (halfMoves == null) {
      setHalfMoves(new MoveList());
    }
    return halfMoves;
  }


  public void setHalfMoves(MoveList halfMoves) {
    this.halfMoves = halfMoves;
    setCurrentMoveList(halfMoves);
  }


  public String getFen() {
    return fen;
  }


  public void setFen(String fen) {
    this.fen = fen;
  }


  public Board getBoard() {
    return board;
  }


  public void setBoard(Board board) {
    this.board = board;
  }


  public String toPgn(boolean includeVariations, boolean includeComments) throws MoveConversionException {
    StringBuilder sb = new StringBuilder();

    sb.append(makeProp("White", getWhitePlayer()));
    sb.append(makeProp("Black", getBlackPlayer()));
    sb.append(makeProp("Result", getResult().getDescription()));
    sb.append(makeProp("PlyCount", getPlyCount()));

    if (getFen() != null && !getFen().equals("")) {
      sb.append(makeProp("FEN", getFen()));
    }
    if (getEco() != null && !getEco().equals("")) {
      sb.append(makeProp("ECO", getEco()));
    }

    if (getProperty() != null) {
      for (Entry<String, String> entry : getProperty().entrySet()) {
        sb.append(makeProp(entry.getKey(), entry.getValue()));
      }
    }

    sb.append('\n');
    int index = 0;
    int moveCounter = getInitialPosition() + 1;
    int variantIndex = 0;
    int lastSize = sb.length();

    if (getHalfMoves().size() == 0) {
      sb.append(getMoveText().toString());
    } else {
      sb.append(moveCounter);
      if (moveCounter % 2 == 0) {
        sb.append(".. ");
      } else {
        sb.append(". ");
      }
      final String sanArray[] = getHalfMoves().toSANArray();
      for (int i = 0; i < sanArray.length; i++) {
        String san = sanArray[i];
        index++;
        variantIndex++;

        sb.append(san);
        sb.append(' ');

        if (sb.length() - lastSize > 80) {
          sb.append("\n");
          lastSize = sb.length();
        }
        if (getNag() != null) {
          String nag = getNag().get(variantIndex);
          if (nag != null) {
            sb.append(nag);
            sb.append(' ');
          }
        }

        if (getCommentary() != null) {
          String comment = getCommentary().get(variantIndex);
          if (comment != null) {
            sb.append("{");
            sb.append(comment);
            sb.append("}");
          }
        }
        if (getVariations() != null) {
          MoveList var = getVariations().get(variantIndex);
          if (var != null) {
            variantIndex = translateVariation(sb, var, -1,
                variantIndex, index, moveCounter, lastSize);
            if (index % 2 != 0) {
              sb.append(moveCounter);
              sb.append("... ");
            }
          }
        }
        if (i < sanArray.length - 1 &&
            index % 2 == 0 && index >= 2) {
          moveCounter++;

          sb.append(moveCounter);
          sb.append(". ");
        }
      }
    }
    sb.append(getResult().getDescription());
    return sb.toString();

  }

  private int translateVariation(StringBuilder sb, MoveList variation, int parent,
      int variantIndex, int index, int moveCounter, int lastSize) throws MoveConversionException {
    final int variantIndexOld = variantIndex;
    if (variation != null) {
      boolean terminated = false;
      sb.append("(");
      int i = 0;
      int mc = moveCounter;
      int idx = index;
      String sanArray[] = variation.toSANArray();
      for (i = 0; i < sanArray.length; i++) {
        String sanMove = sanArray[i];
        if (i == 0) {
          sb.append(mc);
          if (idx % 2 == 0) {
            sb.append("... ");
          } else {
            sb.append(". ");
          }
        }

        variantIndex++;

        sb.append(sanMove);
        sb.append(' ');
        final MoveList child = getVariations().get(variantIndex);
        if (child != null) {
          if (i == sanArray.length - 1 &&
              variantIndexOld != child.getParent()) {
            terminated = true;
            sb.append(") ");
          }
          variantIndex = translateVariation(sb, child, variantIndexOld,
              variantIndex, idx, mc, lastSize);
        }
        if (idx % 2 == 0 && idx >= 2
            && i < sanArray.length - 1) {
          mc++;

          sb.append(mc);
          sb.append(". ");
        }
        idx++;

      }
      if (!terminated) {
        sb.append(") ");
      }

    }
    return variantIndex;
  }


  @Override
  public String toString() {
    try {
      return toPgn(true, true);
    } catch (MoveConversionException e) {
      return null;
    }
  }


  public String getGameId() {
    return gameId;
  }


  public void setGameId(String gameId) {
    this.gameId = gameId;
  }


  public int getPosition() {
    return position;
  }


  public void setPosition(int position) {
    this.position = position;
  }

  public int getInitialPosition() {
    return initialPosition;
  }

  public void setInitialPosition(int initialPosition) {
    this.initialPosition = initialPosition;
  }


  public MoveList getCurrentMoveList() {
    return currentMoveList;
  }


  public void setCurrentMoveList(MoveList currentMoveList) {
    this.currentMoveList = currentMoveList;
  }


  public String getEco() {
    return eco;
  }


  public void setEco(String eco) {
    this.eco = eco;
  }

  public String getOpening() {
    return opening;
  }

  public void setOpening(String opening) {
    this.opening = opening;
  }


  public StringBuilder getMoveText() {
    return moveText;
  }

  public void setMoveText(StringBuilder moveText) {
    this.moveText = moveText;
  }

  boolean loaded = false;
  public void loadMoveText() throws PgnException, MoveConversionException {
    if (!loaded && getMoveText() != null) {
      loaded=true;
      _loadMoveText(getMoveText());      
    }    
  }

  
  private void _loadMoveText(StringBuilder moveText) throws PgnException, MoveConversionException {

    getHalfMoves().clear();
    if (getVariations() != null) {
      getVariations().clear();
    }
    if (getCommentary() != null) {
      getCommentary().clear();
    }
    if (getNag() != null) {
      getNag().clear();
    }

    StringUtil.replaceAll(moveText, "\n", " \n ");
    StringUtil.replaceAll(moveText, "{", " { ");
    StringUtil.replaceAll(moveText, "}", " } ");
    StringUtil.replaceAll(moveText, "(", " ( ");
    StringUtil.replaceAll(moveText, ")", " ) ");

    String text = moveText.toString();
    if (text == null) {
      return;
    }
    if (getHalfMoves() == null) {
      if (getFen() != null && !getFen().trim().equals("")) {
        setHalfMoves(new MoveList(getFen()));
      } else {
        setHalfMoves(new MoveList());
      }
    }
    StringBuilder moves = new StringBuilder();
    StringBuilder comment = null;
    LinkedList<RTextEntry> variation = new LinkedList<RTextEntry>();

    int halfMove = 0;
    int variantIndex = 0;

    boolean onCommentBlock = false;
    boolean onVariationBlock = false;
    boolean onLineCommentBlock = false;
    for (String token : text.split(" ")) {
      if (token == null || token.trim().equals("")) {
        continue;
      }
      if (!(onLineCommentBlock || onCommentBlock) &&
          token.indexOf("...") > -1) {
        token = StringUtil.afterSequence(token, "...");
        if (token == null ||
            token.trim().length() == 0) {
          continue;
        }
      }
      if (!(onLineCommentBlock || onCommentBlock) &&
          token.indexOf(".") > -1) {
        token = StringUtil.afterSequence(token, ".");
        if (token == null ||
            token.trim().length() == 0) {
          continue;
        }
      }
      if (!(onLineCommentBlock || onCommentBlock) &&
          token.startsWith("$")) {
        if (getNag() == null) {
          setNag(new HashMap<Integer, String>());
        }
        getNag().put(variantIndex, token);
        continue;
      }
      if (token.equals("{") &&
          !(onLineCommentBlock || onCommentBlock)) {
        onCommentBlock = true;
        comment = new StringBuilder();
        continue;
      } else if (token.equals("}") && !onLineCommentBlock) {
        onCommentBlock = false;
        if (comment != null) {
          if (getCommentary() == null) {
            setCommentary(new HashMap<Integer, String>());
          }
          getCommentary().put(variantIndex, comment.toString());
        }
        comment = null;
        continue;
      } else if (token.equals(";") && !onCommentBlock) {
        onLineCommentBlock = true;
        comment = new StringBuilder();
        continue;
      } else if (token.equals("\n") && onLineCommentBlock) {
        onLineCommentBlock = false;
        if (comment != null) {
          getCommentary().put(variantIndex, comment.toString());
        }
        comment = null;
        continue;
      } else if (token.equals("(") &&
          !(onCommentBlock) || onLineCommentBlock) {
        onVariationBlock = true;
        variation.add(new RTextEntry(variantIndex));
        continue;
      } else if (token.equals(")") && onVariationBlock &&
          !(onCommentBlock) || onLineCommentBlock) {
        onVariationBlock = false;
        if (variation != null) {
          final RTextEntry last = variation.pollLast();
          StringBuilder currentLine = new StringBuilder(getMovesAt(moves.toString(), halfMove));
          try {

            onVariationBlock = variation.size() > 0;

            for (RTextEntry entry : variation) {
              currentLine.append(getMovesAt(entry.text.toString(), entry.size));
            }

            MoveList tmp = new MoveList();
            tmp.loadFromSAN(getMovesAt(currentLine.toString(), last.index));
            MoveList var = MoveList.createMoveListFrom(tmp, tmp.size());
            var.loadFromSAN(last.text.toString());
            final RTextEntry parent = variation.peekLast();
            if (onVariationBlock && parent != null) {
              var.setParent(parent.index);
            } else {
              var.setParent(-1);
            }
            if (getVariations() == null) {
              setVariations(new HashMap<Integer, MoveList>());
            }
            getVariations().put(last.index, var);
          } catch (Exception e) {
            if (last != null && currentLine != null) {
              throw new PgnException("Error while reading variation: " +
                  getMovesAt(currentLine.toString(), last.index) + " - " +
                  last.text.toString(), e);
            } else {
              throw new PgnException("Error while reading variation: ", e);
            }
          }
          currentLine = null;
        }
        continue;
      }

      if (onCommentBlock || onLineCommentBlock) {
        if (comment != null) {
          comment.append(token);
          comment.append(" ");
        }
        continue;
      }

      if (onVariationBlock) {
        if (variation != null) {
          variation.getLast().text.append(token);
          variation.getLast().text.append(" ");
          variation.getLast().size++;
          variantIndex++;
        }
        continue;
      }
      variantIndex++;
      halfMove++;
      moves.append(token);
      moves.append(" ");
    }

    StringUtil.replaceAll(moves, "\n", " ");
    getHalfMoves().clear();
    getHalfMoves().loadFromSAN(moves.toString());

  }


  public void gotoMove(final MoveList moves, int index) throws MoveException {
    setCurrentMoveList(moves);
    if (getBoard() != null &&
        index >= 0 && index < moves.size()) {
      getBoard().loadFromFEN(moves.getStartFEN());

      int i = 0;
      for (Move move : moves) {
        if (!getBoard().doMove(move, true)) {
          throw new MoveException("Couldn't load board state. Reason: Illegal move in PGN MoveText.");
        }
        i++;
        if (i - 1 == index) {
          break;
        }
      }
      setPosition(i - 1);

    }

  }

  public void gotoFirst(final MoveList moves) throws MoveException {
    gotoMove(moves, 0);
  }

  public void gotoLast(final MoveList moves) throws MoveException {
    gotoMove(moves, getHalfMoves().size() - 1);
  }

  public void gotoNext(final MoveList moves) throws MoveException {
    gotoMove(moves, getPosition() + 1);
  }

  public void gotoPrior(final MoveList moves) throws MoveException {
    gotoMove(moves, getPosition() - 1);
  }

  public void gotoFirst() throws MoveException {
    gotoFirst(getCurrentMoveList());
  }

  public void gotoLast() throws MoveException {
    gotoLast(getCurrentMoveList());
  }

  public void gotoNext() throws MoveException {
    gotoNext(getCurrentMoveList());
  }

  public void gotoPrior() throws MoveException {
    gotoPrior(getCurrentMoveList());
  }

  public boolean isEndOfMoveList() {
    return getCurrentMoveList() == null || getPosition() >= getCurrentMoveList().size() - 1;
  }

  public boolean isStartOfMoveList() {
    return getCurrentMoveList() == null && getPosition() == 0;
  }

  public Map<String, String> getProperty() {
    return property;
  }

  public void setProperty(Map<String, String> property) {
    this.property = property;
  }

  class RTextEntry {
    int index;
    int size;
    StringBuilder text = new StringBuilder();

    public RTextEntry(int index) {
      this.index = index;
      this.size = 0;
    }
  }

  public void setWhiteElo(int elo) {
    whiteElo = elo;

  }

  public void setBlackElo(int elo) {
    blackElo = elo;

  }

  public int getWhiteElo() {

    return whiteElo;

  }

  public int getBlackElo() {
    return blackElo;

  }

}

