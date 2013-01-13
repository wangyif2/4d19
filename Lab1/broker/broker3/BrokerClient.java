import java.io.*;
import java.net.Socket;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerClient {
    private static final boolean DEBUG = true;
    private static String localBroker = "nasdaq";

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket lookupSocket = null;
        Socket brokerSocket = null;
        ObjectOutputStream toLookup = null;
        ObjectInputStream fromLookup = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        BufferedReader stdIn = null;

        /* variables for hostname/port */
        String hostname_lookup = "localhost";
        String hostname = "localhost";
        int port_lookup = 4444;
        int port = 4445;

        try {

            if (args.length == 2) {
                hostname_lookup = args[0];
                port_lookup = Integer.parseInt(args[1]);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }

            stdIn = new BufferedReader(new InputStreamReader(System.in));
            String userInput;

            System.out.print("Enter command, symbol or x for exit: \n> ");
            while ((userInput = stdIn.readLine()) != null && !userInput.toLowerCase().equals("x")) {
                String[] command = userInput.toLowerCase().split(" ");

                if (command[0].equals("local") && command[1] != null) {
                    lookupSocket = new Socket(hostname_lookup, port_lookup);

                    toLookup = new ObjectOutputStream(lookupSocket.getOutputStream());
                    fromLookup = new ObjectInputStream(lookupSocket.getInputStream());

                    BrokerPacket packetToLookup = new BrokerPacket();
                    packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
                    packetToLookup.exchange = command[1];
                    toLookup.writeObject(packetToLookup);

                    BrokerPacket packetFromLookup;
                    packetFromLookup = (BrokerPacket) fromLookup.readObject();
 
                    switch (packetFromLookup.num_locations) {
                        case 1:
                            hostname = packetFromLookup.locations[0].broker_host;
                            port = packetFromLookup.locations[0].broker_port;
                            break;
                        default:
                            System.out.println("Broker " + command[1] + " not found!");
                            break;
                    }

                    toLookup.close();
                    fromLookup.close();
                    lookupSocket.close();

                    brokerSocket = new Socket(hostname, port);

                    out = new ObjectOutputStream(brokerSocket.getOutputStream());
                    in = new ObjectInputStream(brokerSocket.getInputStream());

                    localBroker = command[1];
                    System.out.println(command[1] + " as local.");
                    System.out.print("> ");
                }
                else {
                    BrokerPacket packetToServer = new BrokerPacket();

                    packetToServer.symbol = userInput.toLowerCase();
                    packetToServer.type = BrokerPacket.BROKER_REQUEST;
                    packetToServer.exchange = localBroker;
                    out.writeObject(packetToServer);

                    BrokerPacket packetFromServer;
                    packetFromServer = (BrokerPacket) in.readObject();

                    switch (packetFromServer.type) {
                        case BrokerPacket.BROKER_QUOTE:
                            System.out.println("Quote from broker: " + packetFromServer.quote);
                            break;
                        default:
                            System.out.println(command[1].toUpperCase() + " invalid.");
                            break;
                    }

                    System.out.print("> ");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
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
