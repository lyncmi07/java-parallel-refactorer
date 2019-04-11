package safety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.Node;

public class WarningManager {
    private List<Warning> unorderedWarnings;
    private List<Warning> positionedWarnings;
    private Map<String, List<Warning>> classedWarnings;


    public WarningManager() {
        unorderedWarnings = new ArrayList<>();
        positionedWarnings = new ArrayList<>();
        classedWarnings = new HashMap<>();
    }

    public void storeWarning(String message) {
        unorderedWarnings.add(new Warning(message));
    }
    public void storeWarning(String message, Node node) {
        Warning warn = new Warning(message, node);
        String classname = warn.getClassname();
        if(classedWarnings.containsKey(classname)) {
            insertIntoList(classedWarnings.get(classname), warn);
        } else {
            List<Warning> warningList = new ArrayList<>();
            warningList.add(warn);
            classedWarnings.put(classname, warningList);
        }
    }

    public void storeWarning(String message, int line, int column) {
        Warning warn = new Warning(message, line, column);

        insertIntoList(positionedWarnings, warn);
    }

    private void insertIntoList(List<Warning> list, Warning warning) {
        for(int i = 0; i < list.size()-1; i++) {
            /*if(list.get(i).getPosition().isBefore(warning.getPosition())) {
                System.out.println("ADDING WARNING HERE");
                list.add(i+1, warning);
                return;
            }*/

            if(list.get(i).getPosition().isAfter(warning.getPosition())) {
                list.add(i, warning);
                return;
            }

            if(list.get(i).getPosition().isBefore(warning.getPosition()) && 
                list.get(i+1).getPosition().isAfter(warning.getPosition())) {
                list.add(i+1, warning);
                return;
            }
        }

        System.out.println("ADDING WARNING AT END");
        list.add(warning);
    }

    public void printWarnings() {
        for(Warning warn:unorderedWarnings) {
            System.out.println(warn);
        }
        for(Warning warn:positionedWarnings) {
            System.out.println(warn);
        }

        Set<String> classKeySet = classedWarnings.keySet();

        for(String key:classKeySet) {
            System.out.println("Warnings in class " + key);
            List<Warning> warningList = classedWarnings.get(key);
            for(Warning warn:warningList) {
                System.out.println(warn);
            }
        }
    }
}