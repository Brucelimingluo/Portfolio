package game2048;

import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author TODO: YOUR NAME HERE
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;

        // TODO: Modify this.board (and perhaps this.score) to account
        // for the tilt to the Side SIDE. If the board changed, set the
        // changed local variable to true.

        board.setViewingPerspective(side);
        for (int c = 3; c >= 0; c-=1) {
            int top = 3;
            int swap = 1;
            for (int r = 3; r >= 0; r-=1) {
                Tile t = board.tile(c, r);
                if (t != null && r != top) {
                    if (board.tile(c, top) == null) {
                        board.move(c, top, t);
                        changed = true;
                    }
                    else {
                        if (t.value() != board.tile(c, top).value()) {
                            top -= 1;
                            changed = true;
                            board.move(c, top, t);
                        }
                        else {
                            if (swap == t.value()) {
                                top -= 1;
                                board.move(c, top, t);
                            } else {
                                changed = true;
                                board.move(c, top, t);
                                score += board.tile(c, top).value();
                                swap = board.tile(c, top).value();
                            }
                        }
                    }
                }
            }
        }
        board.setViewingPerspective(Side.NORTH);


















//        if (side != Side.NORTH) {
//            board.setViewingPerspective(side);
////            board.setViewingPerspective(Side.NORTH);
//            tilt(Side.NORTH);
//            board.setViewingPerspective(Side.NORTH);
//        }
//        else {

