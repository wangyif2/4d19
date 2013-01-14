import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerLookupHandler extends Thread {
    private Socket socket;
    private BrokerLookup myBrokerLookup;

    public BrokerLookupHandler(Socket socket) {
        this.socket = socket;
        if (OnlineBroker.DEBUG) System.out.println("Created new Thread to handle lookup request");
    }

    @Override
    public void run() {
        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            BrokerPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

            if ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
                /* create a packet to send reply back to client */
                BrokerPacket packetToClient = new BrokerPacket();

                switch (packetFromClient.type) {
                    case BrokerPacket.LOOKUP_REQUEST:
                        if (packetFromClient.exchange == null)
                            packetToClient = lookupBrokers();
                        else
                            packetToClient = lookupSingleBroker(packetFromClient.exchange);
                        toClient.writeObject(packetToClient);
                        break;
                    case BrokerPacket.LOOKUP_REGISTER:
                        packetToClient = registerBroker(packetFromClient);
                        toClient.writeObject(packetToClient);
                        break;
                    default:
                        System.out.println("ERROR: Unknown lookup request.");
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

    private BrokerPacket lookupSingleBroker(String exchange) throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myBrokerLookup = BrokerLookup.getInstance();
        BrokerLocation loc = myBrokerLookup.lookupBrokerLoc(exchange);

        if (loc == null) {
            packetToClient.type = BrokerPacket.ERROR_INVALID_EXCHANGE;
        } else {
            packetToClient.type = BrokerPacket.LOOKUP_REPLY;
            packetToClient.locations = new BrokerLocation[]{loc};
        }

        return packetToClient;
    }

    private BrokerPacket registerBroker(BrokerPacket packetFromClient) throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myBrokerLookup = BrokerLookup.getInstance();
        BrokerLocation loc = myBrokerLookup.lookupBrokerLoc(packetFromClient.exchange);

        if (loc == null) {
            packetToClient.type = BrokerPacket.LOOKUP_REPLY;
            myBrokerLookup.addBroker(packetFromClient.exchange, packetFromClient.locations[0]);

        } else {
            packetToClient.type = BrokerPacket.ERROR_INVALID_EXCHANGE;
        }

        return packetToClient;
    }

    private BrokerPacket lookupBrokers() throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myBrokerLookup = BrokerLookup.getInstance();

        Collection<BrokerLocation> locationCollection = myBrokerLookup.lookupBrokerLoc();
        Iterator iterator = locationCollection.iterator();
        int numBrokers = locationCollection.size();
        BrokerLocation[] locations = new BrokerLocation[numBrokers];

        for(int i = 0; i < locationCollection.size(); i++){
            locations[i] = (BrokerLocation) iterator.next();
        }

        if (locations.length == 0) {
            packetToClient.type = BrokerPacket.ERROR_INVALID_EXCHANGE;
        } else {
            packetToClient.type = BrokerPacket.LOOKUP_REPLY;
            packetToClient.num_locations = locations.length;
            packetToClient.locations = locations;
        }


        return packetToClient;
    }

}
