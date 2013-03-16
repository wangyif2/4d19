import java.io.IOException;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class ActionProcessor implements Runnable {

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
                if (Mazewar.ackTracker.get(nextAction).size() >= Mazewar.connectedClients.size()) {
                    Mazewar.actionQueue.poll();
                    Mazewar.ackTracker.remove(nextAction);
                    switch (nextAction.type) {
                        case MazewarPacket.ADD_NOTICE:
                            if (nextAction.newClient.equals(Mazewar.myName))
                                Mazewar.myClient.notifyAdd();
                            else
                                reportLocation(nextAction.newClient, Mazewar.myClient);
                            break;
                        case MazewarPacket.ADD:
                            if (nextAction.owner.equals(Mazewar.myName)) {
                                /* I am the new client */
                                Mazewar.isRegisterComplete = true;
                            } else {
                                /* Add a new remote client */
                                Mazewar.maze.addRemoteClient(nextAction.owner, nextAction.directedPoint, 0);
                                Mazewar.myClient.resume();
                            }
                            break;
                        case MazewarPacket.MOVE_FORWARD:
                            Mazewar.maze.getClientByName(nextAction.owner).forward();
                            break;
                        case MazewarPacket.MOVE_BACKWARD:
                            Mazewar.maze.getClientByName(nextAction.owner).backup();
                            break;
                        case MazewarPacket.TURN_LEFT:
                            Mazewar.maze.getClientByName(nextAction.owner).turnLeft();
                            break;
                        case MazewarPacket.TURN_RIGHT:
                            Mazewar.maze.getClientByName(nextAction.owner).turnRight();
                            break;
                        case MazewarPacket.FIRE:
                            Mazewar.maze.getClientByName(nextAction.owner).fire();
                            break;
                        case MazewarPacket.INSTANT_KILL:
                            Mazewar.maze.getClientByName(nextAction.owner).kill(nextAction.victim, nextAction.directedPoint, true);
                            break;
                        case MazewarPacket.KILL:
                            Mazewar.maze.getClientByName(nextAction.owner).kill(nextAction.victim, nextAction.directedPoint, false);
                            break;
                        case MazewarPacket.QUIT:
                            Mazewar.maze.getClientByName(nextAction.owner).quit();
                            break;
                    }
                }
            }
        }
    }

    private void reportLocation(String newClient, LocalClient me) {
        // Suspend user input and random generator
        me.pause();

        // Wait 100ms for all packets to be queue
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            // Report my location to the new guy
            try {
                Mazewar.connectedOuts.get(newClient).writeObject(outgoing);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}