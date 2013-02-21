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
                    case MazewarPacket.KILLED:
                        killClient(fromClient);
                        break;
                    case MazewarPacket.QUIT:
                        quitClient(fromClient);
                        break polling;
                    default:
                        break;
                }
            }

            /* cleanup when client exits */
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void killClient(MazewarPacket fromClient) {
        synchronized (MazewarServer.mazeMap) {
            String srcClientName = fromClient.owner;
            String tgtClientName = fromClient.killed;
            DirectedPoint tgtClientLoc = fromClient.mazeMap.get(tgtClientName);

            MazewarServer.mazeMap.put(tgtClientName, tgtClientLoc);

            synchronized (MazewarServer.mazeScore) {
                //adjust the score for killing
                MazewarServer.mazeScore.put(tgtClientName, MazewarServer.mazeScore.get(tgtClientName) + MazewarServer.scoreAdjKilled);
                MazewarServer.mazeScore.put(srcClientName, MazewarServer.mazeScore.get(srcClientName) + MazewarServer.scoreAdjKill);
            }

            MazewarServer.actionQueue.add(fromClient);
        }
    }

    private void firedClient(MazewarPacket fromClient) {
        synchronized (MazewarServer.mazeScore) {
            String clientName = fromClient.owner;

            MazewarServer.mazeScore.put(clientName, MazewarServer.mazeScore.get(clientName) + MazewarServer.scoreAdjFire);

            MazewarServer.actionQueue.add(fromClient);
        }
    }


    private void quitClient(MazewarPacket fromClient) {
        synchronized (MazewarServer.connectedClients) {
            String clientName = fromClient.owner;
            MazewarServer.connectedClients.remove(clientName);

            synchronized (MazewarServer.mazeMap) {
                MazewarServer.mazeMap.remove(clientName);

                synchronized (MazewarServer.mazeScore) {
                    MazewarServer.mazeScore.remove(clientName);
                }
            }

            MazewarServer.actionQueue.add(fromClient);
        }
    }

    private void rotateClient(MazewarPacket fromClient) {
        synchronized (MazewarServer.mazeMap) {
            String clientName = fromClient.owner;
            DirectedPoint clientDp = fromClient.mazeMap.get(clientName);

            MazewarServer.mazeMap.put(clientName, clientDp);
            MazewarServer.actionQueue.add(fromClient);
        }
    }

    private void addClient(MazewarPacket fromClient) {

        synchronized (MazewarServer.mazeMap) {
            String clientName = fromClient.owner;
            DirectedPoint clientDp = fromClient.mazeMap.get(clientName);

            MazewarPacket replyPacket;
            for (Map.Entry<String, DirectedPoint> entry : MazewarServer.mazeMap.entrySet()) {
                Point savedPoint = entry.getValue();
                if (savedPoint.equals((Point) clientDp)) {
                    replyPacket = new MazewarPacket();
                    replyPacket.type = MazewarPacket.ERROR_DUPLICATED_LOCATION;

                    synchronized (this.out) {
                        try {
                            out.writeObject(replyPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return;
                }
            }

            replyPacket = new MazewarPacket();
            replyPacket.type = MazewarPacket.ADD_SUCCESS;
            replyPacket.owner = clientName;

            synchronized (this.out) {
                try {
                    out.writeObject(replyPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            MazewarServer.mazeMap.put(clientName, clientDp);

            synchronized (MazewarServer.mazeScore) {
                MazewarServer.mazeScore.put(clientName, 0);
            }

            MazewarServer.actionQueue.add(fromClient);
        }
    }

    private void moveClient(MazewarPacket fromClient) {
        synchronized (MazewarServer.mazeMap) {
            String clientName = fromClient.owner;
            DirectedPoint clientDp = fromClient.mazeMap.get(clientName);

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
        synchronized (MazewarServer.connectedClients) {
            String clientName = fromClient.owner;
            MazewarPacket replyPacket = new MazewarPacket();
            replyPacket.owner = clientName;

            if (!MazewarServer.connectedClients.containsKey(clientName)) {
                MazewarServer.connectedClients.put(clientName, out);

                replyPacket.type = MazewarPacket.REGISTER_SUCCESS;
                synchronized (MazewarServer.mazeMap) {
                    replyPacket.mazeMap = MazewarServer.mazeMap;
                    synchronized (MazewarServer.mazeScore) {
                        replyPacket.mazeScore = MazewarServer.mazeScore;
                    }
                }
            } else {
                replyPacket.type = MazewarPacket.ERROR_DUPLICATED_CLIENT;
            }

            synchronized (this.out) {
                try {
                    out.writeObject(replyPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}