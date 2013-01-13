import java.io.*;
import java.net.*;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerExchange {
    private static final boolean DEBUG = false;
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

        System.out.print("Enter command or x for exit: \n> ");
        while ((userInput = stdIn.readLine()) != null && !userInput.toLowerCase().contains("x")) {
            String[] request = userInput.toLowerCase().split(" ");
            String command = null;
            String symbol = null;
            String quote = null;

            if (request.length == 2) {
                command = request[0];
                symbol = request[1];
            }
            else if (request.length == 3) {
                command = request[0];
                symbol = request[1];
                quote = request[2];
            }
            else {
                System.out.print("ERROR: Invalid command!\n> ");
                continue;
            }

            /* Send exchange request packet to server */
            BrokerPacket packetToServer = new BrokerPacket();

            /* switch statement parse the first element of the user input */
            if (command.equals(EXCHANGE_ADD)) {
                if (DEBUG) System.out.println(EXCHANGE_ADD + " received: " + symbol);
                packetToServer.type = BrokerPacket.EXCHANGE_ADD;
                packetToServer.symbol = symbol;
            } else if (command.equals(EXCHANGE_UPDATE)) {
                if (DEBUG) System.out.println(EXCHANGE_UPDATE + " received: " + symbol + quote);
                packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
                packetToServer.symbol = symbol;
                packetToServer.quote = Long.parseLong(quote);
            } else if (command.equals(EXCHANGE_REMOVE)) {
                if (DEBUG) System.out.println(EXCHANGE_REMOVE + " received: " + symbol);
                packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
                packetToServer.symbol = symbol;
            } else {
                System.out.print("ERROR: Invalid command!\n> ");
                continue;
            }

            out.writeObject(packetToServer);

            /* Read quote packet from server */
            BrokerPacket packetFromServer;
            packetFromServer = (BrokerPacket) in.readObject();

            switch (packetFromServer.type) {
                case BrokerPacket.EXCHANGE_REPLY:
                    System.out.println(packetFromServer.symbol);
                    break;
                case BrokerPacket.ERROR_INVALID_SYMBOL:
                    System.out.println(symbol.toUpperCase() + " invalid.");
                    break;
                case BrokerPacket.ERROR_OUT_OF_RANGE:
                    System.out.println(symbol.toUpperCase() + " out of range.");
                    break;
                case BrokerPacket.ERROR_SYMBOL_EXISTS:
                    System.out.println(symbol.toUpperCase() + " exists.");
                    break;
                default:
                    System.out.println("ERROR: Invalid packet type.");
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
