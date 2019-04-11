package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import codegeneration.CodeGenerator;
import codegeneration.DataVariable;
import discovery.CodeNavigation;
import discovery.ForLocator;
import discovery.ForLoopSharedDataDetector;
import discovery.ForManager;
import safety.SafetyChecks;
import safety.UnrefactorableException;

/**
 * Main application class
 */
public class App {

    private static ForLocator forLocator;
    private static CompilationUnit userFileCu;

    /**
     * Performs a refactoring of a java file chosen by the user in arguments
     * @param args Gives information to the program regarding the location of source files and other libraries that may be included in the project.
     */
    public static void main(String[] args) {
        System.out.println("Parallel Refactorer 0.1");

        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        if(!argumentHandler.shouldContinueProgram()) return;

        String filename = argumentHandler.getRefactorFile();
        String srcDirectory = argumentHandler.getSrcDirectory();
        String[] jarPaths = argumentHandler.getLibraryPaths();
        

        File userfile = null;
        
        try {
            userfile = new File(filename);
			locateForLoops(userfile, srcDirectory, jarPaths);
		} catch (IOException e) {
			System.out.println("ERROR: Some of the files entered do not exist");
        }

        int lineNumber = getLineNumber();
        int columnNumber = getColumnNumber(lineNumber);

        int forLine = lineNumber;
        int forColumn = columnNumber;

        ForStmt refactoringFor = forLocator.getForManager().getForLoopByLineAndColumn(forLine, forColumn);

        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(refactoringFor);

        List<DataVariable> sharedDataList = sdd.getAllSharedDataVariables();

        DataVariable[] sharedData = sharedDataList.toArray(new DataVariable[sharedDataList.size()]);

        try {
            SafetyChecks sChecks = new SafetyChecks(refactoringFor, sdd);
            sChecks.performSafetyChecks();
            sChecks.displayWarnings();
        } catch(UnrefactorableException e) {
            System.out.println("ERROR: " + e.getMessage());
            return;
        }

        String className = getClosureName();

        CodeGenerator cg = new CodeGenerator(className,
                                forLocator.getForManager().getForLoopByLineAndColumn(forLine,forColumn),
                                sharedData,
                                sdd.getForInitIdentifier());

        CompilationUnit cu = cg.generateParallelForClass();

        LexicalPreservingPrinter.setup(userFileCu);

        userFileCu.addImport("parallel.*");

        int noOfChunks = getAboveZeroNumber("How many chunks should the for loop be split into? ");
        int noOfThreads = getAboveZeroNumber("How many threads should the parallel program run on? ");
        
        BlockStmt replacementBlock = cg.generateReplacementCode(noOfChunks, noOfThreads, sharedDataList);

        String lexCorrectReplaceString = correctIndenting(replacementBlock.toString(), forColumn);

        //ForManager.getClassThatContainsForLoop(refactoringFor).addMember(cu.getClassByName(className).get());
        CodeNavigation.getParentOfTypeFromNode(ClassOrInterfaceDeclaration.class, refactoringFor).addMember(cu.getClassByName(className).get());

        refactoringFor.replace(new ExpressionStmt(new NameExpr(lexCorrectReplaceString)));

        writeToFile(args[0], userFileCu);

        //now sort out shutdown of ParallelExecutor on the main file.
        if(argumentHandler.getMainMethodFile() != null) rewriteMainFile(argumentHandler.getMainMethodFile());
    }

    private static void rewriteMainFile(String filename) {
        FileInputStream in;
		try {
			in = new FileInputStream(filename);

            CompilationUnit mainFile = JavaParser.parse(in);

            NodeList<TypeDeclaration<?>> classesInFile = mainFile.getTypes();
            
            for(TypeDeclaration<?> cl:classesInFile) {
                if(cl instanceof ClassOrInterfaceDeclaration) {
                    List<MethodDeclaration> mainMethods = cl.getMethodsBySignature("main", "String[]");
                    if(mainMethods.size() == 1 && mainMethods.get(0).isStatic()) {
                        MethodDeclaration mainMethod = transformMainMethod(mainMethods.get(0));

                        mainFile.addImport("parallel.*");

                        writeToFile(filename, mainFile);

                        return;
                    }
                }
            }

            System.out.println("ERROR: Main method not found. Shutdown ParallelExecutor manually");

        } catch (FileNotFoundException e) {
            System.out.println("ERROR: Main file not found. Shutdown ParallelExecutor manually.");
        }
    }

