import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * User: Ivan
 * Date: 01/02/13
 */
public class MazewarServerBroadcast extends Thread {

    private boolean DEBUG = true;

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
                                    else if (DEBUG)
                                        System.out.println("Client " + broadcastPacket.owner + " doesn't exists!");
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
                        System.out.println("broadcasting to " + broadcastPacket.owner + " " + broadcastPacket.type);
                        entry.getValue().writeObject(broadcastPacket);
                    }

                }
            }
        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
        }
    }
}
