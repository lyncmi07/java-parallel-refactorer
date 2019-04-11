package app;

/**
 * Interprets the arguments given to the program and sorts them for understanding by the program.
 * @author michaellynch
 *
 */
public class ArgumentHandler {

    private String refactorFile = null;
    private String srcDir = null;
    private String mainMethodFile = null;
    private String[] libPaths = null;

    private boolean runProgram;

    /**
     * Interprets the given arguments and fills the fields of the class.
     * @param args Program arguments given by the user.
     */
    public ArgumentHandler(String[] args) {
        runProgram = true;
        for(int i = 0; (i < args.length) && runProgram; i++) {
            if(i == 0) {
                refactorFile = args[0];
            }

            /*if(args[i].equals("--help")) {
                printHelp();
                runProgram = false;
            } */

            switch(args[i]) {
            case "--help":
                printHelp();
                runProgram = false;
                break;
            case "--srcdir":
                i++;
                srcDir = args[i];
                break;
            case "--mainfile":
                i++;
                mainMethodFile = args[i];
                break;
            case "--libs":
                i++;
                i += getLibPaths(args, i);
                break;
            }
        }
    }

    public String getRefactorFile() {
        return refactorFile;
    }

    public String getSrcDirectory() {
        return srcDir;
    }

    public String getMainMethodFile() {
        return mainMethodFile;
    }

    public String[] getLibraryPaths() {
        return libPaths;
    }

    public boolean shouldContinueProgram() {
        return runProgram;
    }

    /**
     * @return The number of arguments need to be skipped before another control argument is found
     */
    private int getLibPaths(String[] args, int startIndex) {
        int noOfLibs = 0;
        for(int i = startIndex; i < args.length; i++) {
            if(args[i].startsWith("--")) {
                break;
            } else {
                noOfLibs++;
            }
        }

        libPaths = new String[noOfLibs];

        for(int i = startIndex; i < (startIndex + noOfLibs); i++) {
            libPaths[i - startIndex] = args[i];
        }

        return noOfLibs;
    }

    /**
     * Prints the help on correct argument syntax directly to console.
     */
    public void printHelp() {
        System.out.println("java -jar parallel_refactor.jar <file to refactor> [arguments]");
        System.out.println("Arguments:");
        System.out.println("--srcdir <source directory> Set the path to the root source director of this java project.");
        System.out.println("--mainfile <path to main file> Set the path to the class with this project's main method.\n When provided, the exit points in the program will have shutdowns to the ParallelExecutor added.");
        System.out.println("--libs [<path to library>] Give a list of libraries that are used in the project. This is required for the safety checker to accurately identify symbols in the program.");
    }
}