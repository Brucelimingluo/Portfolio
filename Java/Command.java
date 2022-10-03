package byow.Core;

public class Command {
    private int cmd;  // command itself
    private long arg;  // parameter

    private Command(int cmd, long arg) {
        this.cmd = cmd;
        this.arg = arg;
    }

    public int getCmd() {
        return cmd;
    }
    public long getArg() {
        return arg;
    }

    public boolean isNotNull() {
        return CMD_NULL != this.cmd;
    }

    @Override
    public String toString() {
        switch (this.cmd) {
            case CMD_NEW: return "N" + String.valueOf(this.arg) + "S";
            case CMD_MOVE: return MOVE_LETTER[(int) this.arg];
            case CMD_LOAD: return "L";
            case CMD_QUIT:
                if (this.arg == 1) {
                    return ":Q";
                }
                break;
            default: return "";
        }
        return "";
    }

    // new game command, with seed
    public static Command newGameCmd(long seed) {
        return new Command(CMD_NEW, seed);
    }

    // new game command, without seed
    public static Command newWithoutSeedCmd() {
        return new Command(CMD_NEW, 0);
    }

    public static Command loadGameDefaultCmd() {
        return new Command(CMD_LOAD, 0x3F3F3F3F);
    }

    public static Command loadGameCmd(int order) {
        return new Command(CMD_LOAD, order);
    }

    public static Command quitAndSaveDefaultCmd() {
        return new Command(CMD_QUIT, 1);
    }

    public static Command moveLeftCmd() {
        return new Command(CMD_MOVE, 2);
    }

    public static Command moveRightCmd() {
        return new Command(CMD_MOVE, 3);
    }

    public static Command moveUpCmd() {
        return new Command(CMD_MOVE, 0);
    }

    public static Command moveDownCmd() {
        return new Command(CMD_MOVE, 1);
    }

    public static Command nullCmd() {
        return new Command(CMD_NULL, 0);
    }


    public static Command quitCmd() {
        return new Command(CMD_QUIT, 0);
    }

    // quit without saving
    public static Command quitDefaultCmd() {
        return new Command(CMD_QUIT, -1);
    }

    public static Command toggleCmd() {
        return new Command(CMD_TOGGLE, 0);
    }

    public static Command modifyCmdInStartMenu() {
        return new Command(CMD_MODIFY, 1);
    }

    public static Command modifyCmdInGame() {
        return new Command(CMD_MODIFY, 2);
    }

    // water theme
    public static Command themeWaterCmd() {
        return new Command(CMD_THEME, 2);
    }

    public static Command initCmd() {
        return new Command(CMD_INIT, 0);
    }

    public static Command enableInteractCmd() {
        return new Command(CMD_STATUS, (long) Engine.ST_INTERACT);
    }

    public static Command lightCmd() {
        return new Command(CMD_LIGHT, 0);
    }

    public static Command helpCmdInStartMenu() {
        return new Command(CMD_HELP, 1);
    }

    public static Command helpCmdInGame() {
        return new Command(CMD_HELP, 2);
    }

    public static final int CMD_NULL = 0;

    public static final int CMD_NEW = 10;
    // Load
    public static final int CMD_LOAD = 20;
    // quit
    public static final int CMD_QUIT = 30;
    // move
    public static final int CMD_MOVE = 40;
    // toggle
    public static final int CMD_TOGGLE = 50;

    public static final int CMD_INIT = 60;

    public static final int CMD_STATUS = 70;
    public static final int CMD_LIGHT = 80;

    public static final int CMD_HELP = 90;
    public static final int CMD_MODIFY = 100;

    public static final int CMD_THEME = 110;

    private static final String [] MOVE_LETTER = {"W", "S", "A", "D"};
}
