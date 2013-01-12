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
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            /* variables for hostname/port */
            String hostname = "localhost";
            int port = 4444;
            String marketname = "tse";

            if (args.length == 3) {
                hostname = args[0];
                port = Integer.parseInt(args[1]);
                marketname = args[3];
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
            brokerSocket = new Socket(hostname, port);


            out = new ObjectOutputStream(brokerSocket.getOutputStream());
            in = new ObjectInputStream(brokerSocket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        System.out.print("Enter command or x for exit: \n > ");
        while ((userInput = stdIn.readLine()) != null && !userInput.toLowerCase().contains("x")) {
            BrokerPacket packetToServer = new BrokerPacket();

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
