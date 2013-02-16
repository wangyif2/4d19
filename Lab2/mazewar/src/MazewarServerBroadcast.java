import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * User: Ivan
 * Date: 01/02/13
 */
public class MazewarServerBroadcast extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(MazewarServerBroadcast.class);

    @Override
    public void run() {
        try {
            MazewarPacket broadcastPacket;

            while (true) {
                while ((broadcastPacket = MazewarServer.actionQueue.poll()) != null) {
                    for (Map.Entry<String, ObjectOutputStream> entry : MazewarServer.connectedClients.entrySet()) {
                        switch (broadcastPacket.type) {
                            case MazewarPacket.REGISTER:
                                if (broadcastPacket.owner == entry.getKey()) continue;
                                break;
                            case MazewarPacket.QUIT:
                                synchronized (this) {
                                    if (MazewarServer.connectedClients.containsKey(broadcastPacket.owner))
                                        MazewarServer.connectedClients.remove(broadcastPacket.owner);
                                    else
                                        logger.info("Client " + broadcastPacket.owner + " doesn't exists!");
                                }
                                break;
                            case MazewarPacket.MOVE_BACKWARD:
                            case MazewarPacket.MOVE_FORWARD:
                            case MazewarPacket.TURN_LEFT:
                            case MazewarPacket.TURN_RIGHT:
                            case MazewarPacket.FIRE:
                            case MazewarPacket.KILLED:
                                break;
                            default:
                        }
                        logger.info("broadcasting to " + broadcastPacket.owner + " " + broadcastPacket.type);
                        entry.getValue().writeObject(broadcastPacket);
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
