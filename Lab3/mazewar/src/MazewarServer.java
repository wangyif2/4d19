import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: Ivan
 * Date: 24/02/13
 */
public class MazewarServer {
    private static final Logger logger = LoggerFactory.getLogger(MazewarServer.class);

    public static String hostname;
    public static int port;

    /**
     * {@link Client} gets eleven points for a kill.
     */
    public static final int scoreAdjKill = 11;

    /**
     * {@link Client} gets ten points for a instant kill (11 for kill and -1 for fire).
     */
    public static final int scoreAdjInstKill = 10;

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
                logger.error("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("ERROR: Could not listen on port!");
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ERROR: Invalid arguments!");
            System.exit(-1);
        }

        new MazewarServerBroadcast().start();

        logger.info("Mazewar Server up and running...\n");

        while (listening) {
            new MazewarServerHandler(serverSocket.accept()).start();
        }

        serverSocket.close();
    }
}
