package discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.ForStmt;

/**
 * Stores for loop statements with their position in the source file.
 */
public class ForManager {
    private Map<Position, ForStmt> allForLoops;

    private OrderedPositionList orderedPositions;

    /**
     * Constructs a new ForManager with no loops.
     */
    public ForManager() {
        allForLoops = new HashMap<>();
        orderedPositions = new OrderedPositionList();
    }

    /**
     * Adds a for loop statement to the manager.
     * @param forStmt for loop to add
     */
    public void addForLoop(ForStmt forStmt) {
        allForLoops.put(forStmt.getBegin().get(), forStmt);
        orderedPositions.add(forStmt.getBegin().get());
    }

    @Override
    public String toString() {
        String fullString = "";
        for(Position position:orderedPositions) {
            fullString += "for loop at line " + position.line + " column " + position.column + "\n";
        }

        return fullString;
    }

    /**
     * Get all for statements that are present on a specific line.
     * @param line line of the for statement
     * @return a list of for statements present on that line
     */
    public List<ForStmt> getForLoopsByLine(int line) {
        List<ForStmt> allLoops = new ArrayList<>();
        List<Position> positionsOnLine = orderedPositions.getPositionsByLine(line);

        for(Position position:positionsOnLine) {
            allLoops.add(allForLoops.get(position));
        }

        return allLoops;
    }

    /**
     * Get a specific for statement based on its line and column
     * @param line line of the for loop
     * @param column column on the line of the for loop
     * @return an individual for loop or null if no such loop is present
     */
    public ForStmt getForLoopByLineAndColumn(int line, int column) {
        Position position = orderedPositions.getPositionByLineAndColumn(line, column);
        
        if(position == null) {
            return null;
        }
        return allForLoops.get(position);
    }

    private static ClassOrInterfaceDeclaration getClassThatContainsNode(Node node) {
        if(node instanceof ClassOrInterfaceDeclaration) {
            return (ClassOrInterfaceDeclaration)node;
        } else {
            return getClassThatContainsNode(node.getParentNode().get());
        }
    }
}