import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Ivan
 * Date: 30/01/13
 * Time: 8:51 AM
 * To change this temp use File | Settings | File Templates.
 */
public class MazewarServerHandler extends Thread {
    final static Logger logger = LoggerFactory.getLogger(MazewarServerHandler.class);
    private Socket socket;
    public ObjectOutputStream out;

    public MazewarServerHandler(Socket socket) {
        this.socket = socket;

        /* stream to write back to client */
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Created a new thread to handle Mazewar Client");
    }

    @Override
    public void run() {
        try {
            /* stream to read from client */
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            MazewarPacket fromClient;

            while ((fromClient = (MazewarPacket) in.readObject()) != null) {
                // Print the packet message on screen for now
                switch (fromClient.type) {
                    case MazewarPacket.REGISTER:
                        registerClient(fromClient);
                        break;
                    case MazewarPacket.MOVE_FORWARD:
                    case MazewarPacket.MOVE_BACKWARD:
                        moveClient(fromClient);
                        break;
                    default:
                        logger.info("ERROR: Unrecognized packet!");
                }
                logger.info("Finished handling request type " + fromClient.type);
                logger.info("Current number of connedtedClients: " + MazewarServer.connectedClients.size());
            }


            /* cleanup when client exits */
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void moveClient(MazewarPacket fromClient) {
        String clientName = fromClient.owner;
        DirectedPoint clientDp = fromClient.mazeMap.get(clientName);
        logger.info("moveClient: " + clientName +
                "\n\tto X: " + clientDp.getX() +
                "\n\tto Y: " + clientDp.getY() +
                "\n\torientation : " + clientDp.getDirection()
        );

        for (Map.Entry<String, DirectedPoint> savedClient : MazewarServer.mazeMap.entrySet()) {
            DirectedPoint savedClientDp = savedClient.getValue();
            int savedClientX = savedClientDp.getX();
            int savedClientY = savedClientDp.getY();

            if (clientDp.getX() == savedClientX && clientDp.getY() == savedClientY)
                return;
        }

        MazewarServer.mazeMap.put(clientName, clientDp);
        MazewarServer.actionQueue.add(fromClient);
    }

    private void registerClient(MazewarPacket fromClient) {
        synchronized (this) {
            String clientName = fromClient.owner;

            logger.info("registerClient: " + clientName);
            MazewarServer.connectedClients.put(clientName, out);
        }
    }
}