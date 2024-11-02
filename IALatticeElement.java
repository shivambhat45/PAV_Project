import soot.Unit;
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
import soot.Local;
import soot.Value;
import soot.jimple.*;

import java.util.HashMap;
import java.util.Map;

public class IALatticeElement implements LatticeElement, Cloneable {
    private Map<String, Interval> state;

    public IALatticeElement() {
        this.state = new HashMap<>();
    }

    public void initializeVariable(String variableName) {
        // Default interval, e.g., representing the full possible range [-∞, ∞]
        Interval defaultInterval = new Interval(-10000, 10000);

        // Initialize the variable with the default interval if not already in the state
        state.putIfAbsent(variableName, defaultInterval);
    }

    public void initialize2Variable(String variableName) {
        Interval defaultInterval = new Interval(10000, -10000);

        // Initialize the variable with the default interval if not already in the state
        state.putIfAbsent(variableName, defaultInterval);
    }

    public Map<String, Interval> getState() {
        return this.state;
    }

    public IALatticeElement(Map<String, Interval> state) {
        this.state = new HashMap<>();
        for (Map.Entry<String, Interval> entry : state.entrySet()) {
            this.state.put(entry.getKey(), new Interval(entry.getValue().getLowerBound(), entry.getValue().getUpperBound())); // Deep copy
        }
    }


