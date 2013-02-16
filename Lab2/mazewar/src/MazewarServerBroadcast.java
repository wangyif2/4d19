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
                        if (broadcastPacket.owner.equals(entry.getKey())) {
                        } else {
                            switch (broadcastPacket.type) {
                                case MazewarPacket.REGISTER:
//                                    broadcastPacket.type = MazewarPacket.REGISTER;
                                    break;
                                case MazewarPacket.QUIT:
                                    break;
                                case MazewarPacket.MOVE_BACKWARD:
                                    break;
                                case MazewarPacket.MOVE_FORWARD:
                                    break;
                                case MazewarPacket.TURN_LEFT:
                                    break;
                                case MazewarPacket.TURN_RIGHT:
                                    break;
                                case MazewarPacket.FIRE:
                                    break;
                                case MazewarPacket.KILLED:
                                    break;
                                default:
                            }
                            logger.info("broadcasting to " + entry.getKey() + " " + broadcastPacket.type);
                            entry.getValue().writeObject(broadcastPacket);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
