import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * User: robert
 * Date: 11/01/13
 */
public class OnlineBrokerHandler extends Thread {
    private Socket socket;
    private Market myMarket;

    public OnlineBrokerHandler(Socket socket) {
        this.socket = socket;
        System.out.println("Created new Thread to handle BrokerClient request");
    }

    @Override
    public void run() {
        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            BrokerPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

            while ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
                if (BrokerPacket.BROKER_BYE == packetFromClient.type)
                    break;

                /* create a packet to send reply back to client */
                BrokerPacket packetToClient = new BrokerPacket();
                packetToClient.type = BrokerPacket.BROKER_QUOTE;

                switch (packetFromClient.type) {
                    case BrokerPacket.BROKER_REQUEST:
                        packetToClient = queryBrokerRequest(packetFromClient.symbol);
                        toClient.writeObject(packetToClient);
                        break;
                    case BrokerPacket.EXCHANGE_ADD:
                        packetToClient = addBrokerRequest(packetFromClient.symbol);
                        toClient.writeObject(packetToClient);
                        break;
                    case BrokerPacket.EXCHANGE_REMOVE:
                        packetToClient = removeBrokerRequest(packetFromClient.symbol);
                        toClient.writeObject(packetToClient);
                        break;
                    case BrokerPacket.EXCHANGE_UPDATE:
                        packetToClient = updateBrokerRequest(packetFromClient.symbol, packetFromClient.quote);
                        toClient.writeObject(packetToClient);
                        break;
                    case BrokerPacket.BROKER_FORWARD:
                        packetToClient = queryLocalBrokerRequest(packetFromClient.symbol);
                        toClient.writeObject(packetToClient);
                    default:
                        System.out.println("Unknown Request from client");
                        break;
                }
            }

            /* cleanup when client exits */
            fromClient.close();
            toClient.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private BrokerPacket queryLocalBrokerRequest(String symbol) throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myMarket = Market.getInstance(OnlineBroker.myName);

        if (myMarket.lookUpStock(symbol) == null) {
            packetToClient.symbol = symbol.toUpperCase() + " invalid.";
        } else {
            packetToClient.symbol = symbol;
            packetToClient.quote = myMarket.lookUpStock(symbol);
            packetToClient.type = BrokerPacket.BROKER_QUOTE;
        }

        return packetToClient;
    }

    private BrokerPacket updateBrokerRequest(String symbol, Long quote) throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myMarket = Market.getInstance(OnlineBroker.myName);

        if (myMarket.lookUpStock(symbol) != null) {
            myMarket.updateStock(OnlineBroker.myName, symbol, quote);
            packetToClient.symbol = symbol.toUpperCase() + " updated to " + quote.toString() + '.';
        } else {
            packetToClient.symbol = symbol.toUpperCase() + " invalid.";
        }

        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;

        return packetToClient;
    }

    private BrokerPacket removeBrokerRequest(String symbol) throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myMarket = Market.getInstance(OnlineBroker.myName);

        if (myMarket.lookUpStock(symbol) != null) {
            myMarket.removeStock(OnlineBroker.myName, symbol);
            packetToClient.symbol = symbol.toUpperCase() + " removed.";
        } else {
            packetToClient.symbol = symbol.toUpperCase() + " invalid.";
        }

        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;

        return packetToClient;
    }

    private BrokerPacket addBrokerRequest(String symbol) throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myMarket = Market.getInstance(OnlineBroker.myName);

        if (myMarket.lookUpStock(symbol) == null) {
            myMarket.addStock(OnlineBroker.myName, symbol);
            packetToClient.symbol = symbol.toUpperCase() + " added.";
        } else {
            packetToClient.symbol = symbol.toUpperCase() + " exists.";
        }

        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;

        return packetToClient;
    }

    private BrokerPacket queryBrokerRequest(String symbol) throws IOException, ClassNotFoundException {
        BrokerPacket packetToClient = new BrokerPacket();

        myMarket = Market.getInstance(OnlineBroker.myName);

        if (myMarket.lookUpStock(symbol) == null) {
            packetToClient = forwardQueryBrokerRequest(symbol);

            if (packetToClient.quote == null)
                packetToClient.symbol = symbol.toUpperCase() + " invalid.";
        } else {
            packetToClient.symbol = symbol;
            packetToClient.quote = myMarket.lookUpStock(symbol);
            packetToClient.type = BrokerPacket.BROKER_QUOTE;
        }

        return packetToClient;
    }

    private BrokerPacket forwardQueryBrokerRequest(String symbol) throws IOException, ClassNotFoundException {
        Socket lookupSocket;
        BrokerPacket packetToLookup, packetFromLookup, packetToBroker, packetFromBroker;
        BrokerPacket packetToClient = new BrokerPacket();

        lookupSocket = new Socket(OnlineBroker.hostname_lookup, OnlineBroker.port_lookup);

        ObjectOutputStream toLookup = new ObjectOutputStream(lookupSocket.getOutputStream());
        ObjectInputStream fromLookup = new ObjectInputStream(lookupSocket.getInputStream());

        packetToLookup = new BrokerPacket();
        packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
        toLookup.writeObject(packetToLookup);

        packetFromLookup = (BrokerPacket) fromLookup.readObject();

        toLookup.close();
        fromLookup.close();
        lookupSocket.close();

        // iterate through brokers to get quote
        int numBrokers = packetFromLookup.num_locations;
        BrokerLocation[] brokerLocations = packetFromLookup.locations;

        for (int i = 0; i < numBrokers; i++) {
            if (brokerLocations[i].broker_port == OnlineBroker.port && brokerLocations[i].broker_host.equals(OnlineBroker.hostname))
                continue;

            Socket brokerSocket = new Socket(brokerLocations[i].broker_host, brokerLocations[i].broker_port);

            ObjectOutputStream toBroker = new ObjectOutputStream(brokerSocket.getOutputStream());
            ObjectInputStream fromBroker = new ObjectInputStream(brokerSocket.getInputStream());

            packetToBroker = new BrokerPacket();
            packetToBroker.type = BrokerPacket.BROKER_FORWARD;
            packetToBroker.symbol = symbol;

            toBroker.writeObject(packetToBroker);

            packetFromBroker = (BrokerPacket) fromBroker.readObject();

            if (packetFromBroker.quote != null) {
                toBroker.close();
                fromBroker.close();
                brokerSocket.close();
                packetToClient = packetFromBroker;
                break;
            }

            toBroker.close();
            fromBroker.close();
            brokerSocket.close();
        }

        return packetToClient;
    }

}
