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

import java.awt.event.KeyListener;
import java.io.IOException;

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
    private static final Logger logger = LoggerFactory.getLogger(LocalClient.class);

    private LocalUpdateHandler localUpdate;

    /**
     * Create a {@link Client} local to this machine.
     *
     * @param name The name of this {@link Client}.
     */
    public LocalClient(String name) {
        super(name);
        assert (name != null);
        localUpdate = new LocalUpdateHandler();
    }

    public void start() {
        localUpdate.start();
    }

    public void registerMaze(Maze maze) {
        super.registerMaze(maze);
        localUpdate.registerMaze(maze);
    }

    /**
     * Notify server a kill.
     */
    protected void kill(String victim, DirectedPoint newDp) {
        notifyServerKill(victim, newDp, false);
        logger.info("Notify client: " + getName() + " killed " + victim +
                "\n\t reSpawning at location " + newDp.getX() + " " + newDp.getY() + " " +
                newDp.getDirection() + "\n");
    }

    /**
     * Notify server moving the client forward.
     *
     * @return <code>true</code> if move was successful, otherwise <code>false</code>.
     */
    protected boolean notifyServerForward() {
        assert (maze != null);

        if (maze.canMoveForward(this)) {
            Point oldPoint = getPoint();
            Direction d = getOrientation();
            DirectedPoint newDp = new DirectedPoint(oldPoint.move(d), d);

            MazewarPacket toServer = new MazewarPacket();
            toServer.owner = getName();
            toServer.type = MazewarPacket.MOVE_FORWARD;
            toServer.mazeMap.put(getName(), newDp);
            notifyServer(toServer);
            logger.info("Notify moveClient: " + getName() +
                    "\n\tfrom X: " + oldPoint.getX() +
                    "\n\t     Y: " + oldPoint.getY() +
                    "\n\tto   X: " + newDp.getX() +
                    "\n\t     Y: " + newDp.getY() +
                    "\n\t     orientation : " + newDp.getDirection() + "\n");
            return true;
        } else {
            logger.info(getName() + " Cannot move forward!\n");
            return false;
        }
    }

    /**
     * Notify server moving the client backward.
     *
     * @return <code>true</code> if move was successful, otherwise <code>false</code>.
     */
    protected boolean notifyServerBackup() {
        assert (maze != null);

        if (maze.canMoveBackward(this)) {
            Point oldPoint = getPoint();
            Direction d = getOrientation();
            DirectedPoint newDp = new DirectedPoint(oldPoint.move(d.invert()), d);

            MazewarPacket toServer = new MazewarPacket();
            toServer.owner = getName();
            toServer.type = MazewarPacket.MOVE_BACKWARD;
            toServer.mazeMap.put(getName(), newDp);
            notifyServer(toServer);
            logger.info("Notify moveClient: " + getName() +
                    "\n\tfrom X: " + oldPoint.getX() +
                    "\n\t     Y: " + oldPoint.getY() +
                    "\n\tto   X: " + newDp.getX() +
                    "\n\t     Y: " + newDp.getY() +
                    "\n\t     orientation : " + newDp.getDirection() + "\n");
            return true;
        } else {
            logger.info(getName() + " Cannot move backward!\n");
            return false;
        }
    }

    /**
     * Notify server turning the client left.
     *
     * @return <code>true</code> if move was successful, otherwise <code>false</code>.
     */
    protected void notifyServerTurnLeft() {
        MazewarPacket toServer = new MazewarPacket();

        Point oldPoint = getPoint();
        Direction d = getOrientation();
        DirectedPoint newDp = new DirectedPoint(oldPoint, d.turnLeft());

        toServer.owner = getName();
        toServer.type = MazewarPacket.TURN_LEFT;
        toServer.mazeMap.put(getName(), newDp);
        notifyServer(toServer);
        logger.info("Nofity rotateClient: " + getName() +
                "\n\tfrom X: " + oldPoint.getX() +
                "\n\t     Y: " + oldPoint.getY() +
                "\n\t     orientation: " + d +
                "\n\tto   X: " + newDp.getX() +
                "\n\tto   Y: " + newDp.getY() +
                "\n\t     orientation : " + newDp.getDirection() + "\n");
    }

    /**
     * Notify server turning the client right.
     *
     * @return <code>true</code> if move was successful, otherwise <code>false</code>.
     */
    protected void notifyServerTurnRight() {
        MazewarPacket toServer = new MazewarPacket();

        Point oldPoint = getPoint();
        Direction d = getOrientation();
        DirectedPoint newDp = new DirectedPoint(oldPoint, d.turnLeft());

        toServer.owner = getName();
        toServer.type = MazewarPacket.TURN_RIGHT;
        toServer.mazeMap.put(getName(), newDp);
        notifyServer(toServer);
        logger.info("Nofity rotateClient: " + getName() +
                "\n\tfrom X: " + oldPoint.getX() +
                "\n\t     Y: " + oldPoint.getY() +
                "\n\t     orientation: " + d +
                "\n\tto   X: " + newDp.getX() +
                "\n\tto   Y: " + newDp.getY() +
                "\n\t     orientation : " + newDp.getDirection() + "\n");
    }

    /**
     * Notify server the client fired.
     *
     * @return <code>true</code> if move was successful, otherwise <code>false</code>.
     */
    protected void notifyServerFire() {
        assert (maze != null);

        MazewarPacket toServer = new MazewarPacket();
        toServer.owner = getName();
        Cell newCell = maze.canFire(this);
        if (newCell == null) {
            logger.info(getName() + " Cannot fire!\n");
            return;
        }

        Object contents = newCell.getContents();
        if (contents != null) {
            // Check if instant kill happens
            DirectedPoint newDp = maze.canKill(contents);
            if (newDp != null) {
                String victim = ((Client) contents).getName();
                notifyServerKill(victim, newDp, true);
                logger.info("Notify client: " + getName() + " instantly killed " + victim +
                        "\n\t reSpawning at location " + newDp.getX() + " " + newDp.getY() + " " +
                        newDp.getDirection() + "\n");
            } else {
                logger.info(getName() + " cancels projectile!\n");
            }
        } else {
            toServer.type = MazewarPacket.FIRE;
            notifyServer(toServer);
            logger.info("Notify client: " + getName() + " fired\n");
        }
    }

    protected void notifyServerQuit() {
        assert (maze != null);
        MazewarPacket toServer = new MazewarPacket();

        toServer.owner = getName();
        toServer.type = MazewarPacket.QUIT;

        notifyServer(toServer);
        logger.info("Notify server quiting\n");
    }

    private void notifyServerKill(String victim, DirectedPoint newDp, boolean isInstant) {
        MazewarPacket toServer = new MazewarPacket();
        toServer.type = isInstant ? MazewarPacket.INSTANT_KILL : MazewarPacket.KILL;
        toServer.owner = getName();
        toServer.victim = victim;
        toServer.mazeMap.put(victim, newDp);
        notifyServer(toServer);
    }

    private void notifyServer(MazewarPacket toServer) {
        synchronized (Mazewar.out) {
            try {
                Mazewar.out.writeObject(toServer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
