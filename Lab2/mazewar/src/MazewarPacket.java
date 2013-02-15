import java.io.Serializable;

/**
 * User: Ivan
 * Date: 31/01/13
 */
public class MazewarPacket implements Serializable {
    public static final int NULL = 0;

    public static final int REGISTER = 100;

    public static final int MOVE_FORWARD = 101;
    public static final int MOVE_BACKWARD = 102;
    public static final int TURN_LEFT = 103;
    public static final int TURN_RIGHT = 104;

    public static final int FIRE = 105;
    public static final int QUIT = 106;

    public static final int KILLED = 107;

    public int type = MazewarPacket.NULL;

    public String owner;
}
