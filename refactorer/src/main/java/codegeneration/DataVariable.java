package codegeneration;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

/**
 * Represents a variable in the source code.
 * The DataVariable is used to check two variable accesses against each other to ensure that they 
 * refer to the same variable.
 * @author michaellynch
 *
 */
public class DataVariable {

    private Type dataType;
    private String dataIdent;

    private boolean usingSharedDataVar;
    private boolean sharedData;

    /**
     * Creates a DataVariable with the given type and identifier.
     * @param dataType The type of the variable being represented.
     * @param dataIdent The identifier of the variable being represented.
     */
    public DataVariable(Type dataType, String dataIdent) {
        this.dataType = dataType;
        this.dataIdent = dataIdent;
        usingSharedDataVar = false;
    }

    /**
     * Creates a DataVariable with the given type and identifer along with information on whether or not this 
     * variable is shared between multiple iterations in the loop.
     * @param dataType The type of the variable being represented.
     * @param dataIdent The identifier of the variable being represented.
     * @param sharedData True if the variable is shared between multiple iterations of the loop.
     */
    public DataVariable(Type dataType, String dataIdent, boolean sharedData) {
        this.dataType = dataType;
        this.dataIdent = dataIdent;
        usingSharedDataVar = true;
        this.sharedData = sharedData;
    }

    /**
     * Creates a DataVariable based on the expression given.
     * @param expr The expression to extract the DataVariable from.
     */
    public DataVariable(NameExpr expr) {
        ClassOrInterfaceType type = new ClassOrInterfaceType(null, 
            expr.resolve().getType().describe());

        this.dataType = type;
        this.dataIdent = expr.getNameAsString();
        usingSharedDataVar = false;
    }

    /**
     * Creates a DataVariable based on the expression given along with information on whether or not this variable
     * is shared between multiple iterations in the loop.
     * @param expr The expression to extract the DataVariable from.
     * @param sharedData True is the variable is shared between multiple iterations of the loop.
     */
    public DataVariable(NameExpr expr, boolean sharedData) {
        ClassOrInterfaceType type = new ClassOrInterfaceType(null, 
        expr.resolve().getType().describe());

        this.dataType = type;
        this.dataIdent = expr.getNameAsString();

        usingSharedDataVar = true;
        this.sharedData = sharedData;
    }

    public boolean isSharedData() {
        return sharedData;
    }


    /**
     * Generates a FieldDeclaration based on the variable information for use in generating the new parallel class.
     * @return A FieldDeclaration with the same information as the stored variable.
     */
    public FieldDeclaration getField() {
        return new FieldDeclaration(Modifier.PUBLIC.toEnumSet(), dataType, dataIdent);
    }

    /**
     * Generates a Parameter based on the variable information for use in generating the new parallel class.
     * @return A Parameter with the same information as the stored variable.
     */
    public Parameter getParameter() {
        Parameter param = new Parameter(dataType, dataIdent);
        return param;
    }

    
    /**
     * Generates an assignment to be used in the new parallel class' constructor.
     * @return A AssignExpr assigning the constructor parameter to the class field.
     */
    public AssignExpr getConstructorAssignment() {
        FieldAccessExpr fieldAccess = new FieldAccessExpr(new ThisExpr(), dataIdent);
        AssignExpr assignExpr = new AssignExpr(fieldAccess, new NameExpr(dataIdent), AssignExpr.Operator.ASSIGN);

        return assignExpr;
    }

    public Type getType() {
        return dataType;
    }

    public String getName() {
        return dataIdent;
    }

    public String toString() {
        return dataType + " " + dataIdent;
    }

    @Override 
    public boolean equals(Object a) {
        if(a instanceof DataVariable) {

            if(dataType.asString().equals(((DataVariable)a).getType().asString()) &&
                dataIdent.equals(((DataVariable)a).getName())) {

                    if(((DataVariable)a).usingSharedDataVar && usingSharedDataVar) {
                        if(((DataVariable)a).sharedData == sharedData) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    return true;
            }
        }
        return false;
    }
}  