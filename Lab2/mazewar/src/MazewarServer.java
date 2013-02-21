import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: Ivan
 * Date: 30/01/13
 */
public class MazewarServer {

    public static String hostname;
    public static int port;

    /**
     * {@link Client} gets eleven points for a kill.
     */
    public static final int scoreAdjKill = 11;

    /**
     * {@link Client} loses one point per shot.
     */
    public static final int scoreAdjFire = -1;

    /**
     * {@link Client} loses five points if killed.
     */
    public static final int scoreAdjKilled = -5;


    public static HashMap<String, ObjectOutputStream> connectedClients;
    public static HashMap<String, DirectedPoint> mazeMap;
    public static HashMap<String, Integer> mazeScore;
    public static ConcurrentLinkedQueue<MazewarPacket> actionQueue;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        connectedClients = new HashMap<String, ObjectOutputStream>();
        mazeMap = new HashMap<String, DirectedPoint>();
        mazeScore = new HashMap<String, Integer>();
        actionQueue = new ConcurrentLinkedQueue<MazewarPacket>();

        boolean listening = true;

        try {
            if (args.length == 1) {
                int port = Integer.parseInt(args[0]);
                serverSocket = new ServerSocket(port);
            } else {
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        new MazewarServerBroadcast().start();

        while (listening) {
            new MazewarServerHandler(serverSocket.accept()).start();
        }

        serverSocket.close();
    }

}