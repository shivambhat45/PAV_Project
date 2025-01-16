//import soot.jimple.AssignStmt;
//import soot.jimple.Stmt;
//import soot.jimple.ArrayRef;
//import soot.Value;
//import java.util.*;
//
//public class PointsToLatticeElement implements LatticeElement {
//
//    private final HashMap<String, HashSet<String>> state;
//    private static int newArrayCounter = 0;
//
//    public HashMap<String, HashSet<String>> getState() {
//        return state;
//    }
//    public PointsToLatticeElement() {
//        this.state = new HashMap<>();
//    }
//
//    public PointsToLatticeElement(HashMap<String, HashSet<String>> state) {
//        this.state = new HashMap<>(state);
//    }
//
//    private boolean isRelevantVariable(String var) {
//        // Only track array references (variables starting with 'r')
//        return var.startsWith("r");
//    }
//
//    @Override
//    public LatticeElement join_op(LatticeElement r) {
//        PointsToLatticeElement other = (PointsToLatticeElement) r;
//        HashMap<String, HashSet<String>> newState = new HashMap<>();
//
//        Set<String> allKeys = new HashSet<>();
//        allKeys.addAll(this.state.keySet());
//        allKeys.addAll(other.state.keySet());
//
//        for (String key : allKeys) {
//            if (!isRelevantVariable(key)) continue;
//
//            HashSet<String> merged = new HashSet<>();
//            if (this.state.containsKey(key)) {
//                merged.addAll(this.state.get(key));
//            }
//            if (other.state.containsKey(key)) {
//                merged.addAll(other.state.get(key));
//            }
//            if (!merged.isEmpty()) {
//                newState.put(key, merged);
//            }
//        }
//
//        return new PointsToLatticeElement(newState);
//    }
//
//    @Override
//    public boolean equals(LatticeElement r) {
//        if (!(r instanceof PointsToLatticeElement)) return false;
//        PointsToLatticeElement other = (PointsToLatticeElement) r;
//
//        // Compare only relevant variables
//        Map<String, HashSet<String>> thisRelevant = new HashMap<>();
//        Map<String, HashSet<String>> otherRelevant = new HashMap<>();
//
//        for (Map.Entry<String, HashSet<String>> entry : this.state.entrySet()) {
//            if (isRelevantVariable(entry.getKey())) {
//                thisRelevant.put(entry.getKey(), entry.getValue());
//            }
//        }
//
//        for (Map.Entry<String, HashSet<String>> entry : other.state.entrySet()) {
//            if (isRelevantVariable(entry.getKey())) {
//                otherRelevant.put(entry.getKey(), entry.getValue());
//            }
//        }
//
//        return thisRelevant.equals(otherRelevant);
//    }
//
//    @Override
//    public LatticeElement tf_assignstmt(Stmt stmt, int upperBound) {
//        if (!(stmt instanceof AssignStmt)) return this;
//
//        AssignStmt assignStmt = (AssignStmt) stmt;
//        Value leftOp = assignStmt.getLeftOp();
//        Value rightOp = assignStmt.getRightOp();
//        String left = leftOp.toString();
//        String right = rightOp.toString();
//
//        // Create new state without copying the old state for the target variable
//        PointsToLatticeElement newElement = new PointsToLatticeElement();
//        for (Map.Entry<String, HashSet<String>> entry : state.entrySet()) {
//            if (!entry.getKey().equals(left)) {
//                newElement.state.put(entry.getKey(), new HashSet<>(entry.getValue()));
//            }
//        }
//
//        // Skip if left operand is not a relevant variable
//        if (!isRelevantVariable(left)) return newElement;
//
//        // Handle array allocation
//        if (right.contains("newarray")) {
//            HashSet<String> pointsTo = new HashSet<>();
//            pointsTo.add("new" + String.format("%02d", newArrayCounter++));
//            newElement.state.put(left, pointsTo);
//        }
//        // Handle variable assignment (r2 = r0 or r2 = r1)
//        else if (right.startsWith("r")) {
//            HashSet<String> pointsTo = new HashSet<>();
//            pointsTo.add(right);
//            newElement.state.put(left, pointsTo);
//        }
//
//        return newElement;
//    }
//
//    @Override
//    public LatticeElement tf_condstmt(boolean b, Stmt st, int upperBound) {
//        return this; // Points-to analysis is flow-insensitive
//    }
//
//    @Override
//    public LatticeElement transfer(Stmt stmt, boolean isConditional, boolean conditionTaken, int upperBound) {
//        if (isConditional) {
//            return tf_condstmt(conditionTaken, stmt, upperBound);
//        } else {
//            return tf_assignstmt(stmt, upperBound);
//        }
//    }
//
//    @Override
//    public LatticeElement tf_identity_fn() {
//        return new PointsToLatticeElement(state);
//    }
//
//    @Override
//    public String toString() {
//        List<String> entries = new ArrayList<>();
//        TreeMap<String, HashSet<String>> sortedState = new TreeMap<>(state);
//
//        for (Map.Entry<String, HashSet<String>> entry : sortedState.entrySet()) {
//            if (isRelevantVariable(entry.getKey())) {
//                entries.add(String.format("%s:{%s}",
//                        entry.getKey(),
//                        String.join(",", new TreeSet<>(entry.getValue()))));
//            }
//        }
//        return String.join(" ", entries);
//    }
//}

