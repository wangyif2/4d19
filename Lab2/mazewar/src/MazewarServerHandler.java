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
}
