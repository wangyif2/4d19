import java.io.*;
import java.net.*;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerClient {
    private static boolean isLocalBrokerSet = false;

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

            /* Show start up message */
            System.out.print("Enter command, symbol or x for exit: \n> ");

            stdIn = new BufferedReader(new InputStreamReader(System.in));
            String userInput;
            while ((userInput = stdIn.readLine()) != null && !userInput.toLowerCase().equals("x")) {
                String[] commands = userInput.toLowerCase().split(" ");
                String exchange = null;
                String symbol = null;
                if (commands.length == 1 && isLocalBrokerSet) {
                    symbol = commands[0];

                    /* Send query request packet to server */
                    BrokerPacket packetToServer = new BrokerPacket();

                    packetToServer.symbol = symbol;
                    packetToServer.type = BrokerPacket.BROKER_REQUEST;
                    out.writeObject(packetToServer);

                    /* Read quote packet from server */
                    BrokerPacket packetFromServer;
                    packetFromServer = (BrokerPacket) in.readObject();

                    switch (packetFromServer.type) {
                        case BrokerPacket.BROKER_QUOTE:
                            System.out.println("Quote from broker: " + packetFromServer.quote);
                            break;
                        case BrokerPacket.BROKER_ERROR:
                            System.out.println(symbol.toUpperCase() + " invalid.");
                            break;
                        default:
                            System.out.println("ERROR: Invalid packet type");
                            break;
                    }

                    System.out.print("> ");
                }
                else if (commands.length == 2 && commands[0].equals("local")) {
                    exchange = commands[1];

                    /* Look up broker location using naming service */
                    lookupSocket = new Socket(hostname_lookup, port_lookup);

                    toLookup = new ObjectOutputStream(lookupSocket.getOutputStream());
                    fromLookup = new ObjectInputStream(lookupSocket.getInputStream());

                    /* Send lookup request to naming service */
                    BrokerPacket packetToLookup = new BrokerPacket();
                    packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
                    packetToLookup.exchange = exchange;
                    toLookup.writeObject(packetToLookup);

                    /* Read lookup reply from naming service */
                    BrokerPacket packetFromLookup;
                    packetFromLookup = (BrokerPacket) fromLookup.readObject();
 
                    switch (packetFromLookup.type) {
                        case BrokerPacket.LOOKUP_REPLY:
                            hostname = packetFromLookup.locations[0].broker_host;
                            port = packetFromLookup.locations[0].broker_port;
                            System.out.println(exchange.toUpperCase() + " as local.");
                            
                            toLookup.close();
                            fromLookup.close();
                            lookupSocket.close();
                            break;
                        case BrokerPacket.ERROR_INVALID_EXCHANGE:
                            System.out.print(exchange.toUpperCase() + " does not exist.\n> ");
                            
                            toLookup.close();
                            fromLookup.close();
                            lookupSocket.close();
                            continue;
                        default:
                            System.out.print("ERROR: Invalid packet type\n> ");
                            
                            toLookup.close();
                            fromLookup.close();
                            lookupSocket.close();
                            continue;
                    }
                    isLocalBrokerSet = true;

                    /* Connect to local server to perform query */
                    brokerSocket = new Socket(hostname, port);

                    out = new ObjectOutputStream(brokerSocket.getOutputStream());
                    in = new ObjectInputStream(brokerSocket.getInputStream());

                    System.out.print("> ");
                }
                else {
                    System.out.print("ERROR: Invalid command!\n> ");
                    continue;
                }
            }

        } catch (UnknownHostException e) {
            if (OnlineBroker.DEBUG) e.printStackTrace();
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
        } catch (IOException e) {
            if (OnlineBroker.DEBUG) e.printStackTrace();
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
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
