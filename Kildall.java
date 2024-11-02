import java.util.*;

public class Kildall {

    public static ArrayList<ArrayList<LatticeElement>> run(List<ProgramPoint> points, LatticeElement d0, LatticeElement bot,int upperBound) {
        // set of marked points.
        HashSet<Integer> marked = new HashSet<>();

        ArrayList<LatticeElement> states = new ArrayList<>(points.size());

        // variable to store result of each iteration.
        ArrayList<ArrayList<LatticeElement>> returnValue = new ArrayList<>();

        // set all states to bot, mark all points.
        for (int i = 0; i <points.size(); i++) {
            marked.add(points.get(i).getId());
            states.add(i,bot);
        }

        // set initial point to d0
        states.set(0, d0);


        int y=0;
        while (!marked.isEmpty()){
            Integer curPoint = marked.iterator().next();
            // unmark the current point.
            marked.remove(curPoint);

            LatticeElement currState=states.get(curPoint);

            // Join the state with other points sharing the same successors
            LatticeElement currJoinedState = joinStates(states, points.get(curPoint),points);

            for (int i = 0; i < points.get(curPoint).getSuccessors().size(); i++) {

                LatticeElement nextNewState = currJoinedState.transfer(points.get(curPoint).getStmt(), points.get(curPoint).getStmt().branches(), i != 0,upperBound);

                // Passing on the values to successor if it updates the values at the successor points
                forward(states, points.get(curPoint).getSuccessors().get(i).getId(), nextNewState, marked);

            }

                returnValue.add(new ArrayList<>(states));
        }

        return returnValue;
    }


    private static void forward(ArrayList<LatticeElement> states, int to, LatticeElement statePropagated, HashSet<Integer> marked) {
        LatticeElement nextCurState = states.get(to); // current state at `to` point.
        LatticeElement joined = statePropagated.join_op(nextCurState); // join curState and stateToPropagate.

        if (!joined.equals(nextCurState)) { // if the nextCurState <= joined
            // update and mark.
            states.set(to, joined); // set the new state.
            marked.add(to); // mark the point.

        }


    }

    /**
     * Joins the states of the current program point with other program points that share the same successors.
     *
     * @param states The current states of all program points.
     * @param currentPoint The current program point being processed.
     * @return The joined state.
     */
    private static LatticeElement joinStates(ArrayList<LatticeElement> states, ProgramPoint currentPoint, List<ProgramPoint> points) {
        LatticeElement joinedState = states.get(currentPoint.getId());

        // Iterate through all program points to find those with the same successors
        for (int point=0;point<points.size();point++) {
            if (points.get(point) != currentPoint && haveSameSuccessors(currentPoint, points.get(point))) {
                joinedState = joinedState.join_op(states.get(points.get(point).getId()));
            }
        }

        return joinedState;
    }

    /**
     * Checks if two program points have the same successors.
     *
     * @param pp1 The first program point.
     * @param pp2 The second program point.
     * @return true if they have the same successors, false otherwise.
     */
    private static boolean haveSameSuccessors(ProgramPoint pp1, ProgramPoint pp2) {
        List<ProgramPoint> successors1 = pp1.getSuccessors();
        List<ProgramPoint> successors2 = pp2.getSuccessors();

        if (successors1.size() != successors2.size()) {
            return false;
        }

        // Create a set for quick lookup
        HashSet<Integer> successorIds1 = new HashSet<>();
        for (ProgramPoint successor : successors1) {
            successorIds1.add(successor.getId());
        }

        // Check if all successors of pp2 are in pp1
        for (ProgramPoint successor : successors2) {
            if (!successorIds1.contains(successor.getId())) {
                return false;
            }
        }
        return true;
    }

}

