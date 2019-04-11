package discovery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.javaparser.Position;
import com.github.javaparser.ast.stmt.ForStmt;

/**
 * Holds lexical positions in their order in the source code
 */
public class OrderedPositionList implements Iterable<Position> {
    List<Position> orderedPositions;
    
    /**
     * Constructs a new OrderedPositionList without any positions.
     */
    public OrderedPositionList() {
        orderedPositions = new ArrayList<>();
    }

    /**
     * Adds a new position to the list inserting it into the correct position.
     * @param pos new position to be added
     */
    public void add(Position pos) {
        for(int i = 0; i < orderedPositions.size(); i++) {
            if(orderedPositions.get(i).line > pos.line) {
                orderedPositions.add(i, pos);
                return;
            }
            if(orderedPositions.get(i).line == pos.line && orderedPositions.get(i).column > pos.column) {
                orderedPositions.add(i,pos);
                return;
            } 
        }
        orderedPositions.add(pos);
    }

    /**
     * Clears all positions from the list.
     */
    public void clear() {
        orderedPositions.clear();
    }

	@Override
	public Iterator<Position> iterator() {
		return orderedPositions.iterator();
    }
    
    /**
     * Gets a position at the index in the list.
     * @param index index of the position in the list
     * @return the position at the given index
     */
    public Position get(int index) {
        return orderedPositions.get(index);
    }

    /**
     * Get number of positions present in the list
     * @return number of positions in the list
     */
    public int size() {
        return orderedPositions.size();
    }

    /**
     * Gets all positions present on the given line.
     * @param line given line for positions
     * @return all recorded positions present on the given line
     */
    public List<Position> getPositionsByLine(int line) {
        List<Position> allPositions = new ArrayList<>();
        for(Position position:orderedPositions) {
            if(position.line == line) {
                allPositions.add(position);
            }
        }

        return allPositions;
    }

    /**
     * Gets a specific position based on both line and column.
     * @param line given line for position
     * @param column given column for position
     * @return the position for line and position
     */
    public Position getPositionByLineAndColumn(int line, int column) {
        for(Position position:orderedPositions) {
            if(position.line == line && position.column == column) {
                return position;
            }
        }

        return null;
    }
}