    // Join operation for intervals
    @Override
    public IALatticeElement join_op(LatticeElement r) {
        IALatticeElement otherElement = (IALatticeElement) r;
        Map<String, Interval> newstate = new HashMap<>();

        // loads all entries in this to the result element
        for (Map.Entry<String, Interval> entry : state.entrySet()) {
            Interval interval = entry.getValue();
            Interval otherInterval = otherElement.state.getOrDefault(entry.getKey(), Interval.BOTTOM);
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

    @Override
    public LatticeElement tf_assignstmt(Stmt st, int upperBound) {
        if (!(st instanceof AssignStmt)) {
            return this; // If it's not an assignment statement, return the current state
        }

        AssignStmt assignStmt = (AssignStmt) st;
        Value leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();

        // Create a copy of the current state to avoid modifying the original
        Map<String, Interval> newState = new HashMap<>();
        for (Map.Entry<String, Interval> entry : state.entrySet()) {
            newState.put(entry.getKey(), new Interval(entry.getValue())); // Deep copy each Interval
        }

        // Assuming leftOp is a Local variable
        if (leftOp instanceof Local) {
            Local localVar = (Local) leftOp;
            String varName = localVar.getName();
            Interval newInterval;

            if (rightOp instanceof IntConstant) {
                // If right operand is a constant, set the interval to that constant
                int constantValue = ((IntConstant) rightOp).value;
                newInterval = new Interval(constantValue, constantValue);
            } else if (rightOp instanceof BinopExpr) {
                // If the right operand is a binary operation, compute its interval
                newInterval = computeIntervalFromBinaryOp((BinopExpr) rightOp);
                if (newInterval.getLowerBound() < 0) {
                    newInterval.setLowerBound(-10000);
                }
                if (newInterval.getUpperBound() > upperBound) {
                    newInterval.setUpperBound(10000);
                }
            } else if (rightOp instanceof Local) {
                // If the right operand is a variable, get its interval
                String rightVarName = ((Local) rightOp).getName();
                Interval rightInterval = state.getOrDefault(rightVarName, Interval.TOP);
                newInterval = rightInterval; // Copy the interval from the variable
            } else {
                // If the right operand is something else, we can't determine its interval
                newInterval = Interval.BOTTOM; // Default to bottom for unexpected cases
            }

            // Update the state with the new interval for the variable
            newState.put(varName, newInterval);
            System.out.println(newState);
        }

        return new IALatticeElement(newState);
    }


    private Interval computeIntervalFromBinaryOp(BinopExpr binop) {
        Interval leftInterval = getIntervalFromValue(binop.getOp1());
        Interval rightInterval = getIntervalFromValue(binop.getOp2());
        System.out.println(binop.getSymbol());
        String operator = binop.getSymbol();
        operator = operator.trim();

        switch (operator) {
            case "+":
                return leftInterval.add(rightInterval);
            case "-":
                return leftInterval.subtract(rightInterval);
            case "*":
                System.out.println("mul");
                return leftInterval.multiply(rightInterval);
            case "/":
                return leftInterval.divide(rightInterval);
            case "%":
                return leftInterval.modulo(rightInterval);
            default:
                System.out.println("def");
                return Interval.BOTTOM; // Handle unexpected operators
        }
    }

    private Interval getIntervalFromValue(Value value) {
        if (value instanceof IntConstant) {
            int constantValue = ((IntConstant) value).value;
            return new Interval(constantValue, constantValue);
        } else if (value instanceof Local) {
            // Get interval for a local variable
            String varName = ((Local) value).getName();

            // Return the interval for the variable, or BOTTOM if not present
            Interval interval = state.getOrDefault(varName, Interval.BOTTOM);
            return interval;
        }

        return Interval.BOTTOM; // Default return for unsupported value types
    }

    // Transfer function for conditional statements
    @Override
    public LatticeElement tf_condstmt(boolean isTrueBranch, Stmt st, int upperBound) {
        IfStmt ifStmt = (IfStmt) st;
        Unit target = ifStmt.getTarget();
        Value condition = ifStmt.getCondition();

        // Create a new state based on the current state
        IALatticeElement newState = new IALatticeElement(this.state); // Copy current state

        // Assuming the condition can be a binary operation involving local variables
        if (condition instanceof BinopExpr) {
            BinopExpr binOp = (BinopExpr) condition;
            Interval leftInterval = getIntervalFromValue(binOp.getOp1());
            Interval rightInterval = getIntervalFromValue(binOp.getOp2());

            // Handling BOT CASE
            if (leftInterval.equals(Interval.BOTTOM)) {
                leftInterval = new Interval(-10000, 10000);
            }
            if (rightInterval.equals(Interval.BOTTOM)) {
                rightInterval = new Interval(-10000, 10000);
            }

            // UpperBound
            if (leftInterval.getUpperBound() > upperBound) {
                leftInterval.setUpperBound(10000);
            }
            if (leftInterval.getLowerBound() < 0) {
                leftInterval.setLowerBound(-10000);
            }

            if (binOp instanceof JGeExpr) { // condition: left >= right
                if (isTrueBranch) {
                    newState.state.put(((Local) binOp.getOp1()).getName(),
                            new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound()), leftInterval.getUpperBound()));
                } else {
                    newState.state.put(((Local) binOp.getOp1()).getName(), new Interval(leftInterval.getLowerBound(), Math.min(leftInterval.getUpperBound(), rightInterval.getUpperBound() - 1)));
                }
            } else if (binOp instanceof JLtExpr) {
                if (isTrueBranch) {
                    if (isTrueBranch) {
                        // For true branch, left < right is true
                        newState.state.put(((Local) binOp.getOp1()).getName(),
                                new Interval(leftInterval.getLowerBound(), Math.min(leftInterval.getUpperBound(), rightInterval.getUpperBound() - 1)));
                    } else {
                        newState.state.put(((Local) binOp.getOp1()).getName(),
                                new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound()), leftInterval.getUpperBound()));
                    }
                }
            } else if (binOp instanceof JLeExpr) { // condition: left <= right
                if (isTrueBranch) {
                    newState.state.put(((Local) binOp.getOp1()).getName(), new Interval(leftInterval.getLowerBound(), Math.min(leftInterval.getUpperBound(), rightInterval.getUpperBound())));
                } else {
                    new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound() + 1), leftInterval.getUpperBound());
                    ;
                }


            } else if (binOp instanceof JGtExpr) { // condition: left > right
                if (isTrueBranch) {
                    // For true branch, left > right is true
                    newState.state.put(((Local) binOp.getOp1()).getName(),
                            new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound() + 1), leftInterval.getUpperBound()));
                } else {
                    newState.state.put(((Local) binOp.getOp1()).getName(), new Interval(leftInterval.getLowerBound(), Math.min(leftInterval.getUpperBound(), rightInterval.getUpperBound())));
                }

            } else if (binOp instanceof JNeExpr) { // condition: left != right
                if (isTrueBranch) {
                    // For true branch, left != right is true
                    newState.state.put(((Local) binOp.getOp1()).getName(),new Interval(leftInterval.getLowerBound(), leftInterval.getUpperBound()));

                } else {
                    // For false branch, left == right
                    newState.state.put(((Local) binOp.getOp1()).getName(), new Interval(rightInterval.getLowerBound(), rightInterval.getUpperBound()));
                }
            } else if (binOp instanceof JEqExpr) { // condition: left == right
                if (isTrueBranch) {
                    newState.state.put(((Local) binOp.getOp1()).getName(),
                            new Interval(rightInterval.getLowerBound(), rightInterval.getUpperBound()));

                } else {
                        newState.state.put(((Local) binOp.getOp1()).getName(),
                                new Interval(leftInterval.getLowerBound(), leftInterval.getUpperBound()));
                    }
                }
            }
