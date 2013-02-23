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
import java.net.Socket;
import java.net.UnknownHostException;

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
    public static Socket playerSocket = null;
    public static ObjectOutputStream out;
    public static ObjectInputStream in;

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
     * The {@link Maze} that the game uses.
     */
    private Maze maze = null;

    /**
     * The {@link Client} for the game.
     */
    private LocalClient myClient = null;

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
     * @param hostname
     * @param port
     * @param isRobot
     */
    public Mazewar(String hostname, int port, boolean isRobot) {
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

        // Network initialization
        try {
            playerSocket = new Socket(hostname, port);

            /* stream to write back to server */
            out = new ObjectOutputStream(playerSocket.getOutputStream());
            /* stream to read from server */
            in = new ObjectInputStream(playerSocket.getInputStream());

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
                toServer.type = MazewarPacket.REGISTER;
                toServer.owner = myName;

                out.writeObject(toServer);
                fromServer = (MazewarPacket) in.readObject();
            } while (fromServer.type != MazewarPacket.REGISTER_SUCCESS);
            logger.info("Registered with name: " + myName + " is successful!\n");
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

        // Create the RobotClient/GUIClient connect it to the KeyListener queue
        myClient = isRobot ? new RobotClient(myName) : new GUIClient(myName);
        maze.addClient(myClient);
        if (isRobot)
            this.addKeyListener((RobotClient) myClient);
        else
            this.addKeyListener((GUIClient) myClient);
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


    /**
     * Entry point for the game.
     *
     * @param args Command-line arguments.
     */
    public static void main(String args[]) {
        // variables for hostname/port
        String hostname;
        int port;
        boolean isRobot = false;


        // Check argument correctness
        if (args.length != 2 && args.length != 3) {
            System.err.println("ERROR: Invalid arguments!");
            System.exit(-1);
        }

        hostname = args[0];
        port = Integer.parseInt(args[1]);

        // Distinguish between robot client and gui client
        if (args.length == 3) {
            if (args[2].toLowerCase().equals("robot")) {
                isRobot = true;
            } else if (!args[2].toLowerCase().equals("player")) {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        }

        /* Create the GUI */
        new Mazewar(hostname, port, isRobot);
    }
}
