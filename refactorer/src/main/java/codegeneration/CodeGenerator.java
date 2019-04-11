package codegeneration;

import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;

/**
 * Used to generate the parallel code equivalent of the given sequential program
 * @author michaellynch
 *
 */
public class CodeGenerator {

    private ForStmt loopStatement;
    private DataVariable[] dataVariable;
    private String className;
    private String forInitIdentifier;

    /**
     * Prepares the CodeGenerator with the required data for generating parallel code.
     * @param className The name of the class extending the parallel pattern to be created.
     * @param loopStatement The loop statement that is to be refactored.
     * @param dataVariable List of variables in the loop that are created externally from the loop but are used in the loop.
     * @param forInitIdentifier The identfier string of the loop's iterator.
     */
	public CodeGenerator(String className, ForStmt loopStatement, DataVariable[] dataVariable, String forInitIdentifier) {
        this.className = className;
        this.loopStatement = loopStatement;
        this.dataVariable = dataVariable;
        this.forInitIdentifier = forInitIdentifier;
    }

	/**
	 * Generates a class that extends a parallel pattern to be included into the user's source code.
	 * @return An AST representation of the class.
	 */
    public CompilationUnit generateParallelForClass() {
        CompilationUnit cu = new CompilationUnit();
        ClassOrInterfaceDeclaration cl = cu.addClass(className, Modifier.PRIVATE,  Modifier.STATIC);
        cl.addExtendedType("ParallelForFarmTask");
        //cl.addField("int", "field", Modifier.PUBLIC);
        addDataVariableAsFields(cl);
        generateConstructor(cl);
        generateOperationMethod(cl);

        //ClassOrInterfaceType cit = new ClassOrInterfaceType(new ClassOrInterfaceType(null, "java.util"), "String");

        //System.out.println(cit);

        return cu;
    }

    private void addDataVariableAsFields(ClassOrInterfaceDeclaration cl) {
        for(DataVariable data:dataVariable) {
            cl.addMember(data.getField());
        }
    }

    private NodeList<Parameter> generateConstructorParameters() {
        NodeList<Parameter> allParams = new NodeList<>();

        //DataVariable rangeStart = new DataVariable(, dataIdent)
        Parameter rangeStart = new Parameter(new PrimitiveType(PrimitiveType.Primitive.INT), "rangeStart");
        allParams.add(rangeStart);
        Parameter rangeEnd = new Parameter(new PrimitiveType(PrimitiveType.Primitive.INT), "rangeEnd");
        allParams.add(rangeEnd);
        Parameter noOfChunks = new Parameter(new PrimitiveType(PrimitiveType.Primitive.INT), "noOfChunks");
        allParams.add(noOfChunks);

        for(DataVariable data:dataVariable) {
            allParams.add(data.getParameter());
        }

        return allParams;
    }

    private void generateConstructor(ClassOrInterfaceDeclaration cl) {
        ConstructorDeclaration cd = new ConstructorDeclaration(className);
        cd.setParameters(generateConstructorParameters());

        cd.setBody(generateConstructorBody());

        cl.addMember(cd);
    }

    private BlockStmt generateConstructorBody() {
        NodeList<Statement> statements = new NodeList<>();

        statements.add(
            new ExpressionStmt(
                new MethodCallExpr(
                    "super",
                    new NameExpr("rangeStart"),
                    new NameExpr("rangeEnd"),
                    new NameExpr("noOfChunks")
                )
            )
        );

        for(DataVariable data:dataVariable) {
            statements.add(new ExpressionStmt(data.getConstructorAssignment()));
        }

        BlockStmt block = new BlockStmt(statements);

        return block;
    }

    private void generateOperationMethod(ClassOrInterfaceDeclaration cl) {
        MethodDeclaration operationMethod = cl.addMethod("operation", Modifier.PUBLIC);
        operationMethod.addAndGetAnnotation("Override");
        operationMethod.addParameter("int", "rangeStart");
        operationMethod.addParameter("int", "rangeEnd");
        generateOperationForLoop(operationMethod);
    }

    /**
     * Generates the code that is to replace the loop in the user's code.
     * @param noOfChunks	The number of chunks that the loop is to be split into when parallelised
     * @param noOfThreads	The number of threads that the parallel loop is going to be run on.
     * @param sharedDataList	A list of variables that are created outside the parallelised loop but need to be used within.
     * @return An AST representation of the code that replaces the loop to be parallelised.
     */
    public BlockStmt generateReplacementCode(int noOfChunks, int noOfThreads, List<DataVariable> sharedDataList) {
        NodeList<Statement> statements = new NodeList<>();
        statements.add(generateMethodCall(noOfChunks, noOfThreads, sharedDataList));
        statements.addAll(generateFinishingAssignments(sharedDataList));

        return new BlockStmt(statements);
    }
    
