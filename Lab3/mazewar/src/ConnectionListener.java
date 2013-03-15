import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class ConnectionListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionListener.class);

    private Thread thread;
    private boolean listening = true;
    private ServerSocket listeningSocket;

    public ConnectionListener(int port) {
        thread = new Thread(this);

        try {
            listeningSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        thread.start();
    }

    @Override
    public void run() {
        while (listening) {
            try {
                // Accept new connections
                Socket newSocket = listeningSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(newSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(newSocket.getInputStream());

                // Add client name and in/out stream
                MazewarPacket incoming = (MazewarPacket) in.readObject();
                synchronized (Mazewar.connectedOuts) {
                    Mazewar.connectedIns.put(incoming.owner, in);
                    Mazewar.connectedOuts.put(incoming.owner, out);
                    Mazewar.connectedClients.add(incoming.owner);
                    logger.info("Received connection request from " + incoming.owner + "!\n");
                }

                // Spawn new packet listener
                new PacketListener(incoming.owner, newSocket, in);

                // Multicast an Add_NOTICE to all clients
                notifyClientAddition(incoming.owner);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyClientAddition(String newClient) {
        MazewarPacket outgoing = new MazewarPacket();
        outgoing.type = MazewarPacket.ADD_NOTICE;
        outgoing.newClient = newClient;

        // Multicast ADD_NOTICE to all clients
        Mazewar.multicaster.multicastAction(outgoing);

        // Multicast the ACK to all clients
        Mazewar.multicaster.multicastACK(outgoing);
    }
}
