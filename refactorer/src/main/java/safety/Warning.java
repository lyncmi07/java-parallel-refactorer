package safety;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import discovery.CodeNavigation;

public class Warning {

    public enum WarningType {
        MESSAGE_ONLY,
        MESSAGE_WITH_NODE,
        MESSAGE_WITH_POSITION
    }

    String message;
    //Node node;
    String classname;
    Position position;
    WarningType warningType;

    public WarningType getWarningType() {
        return warningType;
    }
    public String getMessage() {
        return message;
    }
    public String getClassname() {
        return classname;
    }
    public Position getPosition() {
        return position;
    }

    public Warning(String message) {
        this.message = message;
        warningType = WarningType.MESSAGE_ONLY;
    }

    public Warning(String message, Node node) {
        this.message = message;
        this.position = node.getBegin().get();
        this.classname = CodeNavigation.getParentOfTypeFromNode(ClassOrInterfaceDeclaration.class, node).getNameAsString();
        warningType = WarningType.MESSAGE_WITH_NODE;
    }

    public Warning(String message, int line, int column) {
        this.message = message;
        this.position = new Position(line, column);
        warningType = WarningType.MESSAGE_WITH_POSITION;
    }

    public String toString() {
        switch(warningType) {
        case MESSAGE_ONLY:
            return "WARNING: " + message;
        case MESSAGE_WITH_NODE:
            return "WARNING: Line " + position.line + " Column " + position.column + " in class " + classname + ": " + message;
        case MESSAGE_WITH_POSITION:
            return "WARNING: Line " + position.line + " Column " + position.column + ": " + message;
        default:
            return "WARNING: " + message;
        }
    }
}