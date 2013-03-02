import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class ActionProcessor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MazewarServerHandler.class);

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
                    switch (nextAction.type) {
                        case MazewarPacket.ADD_NOTICE:
                            addClient(nextAction);
                            break;
                    }
                    Mazewar.actionQueue.poll();
                }
            }
        }
    }

    private void addClient(MazewarPacket nextAction) {
        logger.info("Packet " + nextAction.lamportClk + " from " + nextAction.owner + " is ready to be processed\n");
    }
}
