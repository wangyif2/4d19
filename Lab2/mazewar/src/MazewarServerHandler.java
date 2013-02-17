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

            polling:
            while ((fromClient = (MazewarPacket) in.readObject()) != null) {
                // Print the packet message on screen for now
                switch (fromClient.type) {
                    case MazewarPacket.ADD:
                        out.writeObject(addClient(fromClient));
                    case MazewarPacket.REGISTER:
                        //TODO: Handle client registration with same name
                        out.writeObject(registerClient(fromClient));
                        break;
                    case MazewarPacket.MOVE:
                        if (isValidClientLocation(fromClient))
                            updateClientLocation(fromClient);
                        break;
                    case MazewarPacket.TURN_LEFT:
                    case MazewarPacket.TURN_RIGHT:
                        updateClientLocation(fromClient);
                        break;
                    case MazewarPacket.FIRE:
                        MazewarServer.actionQueue.add(fromClient);
                        logger.info("ACTION: Firing!");
                        break;
                    case MazewarPacket.QUIT:
                        quitClient(fromClient);
                        break polling;
                    default:
                        logger.info("ERROR: Unrecognized packet!");
                }
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

    private MazewarPacket addClient(MazewarPacket fromClient) {
        MazewarPacket toClient = new MazewarPacket();
        String clientName = fromClient.owner;
        DirectedPoint clientLocation = fromClient.mazeMap.get(fromClient.owner);

        synchronized (this) {
            if (MazewarServer.mazeMap.containsValue(clientLocation)) {
                toClient.type = MazewarPacket.ERROR_DUPLICATED_LOCATION;
            }
            //all good, go ahead and add
            else {
                logger.info("ADDED: " + clientName + " at location " + clientLocation);
                MazewarServer.mazeMap.put(clientName, clientLocation);

                toClient.type = MazewarPacket.ADD_SUCCESS;
            }

            return toClient;
        }
    }

    private MazewarPacket registerClient(MazewarPacket fromClient) {
        MazewarPacket toClient = new MazewarPacket();
        String clientName = fromClient.owner;

        synchronized (this) {
            //if client trying to register duplicated name
            if (MazewarServer.connectedClients.containsKey(clientName)) {
                toClient.type = MazewarPacket.ERROR_DUPLICATED_CLIENT;
            }
            //all good, go ahead and register
            else {
                logger.info("REGISTER: " + clientName);
                MazewarServer.connectedClients.put(clientName, out);

                toClient.type = MazewarPacket.REGISTER_SUCCESS;
                toClient.mazeMap = MazewarServer.mazeMap;
            }

            return toClient;
        }
    }

    private boolean isValidClientLocation(MazewarPacket fromClient) {
        DirectedPoint clientLocation = fromClient.mazeMap.get(fromClient.owner);

        for (Map.Entry<String, DirectedPoint> savedClient : MazewarServer.mazeMap.entrySet()) {
            Point location = savedClient.getValue();
            if (location.equals(clientLocation))
                return false;
        }
        return true;
    }

    private void updateClientLocation(MazewarPacket fromClient) {
        String clientName = fromClient.owner;
        DirectedPoint clientLocation = fromClient.mazeMap.get(fromClient.owner);

        MazewarServer.mazeMap.put(clientName, clientLocation);
        MazewarServer.actionQueue.add(fromClient);
    }

    private void quitClient(MazewarPacket fromClient) {
        synchronized (this) {
            if (MazewarServer.connectedClients.containsKey(fromClient.owner))
                MazewarServer.connectedClients.remove(fromClient.owner);
            else
                logger.info("Client " + fromClient.owner + " doesn't exists!");
        }
        MazewarServer.actionQueue.add(fromClient);
        logger.info("ACTION: Quiting!");
        logger.info(fromClient.owner + "Disconnected!");
    }
}
