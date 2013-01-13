import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerLookupHandler extends Thread {
    private Socket socket;
    private Brokers myBrokers;

    public BrokerLookupHandler(Socket socket) {
        this.socket = socket;
        System.out.println("Created new Thread to handle lookup request");
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
                packetToClient.type = BrokerPacket.LOOKUP_REPLY;

                switch (packetFromClient.type) {
                    case BrokerPacket.LOOKUP_REQUEST:
                        packetToClient = lookupBroker(packetFromClient);
                        toClient.writeObject(packetToClient);
                        break;
                    case BrokerPacket.LOOKUP_REGISTER:
                        packetToClient = registerBroker(packetFromClient);
                        toClient.writeObject(packetToClient);
                        break;
                    default:
                        System.out.println("Unknown lookup request");
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

    private BrokerPacket registerBroker(BrokerPacket packetFromClient) throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myBrokers = Brokers.getInstance();
        myBrokers.addBroker(packetFromClient.exchange, packetFromClient.locations[0]);

        packetToClient.type = BrokerPacket.LOOKUP_REPLY;

        return packetToClient;
    }

    private BrokerPacket lookupBroker(BrokerPacket packetFromClient) throws IOException {
        BrokerPacket packetToClient = new BrokerPacket();

        myBrokers = Brokers.getInstance();
        BrokerLocation loc = myBrokers.lookupBrokerLoc(packetFromClient.exchange);

        if (loc == null){
            packetToClient.num_locations = 0;
        }
        else {
            packetToClient.locations = new BrokerLocation[1];
            packetToClient.locations[0] = loc;
            packetToClient.num_locations = 1;
        }

        packetToClient.type = BrokerPacket.LOOKUP_REPLY;

        return packetToClient;
    }

}
