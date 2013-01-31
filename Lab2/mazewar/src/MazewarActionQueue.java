import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * User: Ivan
 * Date: 31/01/13
 */
public class MazewarActionQueue implements Queue<MazewarAction> {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<MazewarAction> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(MazewarAction mazewarAction) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends MazewarAction> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean offer(MazewarAction mazewarAction) {
        return false;
    }

    @Override
    public MazewarAction remove() {
        return null;
    }

    @Override
    public MazewarAction poll() {
        return null;
    }

    @Override
    public MazewarAction element() {
        return null;
    }

    @Override
    public MazewarAction peek() {
        return null;
    }
}
