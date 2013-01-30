import java.io.Serializable;

/**
 * User: Ivan
 * Date: 31/01/13
 */
public class MazewarPacket implements Serializable {
    public static final int DIR_NULL = 0;
    public static final int DIR_FORWARD = 100;
    public static final int DIR_BACKWARD = 101;
    public static final int DIR_LEFT = 102;
    public static final int DIR_RIGHT = 103;

    public static final int FIRE = 104;
    public static final int QUIT = 999;

    public static final int ERROR_INVALID_SYMBOL = -101;
    public static final int ERROR_OUT_OF_RANGE = -102;
    public static final int ERROR_SYMBOL_EXISTS = -103;
    public static final int ERROR_INVALID_EXCHANGE = -104;

    public int type = MazewarPacket.DIR_NULL;

    public String message;
}
