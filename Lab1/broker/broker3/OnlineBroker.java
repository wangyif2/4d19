import java.io.*;
import java.net.*;

/**
 * User: robert
 * Date: 11/01/13
 */
public class OnlineBroker {
    public static boolean DEBUG = false;
    public static String hostname_lookup;
    public static int port_lookup;
    public static String hostname;
    public static int port;
    public static String myName;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        Socket lookupSocket = null;
        boolean listening = true;

        try {
            if(args.length == 4) {
                hostname_lookup = args[0];
                port_lookup = Integer.parseInt(args[1]);
                hostname = InetAddress.getLocalHost().getHostName();
                port = Integer.parseInt(args[2]);
                myName = args[3];
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }

            lookupSocket = new Socket(hostname_lookup, port_lookup);

            ObjectOutputStream toLookup = new ObjectOutputStream(lookupSocket.getOutputStream());
            ObjectInputStream fromLookup = new ObjectInputStream(lookupSocket.getInputStream());

            BrokerLocation loc = new BrokerLocation(hostname, port);
            /* Send register request to naming service */
            BrokerPacket packetToLookup = new BrokerPacket();
            packetToLookup.type = BrokerPacket.LOOKUP_REGISTER;
            packetToLookup.exchange = myName;
            packetToLookup.locations = new BrokerLocation[]{loc};
            toLookup.writeObject(packetToLookup);
            
            /* Read register reply from naming service */
            BrokerPacket packetFromLookup = (BrokerPacket) fromLookup.readObject();

            switch (packetFromLookup.type) {
                case BrokerPacket.LOOKUP_REPLY:
                    break;
                case BrokerPacket.ERROR_INVALID_EXCHANGE:
                    System.out.println("ERROR: " + myName + " already exists!");
                    
                    toLookup.close();
                    fromLookup.close();
                    lookupSocket.close();
                    System.exit(-1);
                default:
                    System.out.println("ERROR: Invalid packet!");
                    
                    toLookup.close();
                    fromLookup.close();
                    lookupSocket.close();
                    System.exit(-1);
            }

            serverSocket = new ServerSocket(port);
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