    private ExpressionStmt generateMethodCall(int noOfChunks, int noOfThreads, List<DataVariable> sharedDataList) {
        NodeList<Expression> arguments = new NodeList<>();
        arguments.add(getRangeStart());
        arguments.add(getRangeEnd());
        arguments.add(new IntegerLiteralExpr(noOfChunks));
        for(int i = 0; i < sharedDataList.size(); i++) {
            arguments.add(new NameExpr(sharedDataList.get(i).getName()));
        }

        ExpressionStmt statement = new ExpressionStmt(
            new VariableDeclarationExpr(
                new VariableDeclarator(
                    new ClassOrInterfaceType(null, className),
                    new SimpleName("returnData"),
                    new MethodCallExpr(
                        "ParallelExecutor.executeParallel",
                        new ObjectCreationExpr(
                            null, 
                            new ClassOrInterfaceType(null, className), 
                            arguments
                        ),
                        new IntegerLiteralExpr(noOfThreads)
                    )
                )
            )
        );

        return statement;
    }

    private NodeList<Statement> generateFinishingAssignments(List<DataVariable> sharedDataList) {
        NodeList<Statement> allAssignments = new NodeList<>();

        for(int i = 0; i < sharedDataList.size(); i++) {
            allAssignments.add(
                new ExpressionStmt(
                    new AssignExpr(
                        new NameExpr(sharedDataList.get(i).getName()),
                        new FieldAccessExpr(new NameExpr("returnData"), sharedDataList.get(i).getName()),
                        AssignExpr.Operator.ASSIGN
                    )
                )
            );
        }
        
        return allAssignments;
    }

    private Expression getRangeStart() {
        NodeList<Expression> allInitExprs = loopStatement.getInitialization();
        if(allInitExprs.get(0).isVariableDeclarationExpr()) {
            return allInitExprs.get(0).asVariableDeclarationExpr().getVariable(0).getInitializer().get();
        }

        throw new RuntimeException("For loop initialiser not valid");
        //return null;
    }
    private Expression getRangeEnd() {
        BinaryExpr compareExpr = loopStatement.getCompare().get().asBinaryExpr();
        
        //TODO: maybe allow for <= and >= by adding a +1 to rangeEnd

        if(compareExpr.getLeft().isNameExpr() && 
            compareExpr.getLeft().asNameExpr().getNameAsString().equals(forInitIdentifier) &&
            compareExpr.getOperator() == BinaryExpr.Operator.LESS) {
            return compareExpr.getRight();
        } else if(compareExpr.getRight().isNameExpr() &&
            compareExpr.getRight().asNameExpr().getNameAsString().equals(forInitIdentifier) &&
            compareExpr.getOperator() == BinaryExpr.Operator.GREATER) {
            return compareExpr.getLeft();
        }

        throw new RuntimeException("For loop compare not valid");
    }

    private void generateOperationForLoop(MethodDeclaration operationMethod) {
        ForStmt forLoop = new ForStmt();
        //int i = rangeStart
        VariableDeclarationExpr iteratorDecl = new VariableDeclarationExpr(
                                        new VariableDeclarator(new PrimitiveType(PrimitiveType.Primitive.INT), 
                                                                forInitIdentifier, 
                                                                new NameExpr("rangeStart")));
        forLoop.setInitialization(encapsulateInNodeList((Expression)iteratorDecl));

        //i < rangeEnd
        BinaryExpr exitExpr = new BinaryExpr(new NameExpr(forInitIdentifier), 
                                                new NameExpr("rangeEnd"), 
                                                BinaryExpr.Operator.LESS);
        forLoop.setCompare(exitExpr);

        //i++
        UnaryExpr update = new UnaryExpr(new NameExpr(forInitIdentifier), UnaryExpr.Operator.POSTFIX_INCREMENT);
        forLoop.setUpdate(encapsulateInNodeList((Expression)update));
        
        forLoop.setBody(loopStatement.getBody());

        operationMethod.setBody(encapsulateInBlockStmt(forLoop));
    }

    private <N extends Node> NodeList<N> encapsulateInNodeList(N node) {
        NodeList<N> nodeList = new NodeList<>();
        nodeList.add(node);
        return nodeList;
    }

    private BlockStmt encapsulateInBlockStmt(Statement stmt) {
        NodeList<Statement> stmts = new NodeList<>();
        stmts.add(stmt);
        //BlockStmt blockStmt = new BlockStmt
        return new BlockStmt(stmts);
    }
}