import java.util.*;

public class Kildall {

    /**
     * Runs Kildall's LFP Algo on the given program points.
     *
     * @param points the list of program points.
     * @param d0     the initial state of the first program point p0.
     * @param bot    bot LatticeElement.
     * @return
     */
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

//        System.out.println(states);
        System.out.println(marked);


        int y=0;
        while (!marked.isEmpty()){
            Integer curPoint = marked.iterator().next();
            // unmark the current point.
            marked.remove(curPoint);
//            System.out.println(marked);


            LatticeElement currState=states.get(curPoint);

            System.out.println("Current State");
            System.out.println(currState);
            // Join the state with other points sharing the same successors
            LatticeElement currJoinedState = joinStates(states, points.get(curPoint),points);

//            returnValue2.add(new ArrayList<>(states));

            for (int i = 0; i < points.get(curPoint).getSuccessors().size(); i++) {
                //  transfer the curState through the statement and get the new state.
                System.out.println(points.get(curPoint).getStmt());
                // This should reflect the state just before executing the statement.
                System.out.println(states.get(curPoint));
                LatticeElement nextNewState = currJoinedState.transfer(points.get(curPoint).getStmt(), points.get(curPoint).getStmt().branches(), i != 0,upperBound);

//                LatticeElement nextNewState = curState.transfer(points.get(curPoint).getStmt(), points.get(curPoint).getStmt().branches(), i != 0);

                System.out.println("Transfer Applied");
                System.out.println(nextNewState);
                System.out.println("Check Again");
                System.out.println(states.get(curPoint));
                // propagate the new state to the successor.

                if(nextNewState.propogationCondition())

                {
                    propagate(states, points.get(curPoint).getSuccessors().get(i).getId(), nextNewState, marked);
                }

            }
                System.out.println("Added Values After For Loop");
                System.out.println(states);

                returnValue.add(new ArrayList<>(states)); // this is okay because we are not changing LatticeElement anywhere.

                y=y+1;

            System.out.println(marked);

            System.out.println("_______________________");
        }

        return returnValue;
    }

    /**
     * Propagates the newState to the given program point.
     *
     * @param states          list of states.
     * @param to              the point to propagate the state to.
     * @param statePropagated the new state to propagte.
     * @param marked          the  Set of marked points, which will be updated.
     */
    private static void propagate(ArrayList<LatticeElement> states, int to, LatticeElement statePropagated, HashSet<Integer> marked) {
        LatticeElement nextCurState = states.get(to); // current state at `to` point.
        LatticeElement joined = statePropagated.join_op(nextCurState); // join curState and stateToPropagate.

        if (!joined.equals(nextCurState)) { // if the nextCurState <= joined
            // update and mark.
            System.out.println("After Joined");

            System.out.println(joined);
            System.out.println("to: "+to);
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
        System.out.println(pp1.getId());
        System.out.println(pp2.getId());

        return true;
    }

}

