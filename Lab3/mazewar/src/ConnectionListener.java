import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class ConnectionListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MazewarServerHandler.class);

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
                Mazewar.connectedIns.put(incoming.owner, in);
                Mazewar.connectedOuts.put(incoming.owner, out);
                Mazewar.connectedClients.add(incoming.owner);
                logger.info("Received connection request!\n");

                // Spawn new packet listener
                new PacketListener(out, in);

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
        outgoing.lamportClk = ++Mazewar.lamportClk;
        outgoing.seqNum = outgoing.lamportClk;
        outgoing.owner = Mazewar.myName;
        outgoing.newClient = newClient;

        // ACK to myself
        Set<String> receivedACK = new HashSet<String>();
        receivedACK.add(Mazewar.myName);
        Mazewar.ackTracker.put(outgoing, receivedACK);

        // Add ADD_NOTICE to queue
        Mazewar.actionQueue.add(outgoing);

        // Multicast ADD_NOTICE to all clients
        multicastAddNotice(outgoing);
        logger.info("Lamport clk is updated to: " + Mazewar.lamportClk);

        // Multicast the ACK to all clients
        multicastACK(outgoing);
        logger.info("Lamport clk is updated to: " + Mazewar.lamportClk + "\n");
    }

    private void multicastAddNotice(MazewarPacket outgoing) {
        synchronized (Mazewar.connectedOuts) {
            for (Map.Entry<String, ObjectOutputStream> entry : Mazewar.connectedOuts.entrySet()) {
                try {
                    (entry.getValue()).writeObject(outgoing);
                    logger.info("Sent " + outgoing.newClient.toUpperCase() + "'s add notice to " + entry.getKey().toUpperCase());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void multicastACK(MazewarPacket outgoing) {
        MazewarPacket ack = new MazewarPacket();
        ack.lamportClk = outgoing.lamportClk;
        ack.owner = outgoing.owner;
        ack.ACKer = Mazewar.myName;
        ack.seqNum = ++Mazewar.lamportClk;

        synchronized (Mazewar.connectedOuts) {
            for (Map.Entry<String, ObjectOutputStream> entry : Mazewar.connectedOuts.entrySet()) {
                try {
                    entry.getValue().writeObject(ack);
                    logger.info("Sent ACK to " + entry.getKey().toUpperCase() + " with seq num: " + ack.seqNum);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
