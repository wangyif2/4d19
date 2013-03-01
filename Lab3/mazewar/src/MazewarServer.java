import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;

/**
 * User: Ivan
 * Date: 24/02/13
 */
public class MazewarServer {
    private static final Logger logger = LoggerFactory.getLogger(MazewarServer.class);

    private static int port;
    public static HashMap<String, InetSocketAddress> connectedClients;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        connectedClients = new HashMap<String, InetSocketAddress>();

        boolean listening = true;

        try {
            if (args.length == 1) {
                port = Integer.parseInt(args[0]);
                serverSocket = new ServerSocket(port);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: Invalid arguments!");
            System.exit(-1);
        }

        logger.info("Mazewar Server up and running...\n");

        while (listening) {
            new MazewarServerHandler(serverSocket.accept());
        }

        serverSocket.close();
    }
}
