package discovery;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.Position;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OrderedPositionListTest {

    private OrderedPositionList opl;

    @Test 
    public void addOrdering() {
        opl = new OrderedPositionList();

        Position pos1 = new Position(10,10);
        Position pos2 = new Position(11,1);

        opl.add(pos2);
        opl.add(pos1);
        
        assertTrue(opl.get(0) == pos1);
        assertTrue(opl.get(1) == pos2);
    }

    @Test
    public void clearing() {
        opl = new OrderedPositionList();

        opl.add(new Position(1,1));
        opl.add(new Position(2,2));
        opl.add(new Position(3,3));

        assertTrue(opl.size() == 3);

        opl.clear();

        assertTrue(opl.size() == 0);
    }

    @Test
    public void incorrectPosition() {
        opl = new OrderedPositionList();

        Position pos = opl.getPositionByLineAndColumn(10, 11);

        assertNull(pos);
    }

    @Test
    public void correctPosition() {
        opl = new OrderedPositionList();

        opl.add(new Position(10,10));
        Position pos = opl.getPositionByLineAndColumn(10, 10);

        assertNotNull(pos);
    }
    
    @Test
    public void sameLinePositions() {
        opl = new OrderedPositionList();

        Position pos1 = new Position(10,10);
        Position pos2 = new Position(10, 15);
        Position pos3 = new Position(12,0);

        opl.add(pos1);
        opl.add(pos2);
        opl.add(pos3);

        assertTrue(opl.getPositionsByLine(10).size() == 2);
    }

    @Test
    public void sameLineOrder() {
        opl = new OrderedPositionList();

        Position pos1 = new Position(10, 10);
        Position pos2 = new Position(10, 15);

        opl.add(pos2);
        opl.add(pos1);

        assertTrue(opl.get(0) == pos1);
        assertTrue(opl.get(1) == pos2);
    }
}