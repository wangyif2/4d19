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

    public MazewarServerHandler(Socket socket) {
        this.socket = socket;
        if (DEBUG) System.out.println("Created a new thread to handle Mazewar Client");
    }

    @Override
    public void run() {
        try {
            /* stream to read from client */
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            /* stream to write back to client */
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            MazewarPacket packetFromClient;
            MazewarPacket packetToClient;

            while ((packetFromClient = (MazewarPacket) in.readObject()) != null) {

                // Print the packet message on screen for now
                System.out.println(packetFromClient.message);

                // Send back ACK to confirm the message from client
                packetToClient = new MazewarPacket();
                packetToClient.message = packetFromClient.message + " CONFIRMED!";
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
