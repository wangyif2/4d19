import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: Ivan
 * Date: 06/03/13
 */
public class PacketMulticaster {
    private static final Logger logger = LoggerFactory.getLogger(PacketMulticaster.class);

    private Set<String> connectedClients = new HashSet<String>();
    private HashMap<String, ObjectOutputStream> connectedOuts = new HashMap<String, ObjectOutputStream>();
    private HashMap<String, ObjectInputStream> connectedIns = new HashMap<String, ObjectInputStream>();

    public PacketMulticaster(Set<String> connectedClients, HashMap<String,
            ObjectOutputStream> connectedOuts, HashMap<String, ObjectInputStream> connectedIns) {
        this.connectedClients = connectedClients;
        this.connectedOuts = connectedOuts;
        this.connectedIns = connectedIns;
    }

    public void multicastAction(MazewarPacket action) {
        // Set generic fields for action packet
        action.lamportClk = ++Mazewar.lamportClk;
        action.seqNum = action.lamportClk;
        action.owner = Mazewar.myName;

        synchronized (Mazewar.ackTracker) {
            // ACK to myself
            Set<String> receivedACK = new HashSet<String>();
            receivedACK.add(Mazewar.myName);
            Mazewar.ackTracker.put(action, receivedACK);
        }

        // Add action to queue
        Mazewar.actionQueue.add(action);

        // Multicast action packet to all clients
        multicast(action);
        logger.info("Finished sending action packet all clients with seq num: " + action.seqNum);
    }

    public void multicastACK(MazewarPacket action) {
        // Create ack packet and set neccessary fields
        MazewarPacket ack = new MazewarPacket();
        ack.lamportClk = action.lamportClk;
        ack.seqNum = ++Mazewar.lamportClk;
        ack.owner = action.owner;
        ack.ACKer = Mazewar.myName;

        multicast(ack);
        logger.info("Finished sending ACK to all clients with seq num: " + ack.seqNum);
    }

    private void multicast(MazewarPacket outgoing) {
        synchronized (Mazewar.connectedOuts) {
            for (Map.Entry<String, ObjectOutputStream> entry : connectedOuts.entrySet()) {
                try {
                    entry.getValue().writeObject(outgoing);
                    logger.info("Multicast to " + entry.getKey().toUpperCase() + " with seq num: " + outgoing.seqNum);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
