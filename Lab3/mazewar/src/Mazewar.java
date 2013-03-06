/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 *
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(Mazewar.class);

    public static String myName;
    public static InetSocketAddress myAddress;
    public static boolean allConnected;
    public static Set<String> connectedClients = new HashSet<String>();
    public static HashMap<String, ObjectOutputStream> connectedOuts = new HashMap<String, ObjectOutputStream>();
    public static HashMap<String, ObjectInputStream> connectedIns = new HashMap<String, ObjectInputStream>();
    public static int lamportClk = 0;
    public static PriorityBlockingQueue<MazewarPacket> actionQueue = new PriorityBlockingQueue<MazewarPacket>();
    public static ConcurrentHashMap<MazewarPacketIdentifier, Set<String>> ackTracker = new ConcurrentHashMap<MazewarPacketIdentifier, Set<String>>();
    public static Socket playerSocket = null;
    public static ObjectOutputStream out;

    public static ObjectInputStream in;

    public static boolean isRegisterComplete = false;

    public static PacketMulticaster multicaster;

    /**
     * The {@link Maze} that the game uses.
     */
    public static Maze maze = null;

    /**
     * The {@link Client} for the game.
     */
    public static LocalClient myClient = null;

    /**
     * The default width of the {@link Maze}.
     */
    private final int mazeWidth = 20;

    /**
     * The default height of the {@link Maze}.
     */
    private final int mazeHeight = 10;

    /**
     * The default random seed for the {@link Maze}.
     * All implementations of the same protocol must use
     * the same seed value, or your mazes will be different.
     */
    private final int mazeSeed = 42;

    /**
     * The panel that displays the {@link Maze}.
     */
    private OverheadMazePanel overheadPanel = null;

    /**
     * The table the displays the scores.
     */
    private JTable scoreTable = null;
    /**
     * Create the textpane statically so that we can
     * write to it globally using
     * the static consolePrint methods
     */
    private static final JTextPane console = new JTextPane();

    /**
     * Write a message to the console followed by a newline.
     *
     * @param msg The {@link String} to print.
     */
    public static synchronized void consolePrintLn(String msg) {
        console.setText(console.getText() + msg + "\n");
    }

    /**
     * Write a message to the console.
     *
     * @param msg The {@link String} to print.
     */
    public static synchronized void consolePrint(String msg) {
        console.setText(console.getText() + msg);
    }

    /**
     * Clear the console.
     */
    public static synchronized void clearConsole() {
        console.setText("");
    }

    /**
     * Static method for performing cleanup before exiting the game.
     */
    public static void quit() {
        // Network clean-up
        try {
            out.close();
            in.close();
            playerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    /**
     * The place where all the pieces are put together.
     *
     * @param serverHostname
     * @param serverPort
     * @param isRobot
     */
    public Mazewar(String serverHostname, int serverPort, boolean isRobot) {
        super("ECE419 Mazewar");
        consolePrintLn("ECE419 Mazewar started!");

        // Create the maze
        maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
        assert (maze != null);

        // Have the ScoreTableModel listen to the maze to find
        // out how to adjust scores.
        ScoreTableModel scoreModel = new ScoreTableModel();
        assert (scoreModel != null);
        maze.addMazeListener(scoreModel);

        // Register at naming service
        HashMap<String, InetSocketAddress> clientAddresses = null;
        try {
            Socket socket = new Socket(serverHostname, serverPort);

            /* stream to write to/read from server */
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            MazewarPacket toServer;
            MazewarPacket fromServer;

            // Register client name in server
            do {
                // Throw up a dialog to get the GUIClient name.
                myName = JOptionPane.showInputDialog("Enter your name");
                if ((myName == null) || (myName.length() == 0)) {
                    Mazewar.quit();
                }

                // Send register packet to server
                toServer = new MazewarPacket();
                toServer.owner = myName;
                toServer.address = myAddress;
                out.writeObject(toServer);

                fromServer = (MazewarPacket) in.readObject();
            } while (fromServer.type != MazewarPacket.REGISTER_SUCCESS);

            connectedClients.add(myName);
            clientAddresses = fromServer.connectedClients;
            logger.info("Registered at naming service with name: " + myName.toUpperCase() + " is successful!\n");

            // Clean up socket connection
            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("ERROR: Don't know where to connect.");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR: Couldn't get I/O for the connection.");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("ERROR: Class not found.");
            System.exit(1);
        }

        // Create a packet multicaster
        multicaster = new PacketMulticaster(connectedClients, connectedOuts, connectedIns);

        // Create a connection listener to monitor new connection request
        new ConnectionListener(myAddress.getPort());

        // Initialize a hold back queue for processing actions
        actionQueue = new PriorityBlockingQueue<MazewarPacket>();
        new ActionProcessor();

        // Connect to all existing players
        connectToAllPlayers(clientAddresses);

        // Create the RobotClient/GUIClient connect it to the KeyListener queue
        myClient = isRobot ? new RobotClient(myName) : new GUIClient(myName);
        // Add myself to maze right away if I am the first one
        if (connectedClients.size() == 1) {
            maze.addClient(myClient);
            isRegisterComplete = true;
        }
        // Wait until the register process is complete
        while (!isRegisterComplete) try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start the game
        this.addKeyListener(myClient);
        myClient.start();

        // Create the panel that will display the maze.
        overheadPanel = new OverheadMazePanel(maze, myClient);
        assert (overheadPanel != null);
        maze.addMazeListener(overheadPanel);

        // Don't allow editing the console from the GUI
        console.setEditable(false);
        console.setFocusable(false);
        console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

        // Allow the console to scroll by putting it in a scrollpane
        JScrollPane consoleScrollPane = new JScrollPane(console);
        assert (consoleScrollPane != null);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));

        // Create the score table
        scoreTable = new JTable(scoreModel);
        assert (scoreTable != null);
        scoreTable.setFocusable(false);
        scoreTable.setRowSelectionAllowed(false);

        // Allow the score table to scroll too.
        JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
        assert (scoreScrollPane != null);
        scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));

        // Create the layout manager
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(layout);

        // Define the constraints on the components.
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 3.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(overheadPanel, c);
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 2.0;
        c.weighty = 1.0;
        layout.setConstraints(consoleScrollPane, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        layout.setConstraints(scoreScrollPane, c);

        // Add the components
        getContentPane().add(overheadPanel);
        getContentPane().add(consoleScrollPane);
        getContentPane().add(scoreScrollPane);

        // Pack everything neatly.
        pack();

        // Let the magic begin.
        setVisible(true);
        overheadPanel.repaint();
        this.requestFocusInWindow();
    }

    private void connectToAllPlayers(HashMap<String, InetSocketAddress> clientAddresses) {
        // Add all clients to connectedClients Set
        connectedClients.addAll(clientAddresses.keySet());

        // Establish connection with all clients
        InetSocketAddress addr;
        for (Map.Entry<String, InetSocketAddress> entry : clientAddresses.entrySet()) {
            addr = entry.getValue();
            try {
                Socket socket = new Socket(addr.getAddress(), addr.getPort());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // Tell myName to the client
                MazewarPacket outgoing = new MazewarPacket();
                outgoing.owner = myName;
                out.writeObject(outgoing);

                // Add in/out to hash maps
                connectedOuts.put(entry.getKey(), out);
                connectedIns.put(entry.getKey(), in);

                new PacketListener(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        allConnected = true;
    }

    /**
     * Entry point for the game.
     *
     * @param args Command-line arguments.
     */
    public static void main(String args[]) {
        // variables for hostname/port
        String serverHostname;
        int serverPort;
        boolean isRobot = false;

        // Check argument correctness
        if (args.length != 3 && args.length != 4) {
            System.err.println("ERROR: Invalid arguments!");
            System.exit(-1);
        }

        serverHostname = args[0];
        serverPort = Integer.parseInt(args[1]);
        try {
            myAddress = new InetSocketAddress(InetAddress.getLocalHost(), Integer.parseInt(args[2]));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("ERROR: Can't get localhost IP address!");
            System.exit(-1);
        }

        // Distinguish between robot client and gui client
        if (args.length == 4) {
            if (args[3].toLowerCase().equals("robot")) {
                isRobot = true;
            } else if (!args[3].toLowerCase().equals("player")) {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        }

        /* Create the GUI */
        new Mazewar(serverHostname, serverPort, isRobot);
    }
}
