import java.util.*;

public class Merger
{
    /**
     * Merges ProgramPoints with the same ID.
     *
     * @param programPoints The list of ProgramPoints to process.
     * @return A new list with merged ProgramPoints.
     */
    public static List<ProgramPoint> mergeProgramPoints(List<ProgramPoint> programPoints) {
        Map<Integer, ProgramPoint> idToProgramPointMap = new HashMap<>(); // Map to hold unique ProgramPoints by ID

        for (ProgramPoint pp : programPoints) {
            int id = pp.getId();

            // Check if a ProgramPoint with the same ID already exists
            if (idToProgramPointMap.containsKey(id)) {
                // Merge this ProgramPoint with the existing one
                ProgramPoint existingPoint = idToProgramPointMap.get(id);
                existingPoint = merge(existingPoint, pp); // Merge properties
                idToProgramPointMap.put(id, existingPoint); // Update with merged result
            } else {
                // Add new ProgramPoint if it doesn't exist
                idToProgramPointMap.put(id, pp);
            }
        }

        return new ArrayList<>(idToProgramPointMap.values());
    }

    /**
     * Merges the properties of two ProgramPoints with the same ID.
     *
     * @param pp1 The first ProgramPoint.
     * @param pp2 The second ProgramPoint.
     * @return A merged ProgramPoint containing combined properties.
     */
    private static ProgramPoint merge(ProgramPoint pp1, ProgramPoint pp2) {
        // Merge successors without duplicates
        Set<ProgramPoint> mergedSuccessors = new HashSet<>(pp1.getSuccessors());
        mergedSuccessors.addAll(pp2.getSuccessors());

        // Create a new ProgramPoint with combined successors
        ProgramPoint mergedPoint = new ProgramPoint(pp1.getId(), pp1.getStmt()); // Keep one statement or merge as needed
        mergedPoint.setSuccessors(new ArrayList<>(mergedSuccessors));

        // Optionally, add logic to handle statement differences, if any
        return mergedPoint;
    }
}