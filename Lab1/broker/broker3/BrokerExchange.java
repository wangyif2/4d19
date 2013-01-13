import java.io.*;
import java.net.Socket;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerExchange {
    private static final boolean DEBUG = true;
    private static final String EXCHANGE_ADD = "add";
    private static final String EXCHANGE_UPDATE = "update";
    private static final String EXCHANGE_REMOVE = "remove";

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket brokerSocket = null;
        Socket lookupSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        /* variables for hostname/port */
        String hostname_lookup = "localhost";
        String hostname = "localhost";
        int port_lookup = 4444;
        int port = 4445;
        String marketname = "nasdaq";

        try {
            if (args.length == 3) {
                hostname_lookup = args[0];
                port_lookup = Integer.parseInt(args[1]);
                marketname = args[2];
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
            lookupSocket = new Socket(hostname_lookup, port_lookup);

            ObjectOutputStream toLookup = new ObjectOutputStream(lookupSocket.getOutputStream());
            ObjectInputStream fromLookup = new ObjectInputStream(lookupSocket.getInputStream());

            BrokerPacket packetToLookup = new BrokerPacket();
            packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
            packetToLookup.exchange = marketname;
            toLookup.writeObject(packetToLookup);

            BrokerPacket packetFromLookup;
            packetFromLookup = (BrokerPacket) fromLookup.readObject();
 
            switch (packetFromLookup.num_locations) {
                case 1:
                    hostname = packetFromLookup.locations[0].broker_host;
                    port = packetFromLookup.locations[0].broker_port;
                    break;
                default:
                    System.out.println("Broker " + marketname + " not found!");
                    break;
            }

            toLookup.close();
            fromLookup.close();
            lookupSocket.close();

            brokerSocket = new Socket(hostname, port);

            out = new ObjectOutputStream(brokerSocket.getOutputStream());
            in = new ObjectInputStream(brokerSocket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        System.out.print("Enter command or x for exit: \n > ");
        while ((userInput = stdIn.readLine()) != null && !userInput.toLowerCase().contains("x")) {
            BrokerPacket packetToServer = new BrokerPacket();
            packetToServer.exchange = marketname;

            //switch statement parse the first element of the user input
            String[] request = userInput.toLowerCase().split(" ");
            if (request[0].equals(EXCHANGE_ADD)) {
                if (DEBUG) System.out.println(EXCHANGE_ADD + " received");
                packetToServer.type = BrokerPacket.EXCHANGE_ADD;
                packetToServer.symbol = request[1];
            } else if (request[0].equals(EXCHANGE_UPDATE)) {
                packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
                packetToServer.symbol = request[1];
                packetToServer.quote = Long.parseLong(request[2]);
                if (DEBUG) System.out.println(EXCHANGE_UPDATE + " received: " + request[1] + request[2]);
            } else if (request[0].equals(EXCHANGE_REMOVE)) {
                if (DEBUG) System.out.println(EXCHANGE_REMOVE + " received" + packetToServer.toString());
                packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
                packetToServer.symbol = request[1];
            } else {
                if (DEBUG) System.out.println("not recognized");
            }

            out.writeObject(packetToServer);

            BrokerPacket packetFromServer;
            packetFromServer = (BrokerPacket) in.readObject();

            switch (packetFromServer.type) {
                case BrokerPacket.EXCHANGE_REPLY:
                    System.out.println(packetFromServer.symbol);
                    break;
                default:
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
