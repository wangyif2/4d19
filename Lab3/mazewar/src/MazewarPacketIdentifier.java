import java.io.Serializable;

/**
 * User: Ivan
 * Date: 02/03/13
 */
public class MazewarPacketIdentifier implements Serializable {
    public int lamportClk;
    public String owner;

    @Override
    public boolean equals(Object id) {
        return (this.lamportClk == ((MazewarPacketIdentifier)id).lamportClk && this.owner.equals(((MazewarPacketIdentifier)id).owner));
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 31 * hash + lamportClk;
        hash = 31 * hash + owner.hashCode();
        return hash;
    }
}