    private static MethodDeclaration transformMainMethod(MethodDeclaration mainMethod) {
        BlockStmt methodBlock = mainMethod.getBody().get();


        FieldAccessExpr shutdownExpr = new FieldAccessExpr(new NameExpr("ParallelExecutor"), "shutdown()");

        methodBlock.addStatement(shutdownExpr);

        List<ReturnStmt> allReturnStmts = methodBlock.findAll(ReturnStmt.class);

        BlockStmt coupledExpr = new BlockStmt();
        coupledExpr.addStatement(shutdownExpr);
        coupledExpr.addStatement(new ReturnStmt());

        for(ReturnStmt returnStmt:allReturnStmts) {
            returnStmt.replace(coupledExpr);
        }

        return mainMethod;
    }

    private static void writeToFile(String filename, CompilationUnit userFileCu) {
        try {
            PrintWriter fileOutput = new PrintWriter(new FileWriter(filename));
            fileOutput.print(userFileCu);
            fileOutput.close();
        } catch (IOException e) {
            System.out.println("ERROR: unable to write to source file.");
        }
    }

    private static String correctIndenting(String replacementBlock, int forColumn) {
        String[] replaceString = replacementBlock.split(("\n"));
        String lexCorrectReplaceString = replaceString[0] + "\n";
        String extraSpaces = "";
        for(int i = 0; i < forColumn; i++) {
            extraSpaces += " ";
        }
        for(int i = 1; i < replaceString.length; i++) {
            lexCorrectReplaceString += extraSpaces + replaceString[i];
            if(i != replaceString.length-1) {
                lexCorrectReplaceString += "\n";   
            }
        }

        return lexCorrectReplaceString;
    }

    private static String getClosureName() {
        Scanner in = new Scanner(System.in);

        boolean validClassName = false;
        String className = "Closure";
        while(!validClassName) {
            System.out.print("Give name for newly created class:");
            String userInput = in.next();

            if(userInput.length() == 0) {
                validClassName = true;
            } else if(userInput.matches("[A-Za-z][A-Za-z0-9_]*")) {
                className = userInput;
                validClassName = true;
            }
        }

        return className;
    }

    private static int getAboveZeroNumber(String promptText) {
        Scanner in = new Scanner(System.in);
        int inputNumber;

        while(true) {
            System.out.print(promptText);
            try {
                inputNumber = in.nextInt();

                if(inputNumber < 0) {
                    throw new NumberFormatException();
                }

                break;
            } catch(NumberFormatException e) {
                System.out.println("Please enter a valid integer above 0");
            }
        }

        return inputNumber;

    }

    private static int getColumnNumber(int lineNumber) {
        Scanner in = new Scanner(System.in);
        int columnNumber = -1;
        if(forLocator.getForManager().getForLoopsByLine(lineNumber).size() > 1) {
            boolean columnSet = false;
            while(!columnSet) {
                System.out.print("Choose for loop (column): ");
                try {
                    columnNumber = in.nextInt();
                    if(forLocator.getForManager().getForLoopByLineAndColumn(lineNumber, columnNumber) == null) {
                        throw new Exception();
                    }
                    columnSet = true;
                } catch(Exception e) {
                    System.out.println("Enter a valid for loop column number");
                }
            }
        } else {
            columnNumber = forLocator.getForManager().getForLoopsByLine(lineNumber).get(0).getBegin().get().column;
        }

        return columnNumber;
    }

    private static int getLineNumber() {
        Scanner in = new Scanner(System.in);
        boolean lineSet = false;
        int lineNumber = -1;
        
        
        while(!lineSet) {
            System.out.print("Choose for loop (line): ");
            try {
                lineNumber = in.nextInt();
                if(forLocator.getForManager().getForLoopsByLine(lineNumber).isEmpty()) {
                    throw new Exception();
                }
                lineSet = true;
            } catch(Exception e) {
                System.out.println("Enter a valid for loop line number");
            }
        }

        return lineNumber;
    }

    /**
     * locates all the for loops in the source file and then prints their location to screen.
     * @param filename name of the source file
     */
    private static void locateForLoops(File userfile, String srcDirectory, String[] jarPaths) throws FileNotFoundException, IOException {

        

        FileInputStream in = new FileInputStream(userfile);

        CombinedTypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());

        if(srcDirectory != null) {
            typeSolver.add(new JavaParserTypeSolver(srcDirectory));
        }

        if(jarPaths != null) {
            for(int i = 0; i < jarPaths.length; i++) {
                typeSolver.add(new JarTypeSolver(jarPaths[i]));
            }
        }

        JavaSymbolSolver jss = new JavaSymbolSolver(typeSolver);
        JavaParser.getStaticConfiguration().setSymbolResolver(jss);

        userFileCu = JavaParser.parse(in);

        forLocator = new ForLocator(userFileCu);
        System.out.println("Found For Loops:");
        System.out.println(forLocator.getForManager());
    }
}