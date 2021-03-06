import java.io.IOException;
import java.net.ServerSocket;

/**
 * User: robert
 * Date: 11/01/13
 */
public class OnlineBroker {
    public static final String MKT_NAME= "nasdaq";
    public static boolean DEBUG = false;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            if(args.length == 1) {
                int port = Integer.parseInt(args[0]);
                serverSocket = new ServerSocket(port);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            if (OnlineBroker.DEBUG) e.printStackTrace();
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        } catch (Exception e) {
            if (OnlineBroker.DEBUG) e.printStackTrace();
            System.err.println("ERROR: Invalid arguments!");
            System.exit(-1);
        }

        if (DEBUG) System.out.println("Server up and running...");

        while (listening) {
            new OnlineBrokerHandler(serverSocket.accept()).start();
        }

        serverSocket.close();
    }

}
