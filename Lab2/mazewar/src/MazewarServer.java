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

    private static boolean DEBUG = true;

    public static String hostname;
    public static int port;

    public static HashMap<String, ObjectOutputStream> connectedClients;
    public static ConcurrentLinkedQueue<MazewarPacket> actionQueue;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        connectedClients = new HashMap<String, ObjectOutputStream>();
        actionQueue = new ConcurrentLinkedQueue<MazewarPacket>();

        boolean listening = true;

        try {
            if (args.length == 1) {
                int port = Integer.parseInt(args[0]);
                serverSocket = new ServerSocket(port);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        } catch (Exception e) {
            if (DEBUG) e.printStackTrace();
            System.err.println("ERROR: Invalid arguments!");
            System.exit(-1);
        }

        new MazewarServerBroadcast().start();

        if (DEBUG) System.out.println("Mazewar Server up and running...");

        while (listening) {
            new MazewarServerHandler(serverSocket.accept()).start();
        }

        serverSocket.close();
    }

}
