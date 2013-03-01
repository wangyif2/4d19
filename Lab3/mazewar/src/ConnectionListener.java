import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class ConnectionListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MazewarServerHandler.class);

    private Thread thread;
    private boolean listening = true;
    private ServerSocket listeningSocket;

    public ConnectionListener(int port) {
        thread = new Thread(this);

        try {
            listeningSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        thread.start();
    }

    @Override
    public void run() {
        while (listening) {
            try {
                Socket newSocket = listeningSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(newSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(newSocket.getInputStream());
                new PacketListener(out, in);
                logger.info("Received connection request!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
