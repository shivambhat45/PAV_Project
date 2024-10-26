import soot.Body;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IALatticePreProcess implements IAPreProcess {
    @Override
    public List<ProgramPoint> PreProcess(Body body) {
        List<ProgramPoint> result = new ArrayList<>();
        HashMap<Unit, ProgramPoint> unitToProgramPoint = new HashMap<>();
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        HashMap<List<Unit>, Integer> successorsToIdMap = new HashMap<>();  // Map unique successor sets to IDs
        int lineNo = 1; // Start sequential ID from 1 for clarity

        // First pass: Create ProgramPoints with unique IDs based on successors
        for (Unit unit : body.getUnits()) {
            List<Unit> successors = graph.getSuccsOf(unit);  // Get successors for this unit

            // Check if this unique set of successors already has an ID
            Integer existingId = successorsToIdMap.get(successors);
            if (existingId == null) {
                // Assign new sequential ID if this successor set hasn't been seen before
                existingId = lineNo++;
                successorsToIdMap.put(successors, existingId);
            }

            // Create the ProgramPoint with the unique ID for this set of successors
            ProgramPoint programPoint = new ProgramPoint(existingId, (Stmt) unit);
            unitToProgramPoint.put(unit, programPoint);
            result.add(programPoint);
        }

        // Second pass: Link successors based on the unique ID mappings
        for (Unit unit : body.getUnits()) {
            ProgramPoint currentPoint = unitToProgramPoint.get(unit);
            if (currentPoint != null) {
                List<ProgramPoint> successorPoints = new ArrayList<>();
                for (Unit succ : graph.getSuccsOf(unit)) {
                    ProgramPoint succPoint = unitToProgramPoint.get(succ);
                    if (succPoint != null) {
                        successorPoints.add(succPoint);
                    }
                }
                currentPoint.setSuccessors(successorPoints);
            }
        }

        // Debugging output to verify
        for (ProgramPoint pp : result) {
            System.out.println("___________________");
            System.out.println("ProgramPoint" + pp);
            System.out.println("Statement: " + pp.getStmt());
            System.out.println("Successors: " + pp.getSuccessors());
            System.out.println("___________________");
        }

        return result;
    }
}