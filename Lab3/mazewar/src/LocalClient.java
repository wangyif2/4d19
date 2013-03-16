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

import java.awt.event.KeyListener;

/**
 * An abstract class for {@link Client}s in a {@link Maze} that local to the
 * computer the game is running upon. You may choose to implement some of
 * your code for communicating with other implementations by overriding
 * methods in {@link Client} here to intercept upcalls by {@link GUIClient} and
 * {@link RobotClient} and generate the appropriate network events.
 *
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: LocalClient.java 343 2004-01-24 03:43:45Z geoffw $
 */


public abstract class LocalClient extends Client implements KeyListener {

    protected boolean pause = false;

    /**
     * Create a {@link Client} local to this machine.
     *
     * @param name The name of this {@link Client}.
     */
    public LocalClient(String name) {
        super(name);
        assert (name != null);
    }

    public void registerMaze(Maze maze) {
        super.registerMaze(maze);
    }

    protected void pause() {
        pause = true;
    }

    protected void resume() {
        pause = false;
    }

    protected void quit() {
        Mazewar.quit();
    }

    /**
     * Notify connected clients adding me.
     */
    protected void notifyAdd() {
        // Wait until all existing clients have reported their locations
        while (Mazewar.maze.getNumOfClients() < Mazewar.connectedOuts.size()) try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Clear queue
        Mazewar.actionQueue.clear();

        MazewarPacket outgoing = new MazewarPacket();
        outgoing.type = MazewarPacket.ADD;
        outgoing.directedPoint = Mazewar.maze.addLocalClient(this);

        multicastAction(outgoing);

    }

    /**
     * Notify connected clients moving the client forward.
     *
     * @return <code>true</code> if move was successful, otherwise <code>false</code>.
     */
    protected boolean notifyForwardAction() {
        assert (maze != null);

        if (maze.canMoveForward(this)) {
            MazewarPacket outgoing = new MazewarPacket();
            outgoing.type = MazewarPacket.MOVE_FORWARD;

            multicastAction(outgoing);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Notify connected clients moving the client backward.
     *
     * @return <code>true</code> if move was successful, otherwise <code>false</code>.
     */
    protected boolean notifyBackupAction() {
        assert (maze != null);

        if (maze.canMoveBackward(this)) {
            MazewarPacket outgoing = new MazewarPacket();
            outgoing.type = MazewarPacket.MOVE_BACKWARD;

            // Multicast the moving backward action
            multicastAction(outgoing);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Notify connected clients turning the client left.
     */
    protected void notifyTurnLeftAction() {
        MazewarPacket outgoing = new MazewarPacket();
        outgoing.type = MazewarPacket.TURN_LEFT;

        // Multicast the turning left action
        multicastAction(outgoing);

    }

    /**
     * Notify connected clients turning the client right.
     */
    protected void notifyTurnRightAction() {
        MazewarPacket outgoing = new MazewarPacket();
        outgoing.type = MazewarPacket.TURN_RIGHT;

        // Multicast the turning right action
        multicastAction(outgoing);

    }

    /**
     * Notify server the client fired.
     */
    protected boolean notifyFireAction() {
        assert (maze != null);

        if (maze.canFire(this)) {
            MazewarPacket outgoing = new MazewarPacket();
            outgoing.type = MazewarPacket.FIRE;

            // Multicast the fire action
            multicastAction(outgoing);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Notify connected clients a kill.
     */
    protected void notifyKill(String victim, DirectedPoint newDp, boolean isInstant) {
        assert (maze != null);

        MazewarPacket outgoing = new MazewarPacket();
        outgoing.type = isInstant ? MazewarPacket.INSTANT_KILL : MazewarPacket.KILL;
        outgoing.victim = victim;
        outgoing.directedPoint = newDp;

        // Multicast the kill action
        multicastAction(outgoing);

    }

    protected void notifyQuit() {
        assert (maze != null);

        MazewarPacket outgoing = new MazewarPacket();
        outgoing.type = MazewarPacket.QUIT;

        // Multicast the turning right action
        multicastAction(outgoing);

    }

    private void multicastAction(MazewarPacket outgoing) {
        // Multicast add action
        Mazewar.multicaster.multicastAction(outgoing);

        // Multicast ACK to all clients
        Mazewar.multicaster.multicastACK(outgoing);
    }
}