//            }
            // Return the updated state after applying the conditional transfer function
        return newState;
    }

    @Override
    public LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken, int upperBound) {
        AbstractStmtSwitch<LatticeElement> stmtSwitch = new AbstractStmtSwitch<LatticeElement>() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                setResult(tf_assignstmt(stmt, upperBound));
            }

            @Override
            public void caseIfStmt(IfStmt stmt) {
                setResult(tf_condstmt(conditionTaken, stmt, upperBound));
            }

            @Override
            public void defaultCase(Object obj) {
                // for any other type of statement apply the identity function.
                if (st instanceof DefinitionStmt) {
                    DefinitionStmt defStmt = (DefinitionStmt) st;
                    Value lhs = defStmt.getLeftOp(); // Get the left side of the statement

                    if (lhs instanceof Local) {
                        String variableName = ((Local) lhs).getName(); // Extract variable name
                        // You can now use variableName to retrieve or set its interval in `state`
                        initializeVariable(variableName);
                    }

                }

                setResult(tf_identity_fn());
            }
        };
        st.apply(stmtSwitch);
        return stmtSwitch.getResult();
    }

    @Override
    public LatticeElement tf_identity_fn() {
        return new IALatticeElement(cloning(state));
    }


    public Map<String, Interval> cloning(Map<String, Interval> x) {
        Map<String, Interval> cloned = new HashMap<>();

        for (Map.Entry<String, Interval> entry : x.entrySet()) {
            cloned.put(entry.getKey(), new Interval(entry.getValue().getLowerBound(), entry.getValue().getUpperBound()));
        }

        return cloned;
    }


    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("{");
        for (Map.Entry<String, Interval> entry : state.entrySet()) {
            ret.append(entry.getKey()).append(":").append(entry.getValue()).append(", ");
        }
        if (ret.length() > 1) {
            ret.setLength(ret.length() - 2); // Remove last comma
        }
        return ret.append("}").toString();
    }


}


class Interval {

    private int lowerBound;
    private int upperBound;

