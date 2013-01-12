import java.io.IOException;
import java.net.*;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerLookupServer {
    private static boolean DEBUG = true;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            int port = 4444;

            if(args.length == 1) {
                port = Integer.parseInt(args[0]);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }

            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        if (DEBUG) System.out.println("LookupServer up and running...");

        while (listening) {
            new BrokerLookupHandler(serverSocket.accept()).start();
        }

        serverSocket.close();
    }

}
