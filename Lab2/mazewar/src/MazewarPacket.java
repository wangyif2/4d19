import java.io.Serializable;
import java.util.HashMap;

/**
 * User: robert
 * Date: 17/02/13
 */
public class MazewarPacket implements Serializable {
    public static final int NULL = 0;
    public static final int ERROR_DUPLICATED_CLIENT = 1;
    public static final int ERROR_DUPLICATED_LOCATION = 1;

    public static final int REGISTER = 100;
    public static final int REGISTER_SUCCESS = 101;

    public static final int ADD = 103;
    public static final int ADD_SUCCESS = 104;

    public static final int MOVE = 105;

    public static final int TURN_LEFT = 106;
    public static final int TURN_RIGHT = 107;

    public static final int FIRE = 200;
    public static final int KILLED = 201;

    public static final int QUIT = 300;
    //packet definitions
    public String owner;

    public int type = MazewarPacket.NULL;
    public HashMap<String, DirectedPoint> mazeMap = new HashMap<String, DirectedPoint>();
}
