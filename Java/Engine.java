package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class Engine {

    public static final int ST_INTERACT = 20;
    public static final int WIDTH = 80;
    public static final int HEIGHT = 30;
    public static final int HUD_HEIGHT = 2;
    public static final int BAR_HEIGHT = 0;
    public static final int TOTAL_HEIGHT = HEIGHT + HUD_HEIGHT + BAR_HEIGHT;
    public static final int TOTAL_WIDTH = WIDTH;
    private static final Pattern PATTERN = Pattern.compile("[0-9]*");
    /******************************************************************************************
     The following are final static variables
     *****************************************************************************************/

    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {1, -1, 0, 0};  // up, down, left, right
    private static final String[] MOVE_MESSAGE = {"move up",
        "move down", "move left", "move right"};
    private static final Font HUD_FONT = new Font("Times New Roman", Font.PLAIN, 20);
    private static final Font BIG_TITLE_FONT = new Font("Monoca", Font.BOLD, 40);
    private static final Font MID_TITLE_FONT = new Font("Times New Roman", Font.BOLD, 30);
    private static final Font LITTLE_TITLE_FONT = new Font("Times New Roman", Font.PLAIN, 20);

    private static final int ST_AUTO_PLAY = 10;

    private static final int ST_WAIT = 30;
    private static final int ST_QUIT = 40;
    private static final int MIST_RADIUS = 4;

    private static final String[] FILE_NAME = {"", "1.txt", "2.txt", "3.txt", "4.txt", "5.txt"};
    private static final long TIME_PAUSE = 300L;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy z");
    private final boolean[][] TAKEN = new boolean[80][30];
    private final ArrayList<Room> rooms = new ArrayList<>();
    private final ArrayList<Command> history = new ArrayList<>();

    private final int barX = 10;
    private final int hudX = (int) (WIDTH * 0.9);
    private final int hudY = HUD_HEIGHT / 2 + HEIGHT + BAR_HEIGHT;
    private final int hudHalfWidth = (int) (WIDTH * 0.1);
    private final int hudHalfHeight = HUD_HEIGHT / 2;
    TERenderer ter = new TERenderer();
    private TETile[][] board;
    private Queue<Command> queue;
    private Integer status;
    private Boolean hasMist = false;
    private int avatarX;
    private int avatarY;
    private int doorX;
    private int doorY;
    private int theme = 1;
    private String name = "AVATAR";  // (default) username
    private boolean needStdDraw;  // whether drawing is needed, specifically for the autograder

    private final Thread execThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (Engine.this.status != ST_QUIT) {
                Engine.this.pause(TIME_PAUSE);
                execute();
            }
        }
    });

    private final Thread hudThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (Engine.this.status != ST_QUIT) {
                Engine.this.pause(TIME_PAUSE);
                if (board == null) {
                    continue;
                }
                if (!Engine.this.needStdDraw) {
                    continue;
                }

                double dx = StdDraw.mouseX();
                double dy = StdDraw.mouseY();
                int x = (int) dx, y = (int) dy;
                if (0 <= x && x < WIDTH && BAR_HEIGHT <= y && y < BAR_HEIGHT + HEIGHT) {
                    TETile type = board[x][y];
                    if (type == Tileset.WALL) {
                        showHud("wall");
                    } else if (type == Tileset.AVATAR) {
                        showHud(name);
                    } else if (type == Tileset.LOCKED_DOOR) {
                        showHud("EXIT");
                    } else {
                        showHud("empty space");
                    }
                }
            }
            if (Main.DEBUG) {
                System.out.println("exit hud thread.");
            }
        }
    });

    private final Thread timeThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (status != ST_QUIT) {
                Engine.this.pause(1000L);
                if (Engine.this.needStdDraw) {
                    Engine.this.showTime();
                }
            }
        }
    });

    private void init(String cmd, boolean flag) {
        queue = new ConcurrentLinkedQueue<>();
        status = ST_INTERACT;
        this.needStdDraw = flag;

        if (!cmd.isEmpty()) {
            if (!initQueueWithArg(cmd)) {
                queue.clear();
                queue.add(Command.initCmd());
            } else {
                status = ST_AUTO_PLAY;
                if (this.needStdDraw) {
                    queue.add(Command.enableInteractCmd());
                } else if (!cmd.endsWith(":Q") && !cmd.endsWith(":q")) {
                    queue.add(Command.quitDefaultCmd());
                }
            }
        } else {
            queue.add(Command.initCmd());
        }

        if (!this.needStdDraw) {
            return;
        }

        StdDraw.setCanvasSize(TOTAL_WIDTH * 16, TOTAL_HEIGHT * 16);
        StdDraw.clear(Color.BLACK);
        StdDraw.setXscale(0, TOTAL_WIDTH);
        StdDraw.setYscale(0, TOTAL_HEIGHT);
        StdDraw.enableDoubleBuffering();
    }


    private boolean initQueueWithArg(String cmd) {
        if (!"N".equalsIgnoreCase(cmd.substring(0, 1))
                &&
                !"L".equalsIgnoreCase(cmd.substring(0, 1))) {
            System.out.println();
            return false;
        }

        int index = 0;
        if ("N".equalsIgnoreCase(cmd.substring(0, 1))) {
            index = 1;
            if (index == cmd.length()
                    || !PATTERN.matcher(cmd.substring(index, index + 1)).matches()) {
                return false;
            }
            while (index < cmd.length()
                    && PATTERN.matcher(cmd.substring(index, index + 1)).matches()) {
                index += 1;
            }
            long seed = Long.parseLong(cmd.substring(1, index));
            if (index == cmd.length() || !"s".equalsIgnoreCase(cmd.substring(index, index + 1))) {
                return false;
            }
            queue.add(Command.newGameCmd(seed));
        } else {
            queue.add(Command.loadGameDefaultCmd());
        }
        index += 1;

        if (Main.DEBUG) {
            System.out.println("index = " + index);
            System.out.println(cmd);
        }

        while (index < cmd.length()) {
            String cur = cmd.substring(index, index + 1);
            if ("A".equalsIgnoreCase(cur)) {
                queue.add(Command.moveLeftCmd());
            } else if ("W".equalsIgnoreCase(cur)) {
                queue.add(Command.moveUpCmd());
            } else if ("S".equalsIgnoreCase(cur)) {
                queue.add(Command.moveDownCmd());
            } else if ("D".equalsIgnoreCase(cur)) {
                queue.add(Command.moveRightCmd());
            } else if (":".equals(cur)) {
                index += 1;
                cur = cmd.substring(index, index + 1);
                if ("Q".equalsIgnoreCase(cur)) {
                    queue.add(Command.quitAndSaveDefaultCmd());
                } else {

                    return false;
                }
            }
            index += 1;
        }

        return true;
    }

    public void interactWithKeyboard() {
        this.init("", true);
        this.run();
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     * <p>
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     * <p>
     * In other words, both of these calls:
     * - interactWithInputString("n123sss:q")
     * - interactWithInputString("lww")
     * <p>
     * should yield the exact same world state as:
     * - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */


    //create the first room, with 3 doors
    public Room createRoom(Random random, TETile[][] tiles, int x, int y, int W, int H) {
        Room room = new Room(H, W, new Position(x, y));
        for (int i = x; i < x + W; i++) {
            for (int j = y; j < y + H; j++) {
                tiles[i][j] = Tileset.FLOOR;
                TAKEN[i][j] = true;
                if (i == x || i == x + W - 1 || j == y || j == y + H - 1) {
                    tiles[i][j] = Tileset.WALL;
                    TAKEN[i][j] = true;
                }
            }
        }
        return room;
    }

    public boolean success(Random random, TETile[][] tiles, int x, int y, int W, int H) {
        if (x + W - 1 >= WIDTH || y + H - 1 >= HEIGHT) {
            return false;
        }
        for (int i = x; i < x + W; i++) {
            for (int j = y; j < y + H; j++) {
                if (TAKEN[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public void randomRooms(Random random, TETile[][] tiles, int roomNum) {

        for (int i = 0; i < roomNum; i++) {
            int x = RandomUtils.uniform(random, 3, WIDTH - 3);
            int y = RandomUtils.uniform(random, 3, HEIGHT - 3);
            Position basePos = new Position();
            basePos.setX(x);
            basePos.setY(y);
            int H = RandomUtils.uniform(random, 4, 12);
            int W = RandomUtils.uniform(random, 4, 12);
            if (success(random, tiles, x, y, W, H)) {
                Room room = createRoom(random, tiles, x, y, W, H);
                rooms.add(room);
            } else {
                continue;
            }
        }
    }

    public void connect(Random random, Room r, TETile[][] tiles) {
        int downX = RandomUtils.uniform(random, r.getBasePos().getX() + 1,
                r.getBasePos().getX() + r.getWidth() - 1);
        int upX = RandomUtils.uniform(random, r.getBasePos().getX() + 1,
                r.getBasePos().getX() + r.getWidth() - 1);
        int rightY = RandomUtils.uniform(random, r.getBasePos().getY() + 1,
                r.getBasePos().getY() + r.getHeight() - 1);
        int leftY = RandomUtils.uniform(random, r.getBasePos().getY() + 1,
                r.getBasePos().getY() + r.getHeight() - 1);


        TETile[][] mod1 = searchDown(new Position(downX, r.getBasePos().getY()), tiles);
        TETile[][] mod2 = searchUp(new Position(upX, r.getBasePos().getY()
                + r.getHeight() - 1), mod1);
        TETile[][] mod3 = searchLeft(new Position(r.getBasePos().getX(), leftY), mod2);
        TETile[][] mod4 = searchRight(new Position(r.getBasePos().getX()
                + r.getWidth() - 1, rightY), mod3);
    }

    private TETile[][] searchDown(Position down, TETile[][] tiles) {
        int x = down.getX();
        int y = down.getY();
        Position temp = down;
        if (y == 0 || y == 1) {
            return tiles;
        }
        if (tiles[x][y] == Tileset.FLOOR) {
            return tiles;
        }
        if (tiles[down.getX()][down.getY() - 1] == Tileset.FLOOR) {
            tiles[down.getX()][down.getY()] = Tileset.FLOOR;
            return tiles;
        } else {
            tiles[down.getX()][down.getY()] = Tileset.FLOOR;
            while (tiles[temp.getX()][temp.getY() - 1] != Tileset.WALL) {
//                if (temp.getY() - 1 == 0) {
//                    tiles[temp.getX()][temp.getY() - 1] = Tileset.WALL;
//                    tiles[temp.getX() + 1][temp.getY() - 1] = Tileset.WALL;
//                    tiles[temp.getX() - 1][temp.getY() - 1] = Tileset.WALL;
//                    return tiles;
//                }
//                tiles[temp.getX()][temp.getY() - 1] = Tileset.FLOOR;
//                tiles[temp.getX() + 1][temp.getY() - 1] = Tileset.WALL;
//                tiles[temp.getX() - 1][temp.getY() - 1] = Tileset.WALL;


                tiles[temp.getX()][temp.getY() - 1] = Tileset.FLOOR;
                tiles[temp.getX() + 1][temp.getY() - 1] = Tileset.WALL;
                tiles[temp.getX() - 1][temp.getY() - 1] = Tileset.WALL;
                if (temp.getY() - 1 == 0) {
                    tiles[temp.getX()][temp.getY() - 1] = Tileset.WALL;
                    return tiles;
                }

                temp.setY(temp.getY() - 1);
            }
            if (tiles[temp.getX()][temp.getY() - 2] != Tileset.FLOOR) {
                tiles[temp.getX()][temp.getY() - 1] = Tileset.WALL;
            } else {
                tiles[temp.getX()][temp.getY() - 1] = Tileset.FLOOR;
            }
            tiles[temp.getX() + 1][temp.getY() - 1] = Tileset.WALL;
            tiles[temp.getX() - 1][temp.getY() - 1] = Tileset.WALL;

//            if (tiles[temp.getX()][temp.getY() - 2] == Tileset.WALL) {
//                tiles[temp.getX()][temp.getY() - 2] = Tileset.FLOOR;
//                if (tiles[temp.getX() - 1][temp.getY() - 2] == Tileset.FLOOR) {
//                    tiles[temp.getX() + 1][temp.getY() - 2] = Tileset.WALL;
//                }
//                else if (tiles[temp.getX() + 1][temp.getY() - 2] == Tileset.FLOOR) {
//                    tiles[temp.getX() - 1][temp.getY() - 2] = Tileset.WALL;
//                }
//            }
        }
        return tiles;
    }

    private TETile[][] searchUp(Position up, TETile[][] tiles) {
        int x = up.getX();
        int y = up.getY();
        Position temp = up;
        if (y == HEIGHT - 1 || y == HEIGHT - 2) {
            return tiles;
        }
        if (tiles[up.getX()][up.getY()] == Tileset.FLOOR) {
            return tiles;
        }
        if (tiles[up.getX()][up.getY() + 1] == Tileset.FLOOR) {
            tiles[up.getX()][up.getY()] = Tileset.FLOOR;
            return tiles;
        } else {
            tiles[up.getX()][up.getY()] = Tileset.FLOOR;
            while (tiles[temp.getX()][temp.getY() + 1] != Tileset.WALL) {
//                if (temp.getY() + 1 == HEIGHT - 1) {
//                    tiles[temp.getX()][temp.getY() + 1] = Tileset.WALL;
//                    tiles[temp.getX() + 1][temp.getY() + 1] = Tileset.WALL;
//                    tiles[temp.getX() - 1][temp.getY() + 1] = Tileset.WALL;
//                    return tiles;
//                }
//
//                tiles[temp.getX()][temp.getY() + 1] = Tileset.FLOOR;
//                tiles[temp.getX() + 1][temp.getY() + 1] = Tileset.WALL;
//                tiles[temp.getX() - 1][temp.getY() + 1] = Tileset.WALL;

                tiles[temp.getX()][temp.getY() + 1] = Tileset.FLOOR;
                tiles[temp.getX() + 1][temp.getY() + 1] = Tileset.WALL;
                tiles[temp.getX() - 1][temp.getY() + 1] = Tileset.WALL;
                if (temp.getY() + 1 == HEIGHT - 1) {
                    tiles[temp.getX()][temp.getY() + 1] = Tileset.WALL;
                    return tiles;
                }

                temp.setY(temp.getY() + 1);
            }
            if (tiles[temp.getX()][temp.getY() + 2] != Tileset.FLOOR) {
                tiles[temp.getX()][temp.getY() + 1] = Tileset.WALL;

            } else {
                tiles[temp.getX()][temp.getY() + 1] = Tileset.FLOOR;
            }
            tiles[temp.getX() + 1][temp.getY() + 1] = Tileset.WALL;
            tiles[temp.getX() - 1][temp.getY() + 1] = Tileset.WALL;

//            if (tiles[temp.getX()][temp.getY() + 2] == Tileset.WALL) {
//                tiles[temp.getX()][temp.getY() + 2] = Tileset.FLOOR;
//                if (tiles[temp.getX() - 1][temp.getY() + 2] == Tileset.FLOOR) {
//                    tiles[temp.getX() + 1][temp.getY() + 2] = Tileset.WALL;
//                } else if (tiles[temp.getX() + 1][temp.getY() + 2] == Tileset.FLOOR) {
//                    tiles[temp.getX() - 1][temp.getY() + 2] = Tileset.WALL;
//                }
//            }
        }
        return tiles;
    }

    private TETile[][] searchLeft(Position left, TETile[][] tiles) {
        int x = left.getX();
        int y = left.getY();
        Position temp = left;
        if (x == 0 || x == 1) {
            return tiles;
        }
        if (tiles[left.getX()][left.getY()] == Tileset.FLOOR) {
            return tiles;
        }
        if (tiles[left.getX() - 1][left.getY()] == Tileset.FLOOR) {
            tiles[left.getX()][left.getY()] = Tileset.FLOOR;
            return tiles;
        } else {
            tiles[left.getX()][left.getY()] = Tileset.FLOOR;
            while (tiles[temp.getX() - 1][temp.getY()] != Tileset.WALL) {
//                if (temp.getX() - 1 == 0) {
//                    tiles[temp.getX() - 1][temp.getY()] = Tileset.WALL;
//                    tiles[temp.getX() - 1][temp.getY() + 1] = Tileset.WALL;
//                    tiles[temp.getX() - 1][temp.getY() - 1] = Tileset.WALL;
//                    return tiles;
//                }
//                tiles[temp.getX() - 1][temp.getY()] = Tileset.FLOOR;
//                tiles[temp.getX() - 1][temp.getY() + 1] = Tileset.WALL;
//                tiles[temp.getX() - 1][temp.getY() - 1] = Tileset.WALL;

                tiles[temp.getX() - 1][temp.getY()] = Tileset.FLOOR;
                tiles[temp.getX() - 1][temp.getY() + 1] = Tileset.WALL;
                tiles[temp.getX() - 1][temp.getY() - 1] = Tileset.WALL;
                if (temp.getX() - 1 == 0) {
                    tiles[temp.getX() - 1][temp.getY()] = Tileset.WALL;
                    return tiles;
                }

                temp.setX(temp.getX() - 1);
            }
            if (tiles[temp.getX() - 2][temp.getY()] != Tileset.FLOOR) {
                tiles[temp.getX() - 1][temp.getY()] = Tileset.WALL;
            } else {
                tiles[temp.getX() - 1][temp.getY()] = Tileset.FLOOR;
            }
            tiles[temp.getX() - 1][temp.getY() + 1] = Tileset.WALL;
            tiles[temp.getX() - 1][temp.getY() - 1] = Tileset.WALL;

//            if (tiles[temp.getX() - 2][temp.getY()] == Tileset.WALL) {
//                tiles[temp.getX() - 2][temp.getY()] = Tileset.FLOOR;
//                if (tiles[temp.getX() - 2][temp.getY() + 1] == Tileset.FLOOR) {
//                    tiles[temp.getX() - 2][temp.getY() - 1] = Tileset.WALL;
//                }
//                else if (tiles[temp.getX() - 2][temp.getY() - 1] == Tileset.FLOOR) {
//                    tiles[temp.getX() - 2][temp.getY() + 1] = Tileset.WALL;
//                }
//            }
        }
        return tiles;
    }

    private TETile[][] searchRight(Position right, TETile[][] tiles) {
        int x = right.getX();
        int y = right.getY();
        Position temp = right;
        if (x == WIDTH - 1 || x == WIDTH - 2) {
            return tiles;
        }
        if (tiles[right.getX()][right.getY()] == Tileset.FLOOR) {
            return tiles;
        }
        if (tiles[right.getX() + 1][right.getY()] == Tileset.FLOOR) {
            tiles[right.getX()][right.getY()] = Tileset.FLOOR;
            return tiles;
        } else {
            tiles[right.getX()][right.getY()] = Tileset.FLOOR;
            while (tiles[temp.getX() + 1][temp.getY()] != Tileset.WALL) {
//                if (temp.getX() + 1 == WIDTH - 1) {
//                    tiles[temp.getX() + 1][temp.getY()] = Tileset.WALL;
//                    tiles[temp.getX() + 1][temp.getY() + 1] = Tileset.WALL;
//                    tiles[temp.getX() + 1][temp.getY() - 1] = Tileset.WALL;
//                    return tiles;
//                }
//                tiles[temp.getX() + 1][temp.getY()] = Tileset.FLOOR;
//                tiles[temp.getX() + 1][temp.getY() + 1] = Tileset.WALL;
//                tiles[temp.getX() +1][temp.getY() - 1] = Tileset.WALL;


                tiles[temp.getX() + 1][temp.getY()] = Tileset.FLOOR;
                tiles[temp.getX() + 1][temp.getY() + 1] = Tileset.WALL;
                tiles[temp.getX() + 1][temp.getY() - 1] = Tileset.WALL;
                if (temp.getX() + 1 == WIDTH - 1) {
                    tiles[temp.getX() + 1][temp.getY()] = Tileset.WALL;
                    return tiles;
                }

                temp.setX(temp.getX() + 1);
            }
            if (tiles[temp.getX() + 2][temp.getY()] != Tileset.FLOOR) {
                tiles[temp.getX() + 1][temp.getY()] = Tileset.WALL;
            } else {
                tiles[temp.getX() + 1][temp.getY()] = Tileset.FLOOR;
            }
            tiles[temp.getX() + 1][temp.getY() + 1] = Tileset.WALL;
            tiles[temp.getX() + 1][temp.getY() - 1] = Tileset.WALL;
        }
        return tiles;
    }

    public void chooseLockedDoor(TETile[][] tiles) {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 1; j < HEIGHT; j++) {
                if (tiles[i][j] == Tileset.WALL && tiles[i][j + 1] == Tileset.WALL
                        && tiles[i][j - 1] == Tileset.WALL) {
                    tiles[i][j] = Tileset.LOCKED_DOOR;
                    doorX = i;
                    doorY = j;
                    return;
                }
            }
        }
    }

    private void chooseInitAvatar(Random random, TETile[][] tiles) {
        if (Main.DEBUG) {
            avatarX = doorX + 1;
            avatarY = doorY;
            tiles[avatarX][avatarY] = Tileset.AVATAR;
            return;
        }
        int k = 0;
        while (true) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            if (tiles[x][y] == Tileset.FLOOR && Math.abs(x - doorX)
                    + Math.abs(y - doorY) > WIDTH / (k / 1000 + 1)) {
                avatarX = x;
                avatarY = y;
                tiles[x][y] = Tileset.AVATAR;
                return;
            }
            k++;
        }
    }

    public void generateWorld(Random random, TETile[][] tiles) {
        randomRooms(random, tiles, 20);
        for (Room room : rooms) {
            connect(random, room, tiles);
        }
        chooseLockedDoor(tiles);
        chooseInitAvatar(random, tiles);

        // this.changeTheme(2);
    }

    //*
    public TETile[][] interactWithInputString(String input) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.initWithInputString(input);
        while (!queue.isEmpty()) {
            this.execute();
        }
        return board;
    }

    private boolean initWithInputString(String cmd) {
        if (!"N".equalsIgnoreCase(cmd.substring(0, 1))
                &&
                !"L".equalsIgnoreCase(cmd.substring(0, 1))) {
            System.out.println();
            return false;
        }

        int index = 0;
        if ("N".equalsIgnoreCase(cmd.substring(0, 1))) {
            index = 1;
            if (index == cmd.length()
                    || !PATTERN.matcher(cmd.substring(index, index + 1)).matches()) {
                return false;
            }
            while (index < cmd.length()
                    && PATTERN.matcher(cmd.substring(index, index + 1)).matches()) {
                index += 1;
            }
            long seed = Long.parseLong(cmd.substring(1, index));
            if (index == cmd.length() || !"s".equalsIgnoreCase(cmd.substring(index, index + 1))) {
                return false;
            }
            queue.add(Command.newGameCmd(seed));
        } else {
            queue.add(Command.loadGameDefaultCmd());
        }
        index += 1;

        while (index < cmd.length()) {
            String cur = cmd.substring(index, index + 1);
            if ("A".equalsIgnoreCase(cur)) {
                queue.add(Command.moveLeftCmd());
            } else if ("W".equalsIgnoreCase(cur)) {
                queue.add(Command.moveUpCmd());
            } else if ("S".equalsIgnoreCase(cur)) {
                queue.add(Command.moveDownCmd());
            } else if ("D".equalsIgnoreCase(cur)) {
                queue.add(Command.moveRightCmd());
            } else if (":".equals(cur)) {
                index += 1;
                cur = cmd.substring(index, index + 1);
                if ("Q".equalsIgnoreCase(cur)) {
                    queue.add(Command.quitAndSaveDefaultCmd());
                } else {
                    System.out.println("Wrong cmd String: " + cur);
                    return false;
                }
            }
            index += 1;
        }

        return true;
    }


    private Command getOneComand(String cur) {
        if (cur.equalsIgnoreCase("S")) {  // 向下
            queue.add(Command.moveDownCmd());
        } else if (cur.equalsIgnoreCase("w")) { // 上
            queue.add(Command.moveUpCmd());
        } else if (cur.equalsIgnoreCase("a")) { // 左
            queue.add(Command.moveLeftCmd());
        } else if (cur.equalsIgnoreCase("d")) { // 右
            queue.add(Command.moveRightCmd());
        } else if (cur.equalsIgnoreCase("q")) { // 退出
            queue.add(Command.quitCmd());
        } else if (cur.equalsIgnoreCase("t")) { // 开关遮罩
            queue.add(Command.toggleCmd());
        } else if (cur.equalsIgnoreCase("h")) {
            queue.add(Command.helpCmdInGame());
        } else if (cur.equalsIgnoreCase("m")) {
            queue.add(Command.modifyCmdInGame());
        }
        return Command.nullCmd();
    }

    private void pause(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.out.println("Error sleeping");
        }
    }

    /**
     * 主流程，死循环
     */
    public void run() {

        this.execThread.start();

        this.hudThread.start();

        this.timeThread.start();


        while (this.status != ST_QUIT) {
            this.pause(TIME_PAUSE); // 延时
            if (!this.needStdDraw) {
                continue;
            }
            synchronized (this.status) {
                switch (this.status.intValue()) {
                    case ST_AUTO_PLAY: {
                        while (StdDraw.hasNextKeyTyped()) {
                            StdDraw.nextKeyTyped();
                        }
                        break;
                    }
                    case ST_INTERACT: {
                        if (StdDraw.hasNextKeyTyped()) {
                            String s = "" + StdDraw.nextKeyTyped();
                            Command cmd = this.getOneComand(s);
                            if (cmd.isNotNull()) {
                                queue.add(cmd);
                            }
                        }
                        break;
                    }
                    case ST_WAIT: {
                        break;
                    }
                    default:
                }
            }
        }
        if (Main.DEBUG) {
            System.out.println("exit main thread.");
        }
    }

    private void changeTheme(int color) {
        this.theme = color;
        if (2 == this.theme) {
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (board[x][y] == Tileset.AVATAR
                            || board[x][y] == Tileset.FLOOR
                            || board[x][y] == Tileset.LOCKED_DOOR) {
                        continue;
                    }
                    board[x][y] = Tileset.WATER;
                }
            }
        }
    }

    private void doModifyCmd(Long arg) {
        if (1 == arg) { // start menu
            this.showModifyMenu();
            this.showStartMenu();
        } else {  // modify while game is in process
            synchronized (this.status) {
                int originStatus = this.status;
                this.status = Engine.ST_WAIT;
                this.showModifyMenu();
                this.status = originStatus;
                this.showBoard();
            }
        }
    }

    private void doNewCmd(Command cmd) {
        if (cmd.getArg() != 0) {  // with seed
            if (Main.DEBUG) {
                System.out.println("exce: new game with " + cmd.getArg());
            }
            this.newGame(cmd.getArg());
            history.add(cmd);
        } else {  // without input seed, enter one manually
            synchronized (this.status) {
                this.status = Engine.ST_WAIT;
                queue.clear();
                if (this.needStdDraw) {
                    this.showNewMenu();
                }

            }
        }
    }

    private void doLoadCmd(Command cmd) {
        if (cmd.getArg() == 0x3F3F3F3F) {  // default load
            this.loadByString();
        } else if (cmd.getArg() != 0) {  // load specified file
            this.load((int) cmd.getArg());
//                    history.add(cmd);
        } else {  // enter load interface
            synchronized (this.status) {
                this.status = Engine.ST_WAIT;
                queue.clear();
                if (this.needStdDraw) {
                    this.showLoadMenu();
                }

            }
        }
    }

    private void doQuitCmd(Command cmd) {
        if (cmd.getArg() > 0) {
            this.saveAndQuit((int) cmd.getArg());
            history.add(cmd);
        } else if (cmd.getArg() < 0) {
            synchronized (this.status) {
                this.quit();
            }
        } else {  // enter save interface
            synchronized (this.status) {
                this.status = Engine.ST_WAIT;
                queue.clear();
                if (this.needStdDraw) {
                    this.showSaveMenu();
                }

            }
        }
    }

    private void doMoveCmd(Command cmd) {
        synchronized (this.hasMist) {
            this.move((int) cmd.getArg());
            history.add(cmd);
        }
    }

    private void doInitCmd(Command cmd) {
        synchronized (this.status) {
            queue.clear();
            this.status = Engine.ST_WAIT;
            if (this.needStdDraw) {
                this.showStartMenu();
            }

        }
    }

    private void doStatusCmd(Command cmd) {
        synchronized (this.status) {
            this.status = (int) cmd.getArg();
        }
    }

    private void doToggleCmd(Command cmd) {
        synchronized (this.hasMist) {
            this.hasMist = !this.hasMist;
            if (this.needStdDraw) {
                this.showBoard();
            }

        }
    }

    private void doHelpCmd(Command cmd) {
        if (cmd.getArg() == 1) {
            this.showHelpMenu();
            this.showStartMenu();
        } else {
            this.showHelpMenu();
            this.showBoard();
        }
    }


    private void execute() {
        if (queue.isEmpty()) {
            return;
        }
        Command cmd = queue.poll();
        switch (cmd.getCmd()) {
            case Command.CMD_NEW:  // new game
                doNewCmd(cmd);
                break;
            case Command.CMD_LOAD:  // load game
                doLoadCmd(cmd);
                break;
            case Command.CMD_QUIT:  // exit
                doQuitCmd(cmd);
                break;
            case Command.CMD_MOVE:  // move
                doMoveCmd(cmd);
                break;
            case Command.CMD_INIT:
                doInitCmd(cmd);
                break;
            case Command.CMD_STATUS:
                doStatusCmd(cmd);
                break;
            case Command.CMD_TOGGLE:
                doToggleCmd(cmd);
                break;
            case Command.CMD_HELP:
                doHelpCmd(cmd);
                break;
            case Command.CMD_MODIFY:
                doModifyCmd(cmd.getArg());
                break;
            default:
                System.out.println();
        }
    }

    //command entered at the start interface
    private boolean inputCommand() {
        while (true) {
            this.pause(TIME_PAUSE);
            if (StdDraw.hasNextKeyTyped()) {
                String s = "" + StdDraw.nextKeyTyped();

                if ("n".equalsIgnoreCase(s)) {
                    queue.add(Command.newWithoutSeedCmd());
                    return true;
                } else if ("L".equalsIgnoreCase(s)) {
                    queue.add(Command.loadGameCmd(0));
                    return true;
                } else if ("Q".equalsIgnoreCase(s)) {
                    queue.add(Command.quitDefaultCmd());
                    return true;
                } else if ("H".equalsIgnoreCase(s)) {
                    queue.add(Command.helpCmdInStartMenu());
                    return true;
                } else if ("M".equalsIgnoreCase(s)) {
                    queue.add(Command.modifyCmdInStartMenu());
                    return true;
                } else if ("1".equals(s)) {
                    this.theme = 1;
                    //return true;
                } else if ("2".equals(s)) {
                    this.theme = 2;
                    //return true;
                } else if ("3".equals(s)) {
                    this.theme = 3;
                    //return true;
                } else {
                    continue;
                }
            }
        }
    }


    // enter a seed
    private boolean inputNewSeed() {
        String s = "";
        while (true) {
            this.pause(TIME_PAUSE);
            while (StdDraw.hasNextKeyTyped()) {
                s += StdDraw.nextKeyTyped();

                StdDraw.setPenColor(Color.BLACK);
                StdDraw.filledRectangle(WIDTH / 2, TOTAL_HEIGHT * 1 / 3, TOTAL_WIDTH, 10);
                StdDraw.setPenColor(Color.GREEN);
                StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 1 / 3, s);
                StdDraw.show();

                if (s.endsWith("S") || s.endsWith("s")) {
                    s = s.substring(0, s.length() - 1);
                    long seed = Long.parseLong(s);
                    queue.add(Command.newGameCmd(seed));
                    this.status = Engine.ST_INTERACT;
                    return true;
                }
            }
        }
    }


    // move in the specified direction
    private void move(int order) {
        int dx = this.avatarX + DX[order];
        int dy = this.avatarY + DY[order];

        if (0 <= dx && dx < WIDTH && 0 <= dy && dy < HEIGHT) {
            TETile type = this.board[dx][dy];
            if (type == Tileset.FLOOR) {  // move allowed
                this.board[this.avatarX][this.avatarY] = Tileset.FLOOR;
                this.avatarX = dx;
                this.avatarY = dy;
                this.board[this.avatarX][this.avatarY] = Tileset.AVATAR;
                if (!needStdDraw) {
                    return;
                }
                this.showBoard();
                String s = "";
                if (this.status == ST_AUTO_PLAY) {
                    s = "AUTO REPLAY: ";
                }
                this.showHud(s + Engine.MOVE_MESSAGE[order]);
            } else if (type == Tileset.LOCKED_DOOR) {  // victory
                if (Main.DEBUG) {
                    System.out.println("victory");
                }
                this.queue.clear();
                this.showVictoryMenu();
                return;
            } else {  //move denied
                if (!needStdDraw) {
                    return;
                }
                this.showHud("Pong! Ah!");
            }
        }
    }


    private void showBar() {
        StdDraw.setPenColor(Color.BLACK);
        StdDraw.filledRectangle(barX, hudY, barX, hudHalfHeight);
        StdDraw.setFont(Engine.HUD_FONT);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(barX, hudY, "Help(H) Toggle(T) Modify(M)");
        StdDraw.show();
    }

    private void showHelpMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        StdDraw.setFont(Engine.LITTLE_TITLE_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 0.75,
                "H for help menu.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 0.65,
                "T for toggle mist on/off.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 0.55,
                "Q for save and quit.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 0.45,
                "M for modify AVATAR's name.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 0.35,
                "1 for theme of normal style map.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 0.25,
                "2 for theme of water world of future.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 0.15,
                "3 for theme of jungle world of antiquity.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 0.05,
                "Any key to continue...");

        StdDraw.show();

        while (true) {
            pause(TIME_PAUSE);
            String s = "";
            if (StdDraw.hasNextKeyTyped()) {
                s += StdDraw.nextKeyTyped();
                break;
            }
        }
    }

    // HUD display
    private void showHud(String message) {
        // clear hud area
        StdDraw.setPenColor(Color.BLACK);
        StdDraw.filledRectangle(hudX, hudY, hudHalfWidth, hudHalfHeight);
        StdDraw.setFont(Engine.HUD_FONT);
        StdDraw.setPenColor(Color.ORANGE);
        StdDraw.text(hudX, hudY, message);
        StdDraw.show();
    }

    // display load interface
    private void showLoadMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        StdDraw.setFont(Engine.MID_TITLE_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 3 / 4,
                "Please enter a number between 1~5 that you want to load:");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 2 / 3,
                "Only the first digit is effective.");
        StdDraw.show();

        while (true) {
            this.pause(300);
            if (StdDraw.hasNextKeyTyped()) {
                String s = "" + StdDraw.nextKeyTyped();
                if (PATTERN.matcher(s).matches()) {
                    int order = Integer.parseInt(s);
                    if (order < 0 || order > 5) {
                        continue;
                    }

                    // show
                    StdDraw.setPenColor(Color.BLACK);
                    StdDraw.filledRectangle(WIDTH / 2, TOTAL_HEIGHT * 1 / 3, TOTAL_WIDTH, 10);
                    StdDraw.setPenColor(Color.GREEN);
                    StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 1 / 3, s);
                    StdDraw.show();

                    queue.add(Command.loadGameCmd(order));
                    this.status = Engine.ST_AUTO_PLAY;
                    break;
                }
            }
        }
    }

    private void showModifyMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        StdDraw.setFont(Engine.MID_TITLE_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 3 / 4,
                "Please enter a string as your AVATAR's name.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 2 / 3,
                "Your input will be end with a dot symbol.");
        StdDraw.show();

        String s = "";
        while (true) {
            pause(TIME_PAUSE);
            while (StdDraw.hasNextKeyTyped()) {
                s += StdDraw.nextKeyTyped();

                if (Main.DEBUG) {
                    System.out.println(s);
                }

                StdDraw.setPenColor(Color.BLACK);
                StdDraw.filledRectangle(WIDTH / 2, TOTAL_HEIGHT * 1 / 3, TOTAL_WIDTH, 10);
                StdDraw.setPenColor(Color.GREEN);
                StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 1 / 3, s);
                StdDraw.show();

                if (s.endsWith(".")) {
                    this.name = s.substring(0, s.length() - 1);
                    return;
                }
            }
        }
    }

    // display the interface for entering the seed
    private void showNewMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        StdDraw.setFont(Engine.MID_TITLE_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 3 / 4,
                "Please enter a sequence of numbers as random seed.");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 2 / 3,
                "Your input will be end with an 'S' or 's'");
        StdDraw.show();

        while (!inputNewSeed()) {
            // do nothing
        }
    }

    // display the save interface
    private void showSaveMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        StdDraw.setFont(Engine.MID_TITLE_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 3 / 4,
                "Please enter a number between 1~5 that you want to save:");
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 2 / 3,
                "Only the first digit is effective.");
        StdDraw.show();

        while (true) {
            this.pause(TIME_PAUSE);
            if (StdDraw.hasNextKeyTyped()) {
                String s = "" + StdDraw.nextKeyTyped();
                if (PATTERN.matcher(s).matches()) {
                    int order = Integer.parseInt(s);
                    if (order < 0 || order > 5) {
                        continue;
                    }

                    // show
                    StdDraw.setPenColor(Color.BLACK);
                    StdDraw.filledRectangle(WIDTH / 2, TOTAL_HEIGHT * 1 / 3, TOTAL_WIDTH, 10);
                    StdDraw.setPenColor(Color.GREEN);
                    StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 1 / 3, s);
                    StdDraw.show();

                    this.saveAndQuit(order);
                    break;
                }
            }
        }
    }

    // display the start interface
    private void showStartMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(Engine.BIG_TITLE_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 2 / 3, "GAME");

        StdDraw.setFont(Engine.MID_TITLE_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 1 / 3,
                "New Game (N)    Load Game (L)    Quit (Q)");

        StdDraw.setPenColor(Color.CYAN);
        StdDraw.setFont(Engine.LITTLE_TITLE_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 1 / 5,
                "Help(H)  Toggle (T)  Modify Your Name (M)");

        StdDraw.show();

        while (!inputCommand()) {
            //do nothing
        }
    }


    private void showVictoryMenu() {
        this.status = ST_QUIT;
        StdDraw.clear(Color.BLACK);
        int[] x = new int[10];
        int[] y = new int[10];
        for (int i = 0; i < 10; i++) {
            x[i] = y[i] = 0;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int x = 0; x < TOTAL_WIDTH; x++) {
                    for (int y = 0; y < TOTAL_HEIGHT; y++) {
                        board[x][y] = Tileset.NOTHING;
                    }
                }
                while (true) {
                    Engine.this.pause(700L);

                    Random r = new Random(System.currentTimeMillis());
                    int n = r.nextInt(10) + 1;

                    for (int i = 0; i < n; i++) {
                        board[x[i]][y[i]] = Tileset.NOTHING;
                        x[i] = r.nextInt(TOTAL_WIDTH);
                        y[i] = r.nextInt(TOTAL_HEIGHT);
                        board[x[i]][y[i]] = Tileset.FLOWER;
                    }

                    ter.renderFrame(board);

                    StdDraw.setPenColor(Color.WHITE);
                    StdDraw.setFont(Engine.BIG_TITLE_FONT);
                    StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT * 2 / 3, "Congratulations!");
                    StdDraw.show();
                }
            }
        }).start();

        while (true) {
            this.pause(TIME_PAUSE);
            String s = "";
            if (StdDraw.hasNextKeyTyped()) {
                System.exit(0);
            }
        }
    }


    // show the game board
    private synchronized void showBoard() {
        TETile[][] tmp = new TETile[TOTAL_WIDTH][TOTAL_HEIGHT];
        for (int i = 0; i < TOTAL_WIDTH; i++) {
            for (int j = 0; j < TOTAL_HEIGHT; j++) {
                tmp[i][j] = board[i][j];
            }
        }
        if (this.theme == 2) {  // water
            for (int i = 0; i < WIDTH; i++) {
                for (int j = 0; j < HEIGHT; j++) {
                    if (tmp[i][j] == Tileset.AVATAR
                            || tmp[i][j] == Tileset.LOCKED_DOOR
                            || tmp[i][j] == Tileset.FLOOR) {
                        continue;
                    }
                    tmp[i][j] = Tileset.WATER;
                }
            }
        } else if (this.theme == 3) {
            for (int i = 0; i < WIDTH; i++) {
                for (int j = 0; j < HEIGHT; j++) {
                    if (tmp[i][j] == Tileset.AVATAR
                            || tmp[i][j] == Tileset.LOCKED_DOOR
                            || tmp[i][j] == Tileset.FLOOR) {
                        continue;
                    }
                    tmp[i][j] = Tileset.TREE;
                }
            }
        }

        if (this.hasMist) {
            TETile[][] tmp2 = new TETile[TOTAL_WIDTH][TOTAL_HEIGHT];
            for (int i = 0; i < TOTAL_WIDTH; i++) {
                for (int j = 0; j < TOTAL_HEIGHT; j++) {
                    tmp2[i][j] = Tileset.NOTHING;
                }
            }
            // mist，centered at avatar
            for (int x = this.avatarX - Engine.MIST_RADIUS; x <= this.avatarX; x++) {
                if (x < 0 || x >= TOTAL_WIDTH) {
                    continue;
                }

                int cha = Engine.MIST_RADIUS - (this.avatarX - x);
                int starty = this.avatarY - cha;
                int endy = this.avatarY + cha;
                for (int y = starty; y <= endy; y++) {
                    if (y < 0 || y >= HEIGHT) {
                        continue;
                    }
                    tmp2[x][y] = tmp[x][y];
                }
            }
            for (int x = this.avatarX + 1; x <= this.avatarX + Engine.MIST_RADIUS; x++) {
                if (x < 0 || x >= TOTAL_WIDTH) {
                    continue;
                }

                int cha = Engine.MIST_RADIUS + this.avatarX - x;
                int starty = this.avatarY - cha;
                int endy = this.avatarY + cha;
                for (int y = starty; y <= endy; y++) {
                    if (y < 0 || y >= HEIGHT) {
                        continue;
                    }
                    tmp2[x][y] = tmp[x][y];
                }
            }
            tmp = tmp2;
        }

        this.ter.renderFrame(tmp);
        this.showBar();
        this.showTime();
    }

    private void showTime() {
        Date date = new Date(System.currentTimeMillis());
        String s = SDF.format(date);

        StdDraw.setPenColor(Color.BLACK);
        StdDraw.filledRectangle(TOTAL_WIDTH / 2, HEIGHT + HUD_HEIGHT / 2, 10, hudHalfHeight);
        StdDraw.setPenColor(Color.orange);
        StdDraw.setFont(HUD_FONT);
        StdDraw.text(TOTAL_WIDTH / 2, HEIGHT + HUD_HEIGHT / 2, s);
        StdDraw.show();
    }

    /**
     * initialize the game using a seed
     */
    public void newGame(long seed) {
        if (Main.DEBUG) {
            System.out.println("new game with " + seed);
            System.out.println("" + TOTAL_WIDTH + ", " + TOTAL_HEIGHT);
        }
        board = new TETile[TOTAL_WIDTH][TOTAL_HEIGHT];
        for (int x = 0; x < TOTAL_WIDTH; x += 1) {
            for (int y = 0; y < TOTAL_HEIGHT; y += 1) {
                board[x][y] = Tileset.NOTHING;
            }
        }
        Random random = new Random(seed);
        if (Main.DEBUG) {
            System.out.println("random: " + random);
        }
        generateWorld(random, board);
        if (Main.DEBUG) {
            System.out.println("need draw: " + needStdDraw);
        }
        if (!needStdDraw) {
            return;
        }
        ter.initialize(TOTAL_WIDTH, TOTAL_HEIGHT);
        this.showBoard();
        this.showBar();
    }

    public void quit() {
        this.status = ST_QUIT;
        if (this.needStdDraw) {
            System.exit(0);
        }
    }

    /**
     * save the current game status
     */
    public void saveAndQuit(int index) {
        try {
            String fileName = FILE_NAME[index];
            PrintStream ps = new PrintStream(new FileOutputStream(fileName));
            for (Command cmd : history) {
                ps.print(cmd);
            }
            ps.close();
            this.status = ST_QUIT;
            this.pause(TIME_PAUSE + 20L);
            if (needStdDraw) {
                System.exit(0);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void loadByString() {
        try {
            if (Main.DEBUG) {
                System.out.println("load file: " + FILE_NAME[1]);
            }
            Scanner cin = new Scanner(new FileInputStream(FILE_NAME[1]));
            String s = cin.nextLine();
            cin.close();

            ArrayList<Command> list = new ArrayList<>();
            while (!queue.isEmpty()) {
                list.add(queue.poll());
            }
            this.initQueueWithArg(s);
            for (Command cmd : list) {
                queue.add(cmd);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * load game from the specified file
     */
    private void load(int order) {
        try {
            if (Main.DEBUG) {
                System.out.println("load file: " + FILE_NAME[order]);
            }
            Scanner cin = new Scanner(new FileInputStream(FILE_NAME[order]));
            String s = cin.nextLine();
            cin.close();
            this.initQueueWithArg(s);

            this.queue.add(Command.enableInteractCmd());
        } catch (FileNotFoundException e) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.RED);
            StdDraw.text(TOTAL_WIDTH / 2, TOTAL_HEIGHT / 2,
                    "Load error, press any key to the initial menu.");
            StdDraw.show();
            while (true) {
                this.pause(300);
                if (StdDraw.hasNextKeyTyped()) {
                    StdDraw.nextKeyTyped();
                    queue.add(Command.initCmd());
                    break;
                }
            }
        }
    }


}
