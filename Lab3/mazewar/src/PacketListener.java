import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class PacketListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MazewarServerHandler.class);

    private Thread thread;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public PacketListener(ObjectOutputStream out, ObjectInputStream in) {
        thread = new Thread(this);
        this.out = out;
        this.in = in;

        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                MazewarPacket incoming = (MazewarPacket) in.readObject();
                logger.info("Received packet from: " + incoming.owner.toUpperCase());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
