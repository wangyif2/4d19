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
                    Mazewar.ackTracker.remove(nextAction);
                    switch (nextAction.type) {
                        case MazewarPacket.ADD_NOTICE:
                            if (nextAction.newClient.equals(Mazewar.myName))
                                Mazewar.myClient.notifyAdd();
                            else
                                reportLocation(nextAction.newClient, Mazewar.myClient);
                            logger.info("Add notice from " + nextAction.owner + " with clk " + nextAction.lamportClk + " is processed!\n");
                            break;
                        case MazewarPacket.ADD:
                            if (nextAction.owner.equals(Mazewar.myName)) {
                                /* I am the new client */
                                Mazewar.isRegisterComplete = true;
                                logger.info("Registration completed and game STARTS!\n");
                            } else {
                                /* Add a new remote client */
                                Mazewar.maze.addRemoteClient(nextAction.owner, nextAction.directedPoint, 0);
                                Mazewar.myClient.resume();
                                logger.info("Added " + nextAction.owner +
                                        "\n\t@ X: " + nextAction.directedPoint.getX() + " Y: " + nextAction.directedPoint.getY() +
                                        "\n\tfacing " + nextAction.directedPoint.getDirection() +
                                        "\n\twith clk " + nextAction.lamportClk + " is processed!\n");
                            }
                            break;
                        case MazewarPacket.MOVE_FORWARD:
                            if (Mazewar.maze.getClientByName(nextAction.owner).forward())
                                logger.info("Moved client: " + nextAction.owner + " forward with clk " + nextAction.lamportClk + "\n");
                            break;
                        case MazewarPacket.MOVE_BACKWARD:
                            if (Mazewar.maze.getClientByName(nextAction.owner).backup())
                                logger.info("Moved client: " + nextAction.owner + " backward with clk " + nextAction.lamportClk + "\n");
                            break;
                        case MazewarPacket.TURN_LEFT:
                            Mazewar.maze.getClientByName(nextAction.owner).turnLeft();
                            logger.info("Rotated client: " + nextAction.owner + " left with clk " + nextAction.lamportClk + "\n");
                            break;
                        case MazewarPacket.TURN_RIGHT:
                            Mazewar.maze.getClientByName(nextAction.owner).turnRight();
                            logger.info("Rotated client: " + nextAction.owner + " right with clk " + nextAction.lamportClk + "\n");
                            break;
                        case MazewarPacket.FIRE:
                            if (Mazewar.maze.getClientByName(nextAction.owner).fire())
                                logger.info("Client: " + nextAction.owner + " fired with clk " + nextAction.lamportClk + "\n");
                            break;
                        case MazewarPacket.INSTANT_KILL:
                            Mazewar.maze.getClientByName(nextAction.owner).kill(nextAction.victim, nextAction.directedPoint, true);
                            logger.info(nextAction.owner + " instantly killed client: " + nextAction.victim +
                                    "\n\t reSpawning at location " + nextAction.directedPoint.getX() + " " + nextAction.directedPoint.getY() + " " +
                                    nextAction.directedPoint.getDirection() + " with clk " + nextAction.lamportClk + "\n");
                            break;
                        case MazewarPacket.KILL:
                            Mazewar.maze.getClientByName(nextAction.owner).kill(nextAction.victim, nextAction.directedPoint, false);
                            logger.info(nextAction.owner + " killed client: " + nextAction.victim +
                                    "\n\t reSpawning at location " + nextAction.directedPoint.getX() + " " + nextAction.directedPoint.getY() + " " +
                                    nextAction.directedPoint.getDirection() + " with clk " + nextAction.lamportClk + "\n");
                            break;
                        case MazewarPacket.QUIT:
                            Mazewar.maze.getClientByName(nextAction.owner).quit();
                            logger.info("Client: " + nextAction.owner + " quiting\n");
                            break;
                    }
                }
            }
        }
    }

    private void reportLocation(String newClient, LocalClient me) {
        // Suspend user input and random generator
        me.pause();

        // Clear queue
        Mazewar.actionQueue.clear();
        Mazewar.ackTracker.clear();

        // Get my location
        DirectedPoint myDp = new DirectedPoint(me.getPoint(), me.getOrientation());

        // Prepare packet to report my location
        MazewarPacket outgoing = new MazewarPacket();
        outgoing.type = MazewarPacket.REPORT_LOCATION;
        outgoing.owner = Mazewar.myName;
        outgoing.directedPoint = myDp;
        outgoing.score = me.getScore();

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