    private String finalLowerBound;
    private String finalUpperBound;

    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
    }

    public void setUpperBound(int upperBound) {
        this.upperBound = upperBound;
    }

    public String getFinalLowerBound() {
        return finalUpperBound;
    }

    public void setFinalLowerBound(String finalLowerBound) {
        this.finalLowerBound = finalLowerBound;
    }

    public String getFinalUpperBound() {
        return finalLowerBound;
    }


    public void setFinalUpperBound(String finalUpperBound) {
        this.finalUpperBound = finalUpperBound;
    }

    // Copy constructor for deep copy
    public Interval(Interval other) {
        this.lowerBound = other.lowerBound;
        this.upperBound = other.upperBound;
        this.finalLowerBound = other.finalLowerBound; // Assuming String is immutable
        this.finalUpperBound = other.finalUpperBound; // Assuming String is immutable
    }

    // Deep copy method
    public Interval deepCopy() {
        return new Interval(this);
    }

    public static final Interval BOTTOM = new Interval(10000, -10000);
    public static final Interval TOP = new Interval(-10000, 10000);

    public Interval() {
        this.lowerBound = BOTTOM.lowerBound;
        this.upperBound = BOTTOM.upperBound;
    }

    public Interval(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.finalLowerBound = String.valueOf(lowerBound);
        this.finalUpperBound = String.valueOf(upperBound);
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public boolean isEmpty() {
        return lowerBound > upperBound;
    }

    // Method for addition of two intervals
    public Interval add(Interval other) {

        return new Interval(this.lowerBound + other.lowerBound, this.upperBound + other.upperBound);
    }

    // Method for subtraction of two intervals
    public Interval subtract(Interval other) {
        return new Interval(this.lowerBound - other.lowerBound, this.upperBound - other.upperBound);
    }

    // Method for multiplication of two intervals
    public Interval multiply(Interval other) {
        int[] products = {
                this.lowerBound * other.lowerBound,
                this.lowerBound * other.upperBound,
                this.upperBound * other.lowerBound,
                this.upperBound * other.upperBound
        };
        int minProduct = Math.min(Math.min(products[0], products[1]), Math.min(products[2], products[3]));
        int maxProduct = Math.max(Math.max(products[0], products[1]), Math.max(products[2], products[3]));
        return new Interval(minProduct, maxProduct);
    }

    // Method for division of two intervals
    public Interval divide(Interval other) {
        if (other.lowerBound <= 0 && other.upperBound >= 0) {
            throw new ArithmeticException("Division by an interval that includes zero is not allowed");
        }

        int[] quotients = {
                this.lowerBound / other.lowerBound,
                this.lowerBound / other.upperBound,
                this.upperBound / other.lowerBound,
                this.upperBound / other.upperBound
        };
        int minQuotient = Math.min(Math.min(quotients[0], quotients[1]), Math.min(quotients[2], quotients[3]));
        int maxQuotient = Math.max(Math.max(quotients[0], quotients[1]), Math.max(quotients[2], quotients[3]));
        return new Interval(minQuotient, maxQuotient);
    }

    // Method for modulo of two intervals
    public Interval modulo(Interval other) {
        // Modulo can be complex with intervals; this is a simplistic approach.
        if (other.lowerBound <= 0 && other.upperBound >= 0) {
            throw new ArithmeticException("Modulo by an interval that includes zero is not allowed");
        }
        // In a realistic implementation, you would need to consider the bounds carefully
        return new Interval(0, Math.max(Math.abs(this.lowerBound), Math.abs(this.upperBound)));
    }

    // Join operation
    public Interval join(Interval other) {
        if (other.lowerBound == -10000 && other.upperBound == 10000) {
            return new Interval(this.lowerBound, this.upperBound);
        }
        if (other.lowerBound == 10000 && other.upperBound == -10000) {
            return new Interval(this.lowerBound, this.upperBound);
        }
        int newLower = Math.min(this.lowerBound, other.lowerBound);
        int newUpper = Math.max(this.upperBound, other.upperBound);
        return new Interval(newLower, newUpper);
    }

    @Override
    public String toString() {
        return "[" + lowerBound + ", " + upperBound + "]";
    }

    public String otherString() {
        return "[" + finalLowerBound + ", " + finalUpperBound + "]";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interval)) return false;
        Interval interval = (Interval) o;
        return lowerBound == interval.lowerBound && upperBound == interval.upperBound;
    }

    @Override
    public int hashCode() {
        return 31 * lowerBound + upperBound;
    }
}