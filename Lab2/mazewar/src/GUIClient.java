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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.
 *
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {

    private static boolean DEBUG = true;

    /**
     * Create a GUI controlled {@link LocalClient}.
     */
    public GUIClient(String name) {
        super(name);
    }

    /**
     * Handle a key press.
     *
     * @param e The {@link java.awt.event.KeyEvent} that occurred.
     */
    public void keyPressed(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit.
        MazewarPacket packetToServer = new MazewarPacket();
        packetToServer.owner = getName();

        if ((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
            // Send movement request to server
            packetToServer.type = MazewarPacket.QUIT;
            sendRequestToServer(packetToServer);
            Mazewar.quit();
            // Up-arrow moves forward.
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            // Send movement request to server
            packetToServer.type = MazewarPacket.MOVE_FORWARD;
            sendRequestToServer(packetToServer);
            // Down-arrow moves backward.
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            // Send movement request to server
            packetToServer.type = MazewarPacket.MOVE_BACKWARD;
            sendRequestToServer(packetToServer);
            // Left-arrow turns left.
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            // Send movement request to server
            packetToServer.type = MazewarPacket.TURN_LEFT;
            sendRequestToServer(packetToServer);
            // Right-arrow turns right.
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            // Send movement request to server
            packetToServer.type = MazewarPacket.TURN_RIGHT;
            sendRequestToServer(packetToServer);
            // Spacebar fires.
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            // Send movement request to server
            packetToServer.type = MazewarPacket.FIRE;
            sendRequestToServer(packetToServer);
        }
    }

    /**
     * Handle a key release. Not needed by {@link GUIClient}.
     *
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Handle a key being typed. Not needed by {@link GUIClient}.
     *
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyTyped(KeyEvent e) {
    }

    public void sendRequestToServer(MazewarPacket toServer) {

        try {
            // Send request packet to server
            Mazewar.out.writeObject(toServer);
        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
        }
    }

}
