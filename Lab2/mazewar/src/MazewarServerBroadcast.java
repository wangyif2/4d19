import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * User: Ivan
 * Date: 01/02/13
 */
public class MazewarServerBroadcast extends Thread {

    @Override
    public void run() {
        try {
            MazewarPacket broadcastPacket;

            while (true) {
                while ((broadcastPacket = MazewarServer.actionQueue.poll()) != null) {
                    for (Map.Entry<String, ObjectOutputStream> entry : MazewarServer.connectedClients.entrySet()) {
                        switch (broadcastPacket.type) {
                            case MazewarPacket.ADD:
                            case MazewarPacket.KILLED:
                                if (broadcastPacket.owner.equals(entry.getKey()))
                                    continue;
                                break;
                            default:
                                break;
                        }
                        entry.getValue().writeObject(broadcastPacket);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}