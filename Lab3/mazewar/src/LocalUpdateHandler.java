import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * User: Ivan
 * Date: 24/02/13
 */
public class LocalUpdateHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LocalClient.class);

    private Maze maze = null;
    private final Thread updateThread;

    public LocalUpdateHandler() {
        updateThread = new Thread(this);
    }

    public void start() {
        updateThread.start();
    }

    public void registerMaze(Maze maze) {
        this.maze = maze;
    }

    /**
     * Listening thread to get updates from server
     */
    @Override
    public void run() {
        MazewarPacket fromServer;
        Client owner;

        while (true) {
            assert (Mazewar.in != null);
            if (Mazewar.playerSocket.isClosed()) break;

            try {
                synchronized (Mazewar.in) {
                    fromServer = (MazewarPacket) Mazewar.in.readObject();
                }

                owner = maze.getClientByName(fromServer.owner);
                if (fromServer != null) {
                    switch (fromServer.type) {
                        case MazewarPacket.ADD:
                            maze.addRemoteClient(new RemoteClient(fromServer.owner), getNewDp(fromServer.mazeMap.get(fromServer.owner)));
                            logger.info("Added remote client: " + fromServer.owner + "\n");
                            break;
                        case MazewarPacket.MOVE_FORWARD:
                            owner.forward();
                            logger.info("Moved client: " + fromServer.owner + " forward\n");
                            break;
                        case MazewarPacket.MOVE_BACKWARD:
                            owner.backup();
                            logger.info("Moved client: " + fromServer.owner + " backward\n");
                            break;
                        case MazewarPacket.TURN_LEFT:
                            owner.turnLeft();
                            logger.info("Rotated client: " + fromServer.owner + " left\n");
                            break;
                        case MazewarPacket.TURN_RIGHT:
                            owner.turnRight();
                            logger.info("Rotated client: " + fromServer.owner + " right\n");
                            break;
                        case MazewarPacket.FIRE:
                            owner.fire();
                            logger.info("Client " + fromServer.owner + " fired\n");
                            break;
                        case MazewarPacket.INSTANT_KILL:
                            maze.killClient(owner,
                                    maze.getClientByName(fromServer.victim),
                                    getNewDp(fromServer.mazeMap.get(fromServer.victim)),
                                    true);
                            logger.info("Client " + fromServer.owner + " instantly killed " + fromServer.victim + "\n");
                            break;
                        case MazewarPacket.KILL:
                            maze.killClient(owner,
                                    maze.getClientByName(fromServer.victim),
                                    getNewDp(fromServer.mazeMap.get(fromServer.victim)),
                                    false);
                            logger.info("Client " + fromServer.owner + " killed " + fromServer.victim + "\n");
                            break;
                        case MazewarPacket.QUIT:
                            owner.quit();
                            logger.info(fromServer.owner + " quitting");
                            break;
                        default:
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /* Internals */
    private DirectedPoint getNewDp(DirectedPoint dp) {
        Point p = dp;
        Direction d = dp.getDirection();
        return new DirectedPoint(p, d);
    }
}