/*
            for (int r = 3; r >= 0; r -= 1) {
                for (int c = 3; c >= 0; c -= 1) {
                    Tile t = board.tile(c, r);
                    if (board.tile(c, r) != null) {
                        //????????????????????? ????????????
                        if (validIndex(r - 1) && (board.tile(c, r - 1) != null) && (board.tile(c, r).value() == board.tile(c, r - 1).value())) {
                            board.move(c, r, board.tile(c, r - 1));
                            changed = true;
                            score += board.tile(c, r).value();

                            //???????????? ??????null ???????????????
                            if (validIndex(r + 1) && (board.tile(c, r + 1) == null)) {
                                board.move(c, r + 1, board.tile(c, r));
                            }
                            //????????????????????????
                            if (validIndex(r - 2) && (board.tile(c, r - 2) == null)) {
                                //???????????????????????? ????????????r-1
                                if (validIndex(r - 3) && (board.tile(c, r - 3)) != null) {
                                    board.move(c, r - 1, board.tile(c, r - 3));
                                }
                            }
                            //?????????????????????
                            else if (validIndex(r - 2) && (board.tile(c, r - 2)) != null) {
                                //???????????????????????? ???????????????
//                                if (validIndex(r - 3) && board.tile(c, r - 3) != null && board.tile(c, r - 2).value() == board.tile(c, r - 3).value()) {
//                                    board.move(c, r - 1, board.tile(c, r - 2));
//                                    board.move(c, r - 1, board.tile(c, r - 3));
//                                    changed = true;
//                                    score += board.tile(c, r - 1).value();
//                                }
                                //???????????????????????? ???????????????
                                if (validIndex(r - 3) && board.tile(c, r - 3) != null && board.tile(c, r - 2).value() == board.tile(c, r - 3).value()) {
//                                    board.move(c, r , board.tile(c, r - 1));
                                    board.move(c, r - 2, board.tile(c, r - 3));
                                    board.move(c, r-1, board.tile(c, r-2));
                                    changed = true;
//                                    score += board.tile(c, r - 1).value();
                                    score += board.tile(c, r).value();
                                }
                                //??????????????????
                                else if ((validIndex(r-3) == false)) {
//                                    board.move(c, r, board.tile(c, r-1));
//                                    board.move(c, r+1, board.tile(c, r));
                                    board.move(c, r, board.tile(c, r-2));
                                    changed = true;
//                                    score += board.tile(c, r+1).value();
                                }
                                //????????????????????????
                                 else if ((validIndex(r-3) && board.tile(c, r-3) == null)) {
//                                     board.move(c, r, board.tile(c, r-1));
                                     board.move(c, r-1, board.tile(c, r-2));
                                     changed = true;
//                                     score += board.tile(c, r).value();
                                 }
                            }

                        }
                        // ????????????????????? ?????????
                        else if ((validIndex(r - 1) && (board.tile(c, r - 1) != null) && (board.tile(c, r).value() != board.tile(c, r - 1).value()))) {
                            //????????????????????? ????????????
                            if (validIndex(r-2) && (board.tile(c, r-2) != null) && (board.tile(c, r-2).value() == board.tile(c, r-1).value())) {
                                board.move(c, r - 1, board.tile(c, r - 2));
                                if (validIndex(r + 1) && board.tile(c, r + 1) == null) {
                                    board.move(c, r+1, board.tile(c, r));
                                    board.move(c, r, board.tile(c, r-1));
                                }
                                changed = true;
                                }
                            //????????????????????????
                            else if (validIndex(r-2) && (board.tile(c, r-2) == null)) {
                                //?????????????????????
                                if (validIndex(r + 1) && board.tile(c, r + 1) == null) {
                                    board.move(c, r + 1, board.tile(c, r));
                                    board.move(c, r, board.tile(c, r - 1));
                                    changed = true;
                                }
//                                changed = true;
                            }

                            }

                        //?????????????????????
                        else if (validIndex(r - 1) && (board.tile(c, r - 1) == null)) {
                            //?????????????????????
                            if (validIndex(r - 2) && (board.tile(c, r - 2) == null)) {
                                //????????????????????????
                                if (validIndex(r-3) && (board.tile(c, r-3) == null)) {
//                                    changed = false;
                                }
                                //???????????????????????? ???????????????
                                else if (validIndex(r-3) && board.tile(c, r-3).value() == board.tile(c, r).value()) {
                                    board.move(c, r, board.tile(c, r-3));
                                    changed = true;
                                    score += board.tile(c, r).value();

                                }
                                //????????????????????????
                                else if (validIndex(r + 1) && board.tile(c, r + 1) == null) {
                                    board.move(c, r + 1, t);
                                    changed = true;
                                }
                                else {
                                    changed = true;
                                }
                                //????????????????????????



                            }
                            //????????????????????? ?????????????????? ?????????????????????
                            else if (validIndex(r - 2) && (board.tile(c, r - 2) != null) && (board.tile(c, r - 2)).value() == t.value()) {
                                board.move(c, r, board.tile(c, r - 2));
                                //156-159 testUpTripleMerge
                                //???????????????????????? ?????????????????????
                                if (validIndex(r - 3) && board.tile(c, r - 3) != null) {
                                    board.move(c, r - 1, board.tile(c, r - 3));
                                    changed = true;
                                    score += board.tile(c, r).value();
                                }
                                //????????????????????? ???????????????
                                else if ((validIndex(r-3) == false)) {
                                    board.move(c, r+1, board.tile(c, r));
                                    changed = true;
                                    score += board.tile(c, r+1).value();

                                }

                            }
                            //????????????????????? ??????????????? ??????????????????
                            else if (validIndex(r - 2) && (board.tile(c, r - 2) != null) && (board.tile(c, r - 2)).value() != t.value()) {
                                board.move(c, r - 1, board.tile(c, r - 2));
                                //???????????????????????? ????????????????????? move
                                if ((validIndex(r - 3)) && (board.tile(c, r - 3) != null) &&
                                        board.tile(c, r - 3).value() == board.tile(c, r - 1).value()) {
                                    board.move(c, r - 1, board.tile(c, r - 3));
                                    changed = true;
                                    score += board.tile(c, r - 1).value();
                                }
                                //??????????????????
                                if ((validIndex(r -3) == false)) {
                                    board.move(c, r+1, board.tile(c, r));
                                    board.move(c, r, board.tile(c, r-1));
                                    changed = true;
                                }
                            }
//                        //???????????? ????????????????????? ????????????
//                        if (validIndex(r+1) && (board.tile(c, r+1)) == null) {
//                            board.move(c, r+1, t);
//                        }
//                        else {
//                            continue;
//                        }

//                        changed = true;
//                        score+= 2*t.value();
                        } else if ((validIndex(r - 1) == false)) {
                            board.move(c, 3, t);
                            changed = true;
                        }
//                    else if (validIndex(c+1) && (board.tile(c,r).value() == board.tile(c+1,r).value())) {
//                        board.move(c+1, r, t);
//                        changed = true;
//                        score += board.tile(c,r).value();
//                    }
                        else {
//                        continue;
                            System.out.println(12345);
                        }

                    } else {
                        System.out.println(11111);
                        ;
                    }
                }
            }
//        }
              board.setViewingPerspective(Side.NORTH);
            //board.setViewingPerspective(NORTH);

        */

            checkGameOver();
            if (changed) {
                setChanged();
            }
            return changed;
        }


    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        // TODO: Fill in this function.
        System.out.println(b.tile(0, 0));
        if( (b.tile(0,0) == null) ||
                (b.tile(0,1) == null) ||
                (b.tile(0,2) == null) ||
                (b.tile(0,3) == null) ||
                (b.tile(1,0) == null) ||
                (b.tile(1,1) == null) ||
                (b.tile(1,2) == null) ||
                (b.tile(1,3) == null) ||
                (b.tile(2,0) == null) ||
                (b.tile(2,1) == null) ||
                (b.tile(2,2) == null) ||
                (b.tile(2,3) == null) ||
                (b.tile(3,0) == null) ||
                (b.tile(3,1) == null) ||
                (b.tile(3,2) == null) ||
                (b.tile(3,3) == null))

                {
            return true;
        }
        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
