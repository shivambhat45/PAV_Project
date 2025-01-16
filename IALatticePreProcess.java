import soot.Body;
import soot.Unit;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
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
        int lineNo = 0; // Start sequential ID from 0 for clarity

        // First pass: Create ProgramPoints with unique IDs based on successors
        for (Unit unit : body.getUnits()) {
            unit.addTag(new LineNumberTag(lineNo));
            ProgramPoint programPoint = new ProgramPoint(lineNo++, (Stmt) unit);

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

        return result;
    }
}