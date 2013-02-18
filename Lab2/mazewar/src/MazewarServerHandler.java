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
                    case MazewarPacket.ADD:
                        addClient(fromClient);
                        break;
                    case MazewarPacket.MOVE_FORWARD:
                    case MazewarPacket.MOVE_BACKWARD:
                        moveClient(fromClient);
                        break;
                    case MazewarPacket.TURN_LEFT:
                    case MazewarPacket.TURN_RIGHT:
                        rotateClient(fromClient);
                        break;
                    case MazewarPacket.FIRE:
                        firedClient(fromClient);
                        break;
                    case MazewarPacket.QUIT:
                        quitClient(fromClient);
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

    private void firedClient(MazewarPacket fromClient) {
        synchronized (this){
            String clientName = fromClient.owner;

            logger.info("Client " + clientName + " fired on sender " + fromClient.sender);
            MazewarServer.actionQueue.add(fromClient);
        }
    }

    private void quitClient(MazewarPacket fromClient) {
        synchronized (this) {
            String clientName = fromClient.owner;

            MazewarServer.mazeMap.remove(clientName);
            MazewarServer.connectedClients.remove(clientName);
            MazewarServer.actionQueue.add(fromClient);

        }
    }

    private void rotateClient(MazewarPacket fromClient) {
        synchronized (this) {
            String clientName = fromClient.owner;
            DirectedPoint clientDp = fromClient.mazeMap.get(clientName);
            logger.info("rotateClient: " + clientName +
                    "\n\tto X: " + clientDp.getX() +
                    "\n\tto Y: " + clientDp.getY() +
                    "\n\torientation : " + clientDp.getDirection()
            );

            MazewarServer.mazeMap.put(clientName, clientDp);
            MazewarServer.actionQueue.add(fromClient);
        }
    }

    private void addClient(MazewarPacket fromClient) {
        synchronized (this) {
            String clientName = fromClient.owner;
            DirectedPoint clientDp = fromClient.mazeMap.get(clientName);
            logger.info("addClient: " + clientName +
                    "\n\tto X: " + clientDp.getX() +
                    "\n\tto Y: " + clientDp.getY() +
                    "\n\torientation : " + clientDp.getDirection()
            );

            if (MazewarServer.mazeMap.containsKey(clientName)) {
                logger.info("Duplicated Client: " + clientName);

                DirectedPoint savedDp = MazewarServer.mazeMap.get(clientName);

                MazewarPacket replyPacket = new MazewarPacket();
                replyPacket.type = MazewarPacket.ERROR_DUPLICATED_CLIENT;
                replyPacket.owner = clientName;
                replyPacket.mazeMap.put(clientName, savedDp);

                try {
                    out.writeObject(replyPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                logger.info("Add Success Client: " + clientName);
                MazewarPacket replyPacket = new MazewarPacket();
                replyPacket.type = MazewarPacket.ADD_SUCCESS;
                replyPacket.owner = clientName;

                try {
                    out.writeObject(replyPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                MazewarServer.mazeMap.put(clientName, clientDp);
                MazewarServer.actionQueue.add(fromClient);
            }
        }
    }

    private void moveClient(MazewarPacket fromClient) {
        synchronized (this) {
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
    }

    private void registerClient(MazewarPacket fromClient) {
        synchronized (this) {
            String clientName = fromClient.owner;

            logger.info("registerClient: " + clientName);
            MazewarServer.connectedClients.put(clientName, out);

            MazewarPacket replyPacket = new MazewarPacket();
            replyPacket.type = MazewarPacket.REGISTER_SUCCESS;
            replyPacket.mazeMap = MazewarServer.mazeMap;
            replyPacket.owner = fromClient.owner;

            try {
                out.writeObject(replyPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}