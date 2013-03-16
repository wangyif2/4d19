import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * User: Ivan
 * Date: 24/02/13
 */
public class MazewarServerHandler implements Runnable {

    private Thread thread;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public MazewarServerHandler(Socket socket) {
        thread = new Thread(this);
        this.socket = socket;

        /* stream to write to/read from client */
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        thread.start();
    }

    @Override
    public void run() {
        try {
            synchronized (MazewarServer.clientAddresses) {
                MazewarPacket fromClient = (MazewarPacket) in.readObject();
                switch (fromClient.type) {
                    case MazewarPacket.REGISTER:
                        registerClient(fromClient);
                        break;
                    case MazewarPacket.QUIT:
                        unregisterClient(fromClient);
                        break;
                }

                // Clean up after registration
                in.close();
                out.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void registerClient(MazewarPacket fromClient) {
        MazewarPacket toClient;
        try {
            while (MazewarServer.clientAddresses.containsKey(fromClient.owner)) {
                // Notify new client to change name
                toClient = new MazewarPacket();
                toClient.type = MazewarPacket.ERROR_DUPLICATED_CLIENT;

                out.writeObject(toClient);

                fromClient = (MazewarPacket) in.readObject();
                assert (fromClient != null);
            }

            // Reply with all connected clients
            toClient = new MazewarPacket();
            toClient.type = MazewarPacket.REGISTER_SUCCESS;
            toClient.clientAddresses = MazewarServer.clientAddresses;
            out.writeObject(toClient);

            MazewarServer.clientAddresses.put(fromClient.owner, fromClient.address);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void unregisterClient(MazewarPacket fromClient) {
        MazewarServer.clientAddresses.remove(fromClient.owner);
    }
}