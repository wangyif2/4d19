import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * User: Ivan
 * Date: 24/02/13
 */
public class MazewarPacket extends MazewarPacketIdentifier implements Serializable, Comparable<MazewarPacket> {
    public static final int NULL = 0;
    public static final int ERROR_DUPLICATED_CLIENT = 1;
    public static final int ERROR_DUPLICATED_LOCATION = 2;

    public static final int REGISTER = 100;
    public static final int REGISTER_SUCCESS = 101;

    public static final int ADD_NOTICE = 200;
    public static final int ADD = 201;
    public static final int ADD_SUCCESS = 202;

    public static final int MOVE_FORWARD = 200;
    public static final int MOVE_BACKWARD = 202;

    public static final int TURN_LEFT = 203;
    public static final int TURN_RIGHT = 204;

    public static final int FIRE = 300;
    public static final int INSTANT_KILL = 301;
    public static final int KILL = 302;

    public static final int QUIT = 400;

    //packet definitions
    public String ACKer;
    public int seqNum;
    public String newClient;
    public String victim;

    public InetSocketAddress address;
    public HashMap<String, InetSocketAddress> connectedClients;

    public int type = MazewarPacket.NULL;
    public HashMap<String, DirectedPoint> mazeMap = new HashMap<String, DirectedPoint>();
    public HashMap<String, Integer> mazeScore = new HashMap<String, Integer>();

    @Override
    public int compareTo(MazewarPacket o) {
        if (this.seqNum == o.seqNum)
            return this.owner.compareTo(o.owner);
        else
            return this.seqNum > o.seqNum ? 1 : -1;
    }
}
