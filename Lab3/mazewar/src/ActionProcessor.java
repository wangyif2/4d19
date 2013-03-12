import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class ActionProcessor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ActionProcessor.class);

    private Thread thread;

    public ActionProcessor() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        MazewarPacket nextAction;

        // Continuously polling from the action queue
        while (true) {
            if ((nextAction = Mazewar.actionQueue.peek()) != null) {
                // Perform action when all connected clients have acknowledged the action
                if (Mazewar.ackTracker.get(nextAction).equals(Mazewar.connectedClients)) {
                    Mazewar.actionQueue.poll();
                    switch (nextAction.type) {
                        case MazewarPacket.ADD_NOTICE:
                            addClient(nextAction);
                            break;
                        case MazewarPacket.ADD:
                            if (!nextAction.owner.equals(Mazewar.myName)) {
                                /* Add a new remote client */
                                Mazewar.maze.addRemoteClient(nextAction.owner, nextAction.directedPoint, 0);
                                Mazewar.myClient.resume();
                            } else {
                                /* I am the new client */
                                Mazewar.isRegisterComplete = true;
                            }
                            break;
                        case MazewarPacket.MOVE_FORWARD:
                            if (Mazewar.maze.getClientByName(nextAction.owner).forward())
                                logger.info("Moved client: " + nextAction.owner + " forward\n");
                            break;
                        case MazewarPacket.MOVE_BACKWARD:
                            if (Mazewar.maze.getClientByName(nextAction.owner).backup())
                                logger.info("Moved client: " + nextAction.owner + " backward\n");
                            break;
                        case MazewarPacket.TURN_LEFT:
                            Mazewar.maze.getClientByName(nextAction.owner).turnLeft();
                            logger.info("Rotated client: " + nextAction.owner + " left\n");
                            break;
                        case MazewarPacket.TURN_RIGHT:
                            Mazewar.maze.getClientByName(nextAction.owner).turnRight();
                            logger.info("Rotated client: " + nextAction.owner + " right\n");
                            break;
                    }
                }
            }
        }
    }

    private void addClient(MazewarPacket nextAction) {
        String newClient = nextAction.newClient;
        MazewarPacket outgoing;
        if (newClient.equals(Mazewar.myName)) {
            /* I am the new client */
            logger.info("Packet " + nextAction.lamportClk + " from " + nextAction.owner + " is processed\n");
            // Wait until all existing clients have reported their locations
            while (Mazewar.maze.getNumOfClients() < Mazewar.connectedOuts.size()) ;

            // Clear queue
            Mazewar.actionQueue.clear();

            outgoing = new MazewarPacket();
            outgoing.type = MazewarPacket.ADD;
            outgoing.directedPoint = Mazewar.maze.addClient(Mazewar.myClient);

            // Multicast ADD packet to all clients
            Mazewar.multicaster.multicastAction(outgoing);

            // Multicast ACK to all clients
            Mazewar.multicaster.multicastACK(outgoing);
        } else {
            logger.info("Packet " + nextAction.lamportClk + " from " + nextAction.owner + " is processed\n");
            // Suspend user input and random generator
            Mazewar.myClient.pause();

            // Clear queue
            Mazewar.actionQueue.clear();

            // Get my location
            Point p = Mazewar.myClient.getPoint();
            Direction d = Mazewar.myClient.getOrientation();
            DirectedPoint myDp = new DirectedPoint(p, d);

            // Prepare packet to report my location
            outgoing = new MazewarPacket();
            outgoing.type = MazewarPacket.REPORT_LOCATION;
            outgoing.owner = Mazewar.myName;
            outgoing.directedPoint = myDp;
            outgoing.score = Mazewar.myClient.getScore();

            synchronized (Mazewar.connectedOuts) {
                logger.info("Reporting my location to: " + newClient);
                // Report my location to the new guy
                try {
                    Mazewar.connectedOuts.get(newClient).writeObject(outgoing);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