import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.jimple.ArrayRef;
import soot.Value;
import java.util.*;

public class PointsToLatticeElement implements LatticeElement {

    private final HashMap<String, HashSet<String>> state;
    private static int newArrayCounter = 0;

    public HashMap<String, HashSet<String>> getState() {
        return state;
    }
    public PointsToLatticeElement() {
        this.state = new HashMap<>();
    }

    public PointsToLatticeElement(HashMap<String, HashSet<String>> state) {
        this.state = new HashMap<>(state);
    }

    private boolean isRelevantVariable(String var) {
        // Only track array references (variables starting with 'r')
        return var.startsWith("r");
    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        PointsToLatticeElement other = (PointsToLatticeElement) r;
        HashMap<String, HashSet<String>> newState = new HashMap<>();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(this.state.keySet());
        allKeys.addAll(other.state.keySet());

        for (String key : allKeys) {
            if (!isRelevantVariable(key)) continue;

            HashSet<String> merged = new HashSet<>();
            if (this.state.containsKey(key)) {
                merged.addAll(this.state.get(key));
            }
            if (other.state.containsKey(key)) {
                merged.addAll(other.state.get(key));
            }

            // If either contains Null, ensure Null is part of the merged set
            if ((this.state.containsKey(key) && this.state.get(key).contains("null")) ||
                    (other.state.containsKey(key) && other.state.get(key).contains("null"))) {
                merged.add("null");
            }


            if (!merged.isEmpty()) {
                newState.put(key, merged);
            }
        }

        return new PointsToLatticeElement(newState);
    }

    @Override
    public boolean equals(LatticeElement r) {
        if (!(r instanceof PointsToLatticeElement)) return false;
        PointsToLatticeElement other = (PointsToLatticeElement) r;

        // Compare only relevant variables
        Map<String, HashSet<String>> thisRelevant = new HashMap<>();
        Map<String, HashSet<String>> otherRelevant = new HashMap<>();

        for (Map.Entry<String, HashSet<String>> entry : this.state.entrySet()) {
            if (isRelevantVariable(entry.getKey())) {
                thisRelevant.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, HashSet<String>> entry : other.state.entrySet()) {
            if (isRelevantVariable(entry.getKey())) {
                otherRelevant.put(entry.getKey(), entry.getValue());
            }
        }

        return thisRelevant.equals(otherRelevant);
    }

    @Override
    public LatticeElement tf_assignstmt(Stmt stmt, int upperBound) {
        if (!(stmt instanceof AssignStmt)) return this;

        AssignStmt assignStmt = (AssignStmt) stmt;
        Value leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();
        String left = leftOp.toString();
        String right = rightOp.toString();

        // Create new state without copying the old state for the target variable
        PointsToLatticeElement newElement = new PointsToLatticeElement();
        for (Map.Entry<String, HashSet<String>> entry : state.entrySet()) {
            if (!entry.getKey().equals(left)) {
                newElement.state.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
        }

        // Skip if left operand is not a relevant variable
        if (!isRelevantVariable(left)) return newElement;

        // Handle array allocation
        if (right.contains("newarray")) {
            HashSet<String> pointsTo = new HashSet<>();
            pointsTo.add("new" + String.format("%02d", newArrayCounter++));
            newElement.state.put(left, pointsTo);
        }
        // Handle null assignment
        else if ("null".equals(right)) {
            HashSet<String> pointsTo = new HashSet<>();
            pointsTo.add("null");
            newElement.state.put(left, pointsTo);
        }
        // Handle variable assignment (r2 = r0 or r2 = r1)
        else if (right.startsWith("r")) {
            HashSet<String> pointsTo = new HashSet<>();
            if (state.containsKey(right)) {
                pointsTo.addAll(state.get(right));
            }
            newElement.state.put(left, pointsTo);
        }
        // Generalize handling of conditional assignments
        else {
            HashSet<String> pointsTo = new HashSet<>();
            for (String key : state.keySet()) {
                if (right.contains(key)) {
                    pointsTo.addAll(state.get(key));
                }
            }
            if (!pointsTo.isEmpty()) {
                newElement.state.put(left, pointsTo);
            }
        }

        return newElement;
    }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st, int upperBound) {
        return this; // Points-to analysis is flow-insensitive
    }

    @Override
    public LatticeElement transfer(Stmt stmt, boolean isConditional, boolean conditionTaken, int upperBound) {
        if (isConditional) {
            return tf_condstmt(conditionTaken, stmt, upperBound);
        } else {
            return tf_assignstmt(stmt, upperBound);
        }
    }

    @Override
    public LatticeElement tf_identity_fn() {
        return new PointsToLatticeElement(state);
    }

    @Override
    public String toString() {
        List<String> entries = new ArrayList<>();
        TreeMap<String, HashSet<String>> sortedState = new TreeMap<>(state);

        for (Map.Entry<String, HashSet<String>> entry : sortedState.entrySet()) {
            if (isRelevantVariable(entry.getKey())) {
                entries.add(String.format("%s:{%s}",
                        entry.getKey(),
                        String.join(",", new TreeSet<>(entry.getValue()))));
            }
        }
        return String.join(" ", entries);
    }
}