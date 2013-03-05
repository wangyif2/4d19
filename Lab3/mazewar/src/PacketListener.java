import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class PacketListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PacketListener.class);

    private Thread thread;
    private ObjectInputStream in;

    public PacketListener(ObjectInputStream in) {
        thread = new Thread(this);
        this.in = in;

        thread.start();
    }

    @Override
    public void run() {
        MazewarPacket incoming;
        while (true) {
            try {
                // Listen to new incoming packet
                incoming = (MazewarPacket) in.readObject();

                // Add remote client to maze if the packet is REPORT_LOCATION type
                if (incoming.type == MazewarPacket.REPORT_LOCATION) {
                    Mazewar.maze.addRemoteClient(incoming.owner, incoming.directedPoint, incoming.score);
                    continue;
                }

                /**
                 * For all other types of packets:
                 */
                // Update local lamport clock
                Mazewar.lamportClk = incoming.seqNum > Mazewar.lamportClk ? incoming.seqNum : Mazewar.lamportClk;

                if (incoming.ACKer == null) {
                    /* Action packet*/
                    logger.info("Received packet from: " + incoming.owner.toUpperCase() +
                            " with lamport clock " + incoming.lamportClk);

                    // Make sure all connection has been established when receiving a ADD_NOTICE packet
                    if (incoming.type == MazewarPacket.ADD_NOTICE)
                        while (!Mazewar.connectedClients.contains(incoming.newClient) || !Mazewar.allConnected) ;

                    synchronized (Mazewar.ackTracker) {
                        // ACK to myself
                        Set<String> receivedACK = new HashSet<String>();
                        receivedACK.add(Mazewar.myName);
                        Mazewar.ackTracker.put(incoming, receivedACK);
                    }

                    // Add action to queue
                    Mazewar.actionQueue.add(incoming);

                    // Multicast ACK to all clients
                    Mazewar.multicaster.multicastACK(incoming);
                } else {
                    /* ACK packet*/
                    // Make sure the packet has arrived before receiving the ACK
                    while (!Mazewar.ackTracker.containsKey(incoming)) ;

                    // Add ACK to ackTracker
                    Mazewar.ackTracker.get(incoming).add(incoming.ACKer);
                    logger.info("Received ACK  from: " + incoming.ACKer.toUpperCase() + " for packet: " + incoming.lamportClk +
                            " with seq num " + incoming.seqNum);
                }
                logger.info("Lamport clk is updated to: " + Mazewar.lamportClk + "\n");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Stream corrupted!");
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}
