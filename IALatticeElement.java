import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JNeExpr;
import soot.jimple.internal.JIfStmt;
import soot.Local;
import soot.Value;
import java.util.HashMap;
import java.util.Map;

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
        IALatticeElement otherElement = (IALatticeElement) r;
        return this.state.equals(otherElement.state);
    }

    // Transfer function for assignment statements
    @Override
    public LatticeElement tf_assignstmt(Stmt st) {
        AssignStmt assignStmt = (AssignStmt) st;
        Value lhs = assignStmt.getLeftOp();
        Value rhs = assignStmt.getRightOp();

        String lhsStr = lhs.toString();
        Interval rhsInterval = evaluate(rhs);
        HashMap<String, Interval> newstate = new HashMap<>(state);
        newstate.put(lhsStr, rhsInterval);
        return new IALatticeElement(newstate);
    }

    // Transfer function for conditional statements
    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        JIfStmt ifStmt = (JIfStmt) st;
        BinopExpr condition = (BinopExpr) ifStmt.getCondition();
        String varName = condition.getOp1().toString();
        Interval currentInterval = state.getOrDefault(varName, BOTTOM);
        Interval newInterval;
        if (condition instanceof JLtExpr) {
            newInterval = new Interval(BOTTOM.getLowerBound(), currentInterval.getLowerBound() - 1);
        } else if (condition instanceof JLeExpr) {
            newInterval = new Interval(BOTTOM.getLowerBound(), currentInterval.getLowerBound());
        } else if (condition instanceof JGtExpr) {
            newInterval = new Interval(currentInterval.getUpperBound() + 1, BOTTOM.getUpperBound());
        } else if (condition instanceof JGeExpr) {
            newInterval = new Interval(currentInterval.getUpperBound(), BOTTOM.getUpperBound());
        } else if (condition instanceof JEqExpr) {
            newInterval = currentInterval; // No change in bounds
        } else if (condition instanceof JNeExpr) {
            newInterval = currentInterval; // No change in bounds
        } else {
            throw new UnsupportedOperationException("Unknown condition type: " + condition);
        }

        HashMap<String, Interval> newstate = new HashMap<>(state);
        newstate.put(varName, newInterval);
        return new IALatticeElement(newstate);
    }

    @Override
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
        } else if (val instanceof BinopExpr) {
            return evaluateBinaryOperation((BinopExpr) val);
        }
        return BOTTOM;
    }

    private Interval evaluateBinaryOperation(BinopExpr binop) {
        Value leftOp = binop.getOp1();
        Value rightOp = binop.getOp2();
        String operator = binop.getSymbol();
        Interval leftInterval = evaluate(leftOp);
        Interval rightInterval = evaluate(rightOp);

        switch (operator) {
            case "+":
                return new Interval(
                    leftInterval.getLowerBound() + rightInterval.getLowerBound(),
                    leftInterval.getUpperBound() + rightInterval.getUpperBound()
                );
            case "-":
                return new Interval(
                    leftInterval.getLowerBound() - rightInterval.getUpperBound(),
                    leftInterval.getUpperBound() - rightInterval.getLowerBound()
                );
            case "*":
                return multiplyIntervals(leftInterval, rightInterval);
            case "/":
                return divideIntervals(leftInterval, rightInterval);
            default:
                return BOTTOM; // or throw an exception for unsupported operation
        }
    }

    private Interval multiplyIntervals(Interval left, Interval right) {
        int min = Math.min(
            Math.min(left.getLowerBound() * right.getLowerBound(), left.getLowerBound() * right.getUpperBound()),
            Math.min(left.getUpperBound() * right.getLowerBound(), left.getUpperBound() * right.getUpperBound())
        );
        int max = Math.max(
            Math.max(left.getLowerBound() * right.getLowerBound(), left.getLowerBound() * right.getUpperBound()),
            Math.max(left.getUpperBound() * right.getLowerBound(), left.getUpperBound() * right.getUpperBound())
        );
        return new Interval(min, max);
    }

    private Interval divideIntervals(Interval left, Interval right) {
        if (right.getLowerBound() == 0 && right.getUpperBound() == 0) {
            return BOTTOM; // or throw an exception for division by zero
        }
        int min = Math.min(
            Math.min(left.getLowerBound() / right.getLowerBound(), left.getLowerBound() / right.getUpperBound()),
            Math.min(left.getUpperBound() / right.getLowerBound(), left.getUpperBound() / right.getUpperBound())
        );
        int max = Math.max(
            Math.max(left.getLowerBound() / right.getLowerBound(), left.getLowerBound() / right.getUpperBound()),
            Math.max(left.getUpperBound() / right.getLowerBound(), left.getUpperBound() / right.getUpperBound())
        );
        return new Interval(min, max);
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
