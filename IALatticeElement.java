import java.util.HashMap;
import java.util.Map;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.IntConstant;
import soot.Local;

public class IALatticeElement implements LatticeElement {

    private Map<String, Interval> state;
    // bot element
    private static final Interval BOTTOM = new Interval(Integer.MAX_VALUE, Integer.MIN_VALUE);
    public IALatticeElement() {
        state = new HashMap<>();
    }

    public IALatticeElement(Map<String, Interval> state) {
        this.state = new HashMap<>(state);
    }

    // Join operation for intervals
    @Override
    public IALatticeElement join_op(LatticeElement r) {
        if (!(r instanceof IALatticeElement)) {
            return this; // Return current element if types don't match
        }
        IALatticeElement otherElement = (IALatticeElement) r;
        Map<String, Interval> newstate = new HashMap<>();

        // loads all entries in this to the result element
        for (Map.Entry<String, Interval> entry : state.entrySet()) {
            Interval interval = entry.getValue();
            Interval otherInterval = otherElement.state.getOrDefault(entry.getKey(), BOTTOM);
            newstate.put(entry.getKey(), interval.join(otherInterval));
        }

        // loads all entries in r element to the result element
        for (Map.Entry<String, Interval> entry : otherElement.state.entrySet()) {
            if (!newstate.containsKey(entry.getKey())) {
                newstate.put(entry.getKey(), entry.getValue());
            }
        }
        return new IALatticeElement(newstate);
    }

    // checks if two lattice elements are equal
    @Override
    public boolean equals(LatticeElement r) {
        if (!(r instanceof IALatticeElement)) {
            return false;
        }
        IALatticeElement otherElement = (IALatticeElement) r;
        return this.state.equals(otherElement.state);
    }

    // Transfer function for assignments
    @Override
    public LatticeElement tf_assignstmt(Stmt st) {
        AssignStmt assignStmt = (AssignStmt) st;
        Value lhs = assignStmt.getLeftOp();
        Value rhs = assignStmt.getRightOp();

       

        String lhsStr = lhs.toString();
        Interval rhsInterval = evaluate(rhs);

        HashMap<String, Interval> newstate = new HashMap<>();
        for (Map.Entry<String, Interval> entry : state.entrySet()) {
            newstate.put(entry.getKey(), entry.getValue());
        }
        newstate.put(lhsStr, rhsInterval);

        return new IALatticeElement(newstate);
    }

    public String toString() {
        StringBuilder ret = new StringBuilder("{");
        for (Map.Entry<String, Interval> entry : state.entrySet()) {
            ret.append(entry.getKey()).append(" => ").append(entry.getValue()).append("\n");
        }
        return ret.append("}").toString();
    }
    private Interval evaluate(Value val) {
        if (val instanceof IntConstant) {
            int constantValue = ((IntConstant) val).value;
            return new Interval(constantValue, constantValue);
        } else if (val instanceof Local) {
            return state.getOrDefault(val.toString(), BOTTOM);
        }
        return BOTTOM;
    }
    // Transfer function for conditional statements
    // need to write complete code
    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        return this;
    }

    
}

class Interval {
    private int lowerBound;
    private int upperBound;

    public Interval(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    // Join two intervals
    public Interval join(Interval other) {
        return new Interval(
            Math.min(this.lowerBound, other.lowerBound),
            Math.max(this.upperBound, other.upperBound)
        );
    }

    @Override
    public String toString() {
        return "[" + lowerBound + ", " + upperBound + "]";
    }
}
