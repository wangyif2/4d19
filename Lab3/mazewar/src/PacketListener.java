import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class PacketListener implements Runnable {

    private boolean listening = true;
    private Thread thread;
    private String clientName;
    private Socket clientSocket;
    private ObjectInputStream in;

    public PacketListener(String clientName, Socket clientSocket, ObjectInputStream in) {
        thread = new Thread(this);
        this.clientName = clientName;
        this.clientSocket = clientSocket;
        this.in = in;

        thread.start();
    }

    @Override
    public void run() {
        MazewarPacket incoming;
        while (listening) {
            try {
                // Listen to new incoming packet
                incoming = (MazewarPacket) in.readObject();

                // Add remote client to maze if the packet is REPORT_LOCATION type
                if (incoming.type == MazewarPacket.REPORT_LOCATION) {
                    Mazewar.maze.addRemoteClient(incoming.owner, incoming.directedPoint, incoming.score);
                    continue;
                }

                /**
                 * For all other types of action packets:
                 */
                // Update local lamport clock
                updateLamportClk(incoming.seqNum);

                if (incoming.ACKer == null) {
                    /* Action packet*/
                    // Make sure all connection has been established when receiving a ADD_NOTICE packet
                    if (incoming.type == MazewarPacket.ADD_NOTICE)
                        while (!Mazewar.connectedClients.contains(incoming.newClient) || !Mazewar.allConnected) try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    // ACK to myself
                    PacketMulticaster.trackAck(incoming, Mazewar.myName);


                    // Add action to queue
                    Mazewar.actionQueue.add(incoming);

                    // Multicast ACK to all clients
                    Mazewar.multicaster.multicastACK(incoming);

                    // Be ready to kill the thread if QUIT packet comes
                    if (incoming.type == MazewarPacket.QUIT) listening = false;
                } else {
                    /* ACK packet*/
                    // Add ACKer to track map
                    PacketMulticaster.trackAck(incoming, incoming.ACKer);
                }
            } catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
            }
        }

        // Receive a final ack packet and close the socket
        try {
            incoming = (MazewarPacket) in.readObject();
            // Update local lamport clock
            updateLamportClk(incoming.seqNum);

            // Add ACK to ackTracker
            synchronized (Mazewar.ackTracker) {
                Mazewar.ackTracker.get(incoming).add(incoming.ACKer);
            }
        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        }

        try {
            synchronized (Mazewar.connectedOuts) {
                Mazewar.connectedClients.remove(clientName);
                in.close();
                Mazewar.connectedIns.remove(clientName);
                Mazewar.connectedOuts.get(clientName).close();
                Mazewar.connectedOuts.remove(clientName);
            }
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateLamportClk(Integer seqNum) {
        synchronized (Mazewar.lamportClk) {
            Mazewar.lamportClk = seqNum > Mazewar.lamportClk ? seqNum : Mazewar.lamportClk;
        }
    }
}
