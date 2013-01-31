import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: Ivan
 * Date: 30/01/13
 * Time: 8:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class MazewarServerHandler extends Thread {

    private boolean DEBUG = true;

    private Socket socket;

    public ObjectOutputStream out;

    public MazewarServerHandler(Socket socket) {
        this.socket = socket;

        /* stream to write back to client */
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
        }

        if (DEBUG) System.out.println("Created a new thread to handle Mazewar Client");
    }

    @Override
    public void run() {
        try {

            /* stream to read from client */
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            MazewarPacket packetFromClient;
            MazewarPacket packetToClient;

            polling:
            while ((packetFromClient = (MazewarPacket) in.readObject()) != null) {

                MazewarAction action;
                // Print the packet message on screen for now
                switch (packetFromClient.type) {
                    case MazewarPacket.REGISTER:
                        //action = new MazewarAction(packetFromClient.owner, packetFromClient.type);
                        synchronized (this) {
                            if (!MazewarServer.connectedClients.containsKey(packetFromClient.owner))
                                MazewarServer.connectedClients.put(packetFromClient.owner, out);
                            else
                                if (DEBUG) System.out.println("Client " + packetFromClient.owner + " already exists!");
                        }
                        if (DEBUG) System.out.println("REGISTER: " + packetFromClient.owner);
                        break;
                    case MazewarPacket.MOVE_FORWARD:
                        action = new MazewarAction(packetFromClient.owner, packetFromClient.type);
                        MazewarServer.actionQueue.add(action);
                        if (DEBUG) System.out.println("ACTION: Moving forward!");
                        break;
                    case MazewarPacket.MOVE_BACKWARD:
                        action = new MazewarAction(packetFromClient.owner, packetFromClient.type);
                        MazewarServer.actionQueue.add(action);
                        if (DEBUG) System.out.println("ACTION: Moving backward!");
                        break;
                    case MazewarPacket.TURN_LEFT:
                        action = new MazewarAction(packetFromClient.owner, packetFromClient.type);
                        MazewarServer.actionQueue.add(action);
                        if (DEBUG) System.out.println("ACTION: Turning left!");
                        break;
                    case MazewarPacket.TURN_RIGHT:
                        action = new MazewarAction(packetFromClient.owner, packetFromClient.type);
                        MazewarServer.actionQueue.add(action);
                        if (DEBUG) System.out.println("ACTION: Turning right!");
                        break;
                    case MazewarPacket.FIRE:
                        action = new MazewarAction(packetFromClient.owner, packetFromClient.type);
                        MazewarServer.actionQueue.add(action);
                        if (DEBUG) System.out.println("ACTION: Firing!");
                        break;
                    case MazewarPacket.QUIT:
                        //action = new MazewarAction(packetFromClient.owner, packetFromClient.type);
                        synchronized (this) {
                            if (MazewarServer.connectedClients.containsKey(packetFromClient.owner))
                                MazewarServer.connectedClients.remove(packetFromClient.owner);
                            else
                                if (DEBUG) System.out.println("Client " + packetFromClient.owner + " doesn't exists!");
                        }
                        if (DEBUG) System.out.println("ACTION: Quiting!");
                        if (DEBUG) System.out.println(packetFromClient.owner + "Disconnected!");
                        break polling;
                    default:
                        System.out.println("ERROR: Unrecognized packet!");
                }

                // Send back ACK to confirm the message from client
                packetToClient = new MazewarPacket();
                packetToClient.type = packetFromClient.type;
                out.writeObject(packetToClient);
            }

            /* cleanup when client exits */
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
        } catch (ClassNotFoundException e) {
            if (DEBUG) e.printStackTrace();
        }
    }
}