//
//    public static boolean maxTileExists(Board b) {
//        // TODO: Fill in this function.
//        System.out.println(b.tile(0, 0));
//        for (int x = 0; x < 4; x += 1) {
//            for (int y = 0; y < 4; y += 1) {
//                if ((b.tile(x,y) == null)){
//                    return false;
//                }
//                if (b.tile(x, y).value() == MAX_PIECE) {
//                    return true;
//                }
//
//            }
//        }
//        return false;
//    }
    public static boolean maxTileExists(Board b) {
        // TODO: Fill in this function.
        System.out.println(b.tile(0, 0));
        for (int x = 0; x < 4; x += 1) {
            for (int y = 0; y < 4; y += 1) {
                if (b.tile(x,y) != null) {
                    if ((b.tile(x, y).value() == MAX_PIECE) ){
                        return true;
                    }
                }



            }
        }
        return false;
    }

//        if( (x = MAX_PIECE) ||
//                (b.tile(0,1) = MAX_PIECE) ||
//                (b.tile(0,2) == null) ||
//                (b.tile(0,3) == null) ||
//                (b.tile(1,0) == null) ||
//                (b.tile(1,1) == null) ||
//                (b.tile(1,2) == null) ||
//                (b.tile(1,3) == null) ||
//                (b.tile(2,0) == null) ||
//                (b.tile(2,1) == null) ||
//                (b.tile(2,2) == null) ||
//                (b.tile(2,3) == null) ||
//                (b.tile(3,0) == null) ||
//                (b.tile(3,1) == null) ||
//                (b.tile(3,2) == null) ||
//                (b.tile(3,3) == null))
//
//        {
//            return true;
//        }
//        return false;

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        // TODO: Fill in this function.
        if (emptySpaceExists(b)) {
            return true;
        }
        for (int x = 0; x < 4; x+=1) {
            for (int y = 0; y < 4; y += 1) {
                if ((b.tile(x,y) != null) ) {
                    if (validIndex(y+1) && (b.tile(x,y).value() == b.tile(x,y+1).value()) ||
                            ( validIndex(y-1) && b.tile(x,y).value() == b.tile(x,y-1).value()) ||
                            (validIndex(x-1) && b.tile(x,y).value() == b.tile(x-1,y).value()) ||
                            (validIndex(x+1)) && b.tile(x,y).value() == b.tile(x+1,y).value()) {
                        return true;
                        }
                }
            }

        }
        return false;
    }

//    public static boolean range(int x, int y) {
//        if ((x < 0) || (x > 3) || (y < 0) || (y > 3)) {
//            return false;
//        }
//        return true;
//    }

    public static boolean validIndex(int a) {
        if (a < 0) {
            return false;
        }
        if (a >= 4) {
            return false;
        }
        return true;
    }

    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model???s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
