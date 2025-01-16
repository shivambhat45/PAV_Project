import java.util.*;

public class ValueFilter {

    public static Interval getVariableValueAtProgramPoint(
            ArrayList<ArrayList<LatticeElement>> output2,
            int programPointIndex,
            String variableName) {

        Interval latestInterval = null;

        // Step 1: Iterate through all iterations
        for (int iteration = 0; iteration < output2.size(); iteration++) {
            ArrayList<LatticeElement> iterationData = output2.get(iteration);

            // Step 2: Check if the program point index is valid for this iteration
            if (programPointIndex < 0 || programPointIndex >= iterationData.size()) {
//                System.err.println("Invalid program point index: " + programPointIndex + " in iteration " + iteration);
                continue; // Skip this iteration
            }

            // Step 3: Get the lattice element for the program point
            LatticeElement latticeElement = iterationData.get(programPointIndex);

            // Step 4: Cast to the specific lattice element type
            IALatticeElement iaLatticeElement = (IALatticeElement) latticeElement;

            // Step 5: Retrieve the state map
            Map<String, Interval> state = iaLatticeElement.getState();

            // Step 6: Check if the variable exists
            if (state.containsKey(variableName)) {
                latestInterval = state.get(variableName);
            }
        }

        // Step 7: Return the latest (converged) interval
        if (latestInterval != null) {
            return latestInterval;
        }

//        System.err.println("Variable " + variableName + " not found at program point " + programPointIndex);
        return null;
    }

    public static HashSet<String> getPointerValueAtProgramPoint(
            ArrayList<ArrayList<LatticeElement>> output,
            int programPointIndex,
            String variableName) {

        HashSet<String> latestPointers = null;

        // Step 1: Iterate through all iterations
        for (int iteration = 0; iteration < output.size(); iteration++) {
            ArrayList<LatticeElement> iterationData = output.get(iteration);

            // Step 2: Check if the program point index is valid for this iteration
            if (programPointIndex < 0 || programPointIndex >= iterationData.size()) {
                System.err.println("Invalid program point index: " + programPointIndex + " in iteration " + iteration);
                continue; // Skip this iteration
            }

            // Step 3: Get the lattice element for the program point
            LatticeElement latticeElement = iterationData.get(programPointIndex);

            // Step 4: Cast to the specific lattice element type
            PointsToLatticeElement ptrLatticeElement = (PointsToLatticeElement) latticeElement;

            // Step 5: Retrieve the pointer state (HashMap<String, HashSet<String>>)
            HashMap<String, HashSet<String>> pointerState = ptrLatticeElement.getState();

            // Step 6: Check if the variable exists in the pointer state
            if (pointerState.containsKey(variableName)) {
                latestPointers = pointerState.get(variableName);
            }
        }

        // Step 7: Return the latest (converged) set of pointers
        if (latestPointers != null) {
            return latestPointers;
        }

//        System.err.println("Pointer information for variable " + variableName + " not found at program point " + programPointIndex);
        return null;
    }
}
