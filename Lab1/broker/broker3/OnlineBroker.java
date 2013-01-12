import java.io.*;
import java.net.*;

/**
 * User: robert
 * Date: 11/01/13
 */
public class OnlineBroker {
    public static boolean DEBUG = true;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        Socket lookupSocket = null;
        boolean listening = true;

        try {
            String hostname_lookup = "localhost";
            int port_lookup = 4444;
            int port = 4445;
            String myName = "nasdaq";

            if(args.length == 4) {
                hostname_lookup = args[0];
                port_lookup = Integer.parseInt(args[1]);
                port = Integer.parseInt(args[2]);
                myName = args[3];
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }

            lookupSocket = new Socket(hostname_lookup, port_lookup);

            ObjectInputStream fromLookup = new ObjectInputStream(lookupSocket.getInputStream());
            ObjectOutputStream toLookup = new ObjectOutputStream(lookupSocket.getOutputStream());

            BrokerPacket packetToLookup = new BrokerPacket();
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            BrokerLocation loc = new BrokerLocation(hostname, port);
            packetToLookup.type = BrokerPacket.LOOKUP_REGISTER;
            packetToLookup.exchange = myName;
            packetToLookup.locations[0] = loc;
            toLookup.writeObject(packetToLookup);

            toLookup.close();
            fromLookup.close();
            lookupSocket.close();

            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        if (DEBUG) System.out.println("Server up and running...");

        while (listening) {
            new OnlineBrokerHandler(serverSocket.accept()).start();
        }

        serverSocket.close();
    }

}
