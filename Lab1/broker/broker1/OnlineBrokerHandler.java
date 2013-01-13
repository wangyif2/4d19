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

    public OnlineBrokerHandler(Socket socket) {
        this.socket = socket;
        if (OnlineBroker.DEBUG) System.out.println("Created new Thread to handle BrokerClient request");
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
                        packetToClient.quote = queryBrokerRequest(packetFromClient.symbol);
                        toClient.writeObject(packetToClient);
                        break;
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

    private Long queryBrokerRequest(String symbol) throws IOException {
        return Market.getInstance().lookUpStock(symbol);
    }

}
