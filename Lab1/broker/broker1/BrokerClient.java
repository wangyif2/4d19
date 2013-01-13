import java.io.*;
import java.net.*;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerClient {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket brokerSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            /* variables for hostname/port */
            String hostname = "localhost";
            int port = 4444;

            if (args.length == 2) {
                hostname = args[0];
                port = Integer.parseInt(args[1]);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }

            /* Connect to server */
            brokerSocket = new Socket(hostname, port);

            out = new ObjectOutputStream(brokerSocket.getOutputStream());
            in = new ObjectInputStream(brokerSocket.getInputStream());

        } catch (UnknownHostException e) {
            if (OnlineBroker.DEBUG) e.printStackTrace();
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
        } catch (IOException e) {
            if (OnlineBroker.DEBUG) e.printStackTrace();
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        System.out.print("Enter queries or x for exit: \n> ");
        while ((userInput = stdIn.readLine()) != null && !userInput.toLowerCase().contains("x")) {
            /* Send query request packet to server */
            BrokerPacket packetToServer = new BrokerPacket();

            packetToServer.symbol = userInput.toLowerCase();
            packetToServer.type = BrokerPacket.BROKER_REQUEST;
            out.writeObject(packetToServer);

            /* Read quote packet from server */
            BrokerPacket packetFromServer;
            packetFromServer = (BrokerPacket) in.readObject();

            switch (packetFromServer.type) {
                case BrokerPacket.BROKER_QUOTE:
                    System.out.println("Quote from broker: " + packetFromServer.quote);
                    break;
                default:
                    System.out.println("Quote from broker: 0");
                    break;
            }

            System.out.print("> ");
        }

        /* tell server that i'm quitting */
        BrokerPacket packetToServer = new BrokerPacket();
        packetToServer.type = BrokerPacket.BROKER_BYE;
        out.writeObject(packetToServer);

        out.close();
        in.close();
        stdIn.close();
        brokerSocket.close();
    }
}
