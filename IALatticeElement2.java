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
import soot.jimple.internal.JIfStmt;
import soot.Local;
import soot.Value;
import soot.jimple.*;
import java.util.HashMap;
import java.util.Map;

public class IALatticeElement2 implements LatticeElement,Cloneable {
    private Map<String, Interval> state;
    private boolean propogationCondition=true;
    // bot element
    // private static final Interval BOTTOM = new Interval(Integer.MAX_VALUE, Integer.MIN_VALUE);

    // Custom deep copy method
    @Override
    public LatticeElement deepCopy() {
        Map<String, Interval> newState = new HashMap<>();
        for (Map.Entry<String, Interval> entry : this.state.entrySet()) {
            newState.put(entry.getKey(), new Interval(entry.getValue())); // New Interval for deep copy
        }
        return new IALatticeElement2(newState);
    }


    public IALatticeElement2() {
        this.state = new HashMap<>();
        propogationCondition=true;
    }

    public boolean getPropogationCondition()
    {
        return propogationCondition;
    }

    public boolean setPropogationCondition(boolean b)
    {
        this.propogationCondition=b;
        return this.propogationCondition;
    }

    public void initializeVariable(String variableName) {
        // Default interval, e.g., representing the full possible range [-∞, ∞]
        Interval defaultInterval = new Interval(-1000, 1000);

        // Initialize the variable with the default interval if not already in the state
        state.putIfAbsent(variableName, defaultInterval);
    }

    public Map<String, Interval> getState() {
        return this.state;
    }

//    public IALatticeElement2(Map<String, Interval> state) {
//        this.state = new HashMap<>(state);
//    }

    public IALatticeElement2(Map<String, Interval> state) {
        this.state = new HashMap<>();
        for (Map.Entry<String, Interval> entry : state.entrySet()) {
            this.state.put(entry.getKey(), new Interval(entry.getValue().getLowerBound(), entry.getValue().getUpperBound())); // Deep copy
        }
    }


    // Join operation for intervals
    @Override
    public IALatticeElement2 join_op(LatticeElement r) {
        IALatticeElement2 otherElement = (IALatticeElement2) r;
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
        return new IALatticeElement2(newstate);
    }

    // checks if two lattice elements are equal
    @Override
    public boolean equals(LatticeElement r) {
        IALatticeElement2 otherElement = (IALatticeElement2) r;
        return this.state.equals(otherElement.state);
    }

    @Override
    public LatticeElement tf_assignstmt(Stmt st) {
        if (!(st instanceof AssignStmt)) {
            return this; // If it's not an assignment statement, return the current state
        }

        AssignStmt assignStmt = (AssignStmt) st;
        Value leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();
        System.out.println("Assn"+leftOp);
        System.out.println("Assn"+rightOp);

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
            } else if (rightOp instanceof Local) {
                // If the right operand is a variable, get its interval
                String rightVarName = ((Local) rightOp).getName();
                Interval rightInterval = state.getOrDefault(rightVarName, Interval.TOP);
                newInterval = rightInterval; // Copy the interval from the variable
            } else {
                // If the right operand is something else, we can't determine its interval
                newInterval = Interval.TOP; // Default to bottom for unexpected cases
            }

            // Update the state with the new interval for the variable
            newState.put(varName, newInterval);
            System.out.println(newState);
        }

        return new IALatticeElement2(newState);
    }


    private Interval computeIntervalFromBinaryOp(BinopExpr binop) {
        Interval leftInterval = getIntervalFromValue(binop.getOp1());
        Interval rightInterval = getIntervalFromValue(binop.getOp2());
        System.out.println(binop.getSymbol());
        String operator = binop.getSymbol();

        operator=operator.trim();
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

            // Print variable name for debugging
            System.out.println("Checking variable: " + varName);

            // Print the entire state before accessing the variable
            System.out.println("Current state: " + state);

            // Return the interval for the variable, or BOTTOM if not present
            Interval interval = state.getOrDefault(varName, Interval.TOP);
            System.out.println("Interval for " + varName + ": " + interval);
            return interval;
        }

        return Interval.BOTTOM; // Default return for unsupported value types
    }
    // Transfer function for conditional statements
    @Override
    public LatticeElement tf_condstmt(boolean isTrueBranch, Stmt st,int upperBound) {
        IfStmt ifStmt = (IfStmt) st;
        Unit target = ifStmt.getTarget();
        Value condition = ifStmt.getCondition();
        System.out.println("cond"+condition);
        // Create a new state based on the current state
        IALatticeElement2 newState = new IALatticeElement2(this.state); // Copy current state
        System.out.println("Is it mine");
        System.out.println(newState);

        // Assuming the condition can be a binary operation involving local variables
        if (condition instanceof BinopExpr) {
            BinopExpr binOp = (BinopExpr) condition;
            Interval leftInterval = getIntervalFromValue(binOp.getOp1());
            Interval rightInterval = getIntervalFromValue(binOp.getOp2());
            System.out.println(binOp);
            System.out.println(leftInterval);
            System.out.println(rightInterval);

            if (leftInterval.getLowerBound() < 0 || leftInterval.getUpperBound()>upperBound || rightInterval.getLowerBound()<0 || rightInterval.getUpperBound() > upperBound) {
                newState.setPropogationCondition(false);
            } else {
                if (binOp instanceof JGeExpr) { // condition: left >= right
                    if (isTrueBranch) {
                        if (leftInterval.getUpperBound() == rightInterval.getLowerBound()) {
                            System.out.println("What's in the state");
                            System.out.println(state);
                            newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound()), rightInterval.getUpperBound()));
                        } else {
                            newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound()), leftInterval.getUpperBound()));
                            //                        newState.state.put(((Local) binOp.getOp1()).getName(),new Interval());
                        }
                    } else {
                        System.out.println("Is it here now");
                        if (leftInterval.getUpperBound() == rightInterval.getLowerBound()) {
                            System.out.println("Inside");
                            newState.setPropogationCondition(false);
                        } else {
                            // For false branch, left < right
                            // This means left must be less than the minimum of right
                            newState.state.put(((Local) binOp.getOp1()).getName(), 
                            new Interval(leftInterval.getLowerBound(), Math.min(leftInterval.getUpperBound(), rightInterval.getUpperBound() - 1)));
                            //                        tf_identity_fn();
                        }
                    }
                } else if (binOp instanceof JLtExpr) { // condition: left < right
                    if (isTrueBranch) {
                        // if (leftInterval.getUpperBound() == rightInterval.getLowerBound()) {
                        //     System.out.println("What's in the state");
                        //     System.out.println(state);
                        //     newState.state.put(((Local) binOp.getOp1()).getName(),
                        //             new Interval(leftInterval.getLowerBound(), Math.min(leftInterval.getUpperBound(), rightInterval.getUpperBound() - 1)));
                        // } else {
                        //     newState.state.put(((Local) binOp.getOp1()).getName(),
                        //             new Interval(leftInterval.getLowerBound(), Math.min(leftInterval.getUpperBound(), rightInterval.getUpperBound() - 1)));
                        //     //                        newState.state.put(((Local) binOp.getOp1()).getName(),new Interval());
                        // }
                        // same for both the conditions
                        newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(leftInterval.getLowerBound(), Math.min(leftInterval.getUpperBound(), rightInterval.getUpperBound() - 1)));
                    } else {
                        // >=
                        if (leftInterval.getUpperBound() == rightInterval.getLowerBound()) {
                            System.out.println("What's in the state");
                            System.out.println(state);
                            newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound()), rightInterval.getUpperBound()));
                        } else {
                            newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound()), leftInterval.getUpperBound()));
                            //                        newState.state.put(((Local) binOp.getOp1()).getName(),new Interval());
                        }
                    }
                } else if (binOp instanceof JLeExpr) { // condition: left <= right
                    if (isTrueBranch) {
                        if (leftInterval.getUpperBound() == rightInterval.getLowerBound()) {
                            System.out.println("What's in the state");
                            System.out.println(state);
                            newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(Math.min(leftInterval.getLowerBound(), rightInterval.getLowerBound()), leftInterval.getUpperBound()));
                        } else {
                            newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(Math.min(leftInterval.getLowerBound(), rightInterval.getLowerBound()), rightInterval.getUpperBound()));
                            //                        newState.state.put(((Local) binOp.getOp1()).getName(),new Interval());
                        }
                    } else { // left > right
                        System.out.println("Is it here now");
                        if (leftInterval.getUpperBound() == rightInterval.getLowerBound()) {
                            System.out.println("Inside");
                            newState.setPropogationCondition(false);
                        } else {
                            // For false branch, left > right
                            newState.state.put(((Local) binOp.getOp1()).getName(), 
                            new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound() + 1),leftInterval.getUpperBound()));
                            //                        tf_identity_fn();
                        }
                    }
                } else if (binOp instanceof JGtExpr) { // condition: left > right
                    if (isTrueBranch) {
                        // if (leftInterval.getUpperBound() == rightInterval.getLowerBound()) {
                        //     // System.out.println("What's in the state");
                        //     // System.out.println(state);
                        //     // newState.state.put(((Local) binOp.getOp1()).getName(),
                        //     //         new Interval(leftInterval.getLowerBound(), leftInterval.getUpperBound()));
                        //     System.out.println("Inside");
                        //     newState.setPropogationCondition(false);
                        // } else {
                        //     newState.state.put(((Local) binOp.getOp1()).getName(),
                        //             new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound() + 1), leftInterval.getUpperBound()));
                        //     //                        newState.state.put(((Local) binOp.getOp1()).getName(),new Interval());
                        // }
                        newState.state.put(((Local) binOp.getOp1()).getName(), 
                            new Interval(Math.max(leftInterval.getLowerBound(), rightInterval.getLowerBound() + 1),leftInterval.getUpperBound()));
                    } else { //<=
                        if (leftInterval.getUpperBound() == rightInterval.getLowerBound()) {
                            System.out.println("What's in the state");
                            System.out.println(state);
                            newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(Math.min(leftInterval.getLowerBound(), rightInterval.getLowerBound()), leftInterval.getUpperBound()));
                        } else {
                            newState.state.put(((Local) binOp.getOp1()).getName(),
                                    new Interval(Math.min(leftInterval.getLowerBound(), rightInterval.getLowerBound()), rightInterval.getUpperBound()));
                            //                        newState.state.put(((Local) binOp.getOp1()).getName(),new Interval());
                        }
                    }
                } else if (binOp instanceof JNeExpr) { // condition: left != right
                    if (isTrueBranch) {
                        // For true branch, left != right is true
                        // This means left cannot be equal to right
                        // This is complex; you might need to implement logic to reflect this
                    } else {
                        // For false branch, left == right
                        // In this case, we need to set the intervals to reflect equality
                        newState.state.put(((Local) binOp.getOp1()).getName(),
                                new Interval(rightInterval.getLowerBound(), rightInterval.getUpperBound()));
                    }
                } else if (binOp instanceof JEqExpr) { // condition: left == right
                    if (isTrueBranch) {
                        // For true branch, left == right is true
                        newState.state.put(((Local) binOp.getOp1()).getName(),
                                new Interval(rightInterval.getLowerBound(), rightInterval.getUpperBound()));
                    } else {
                        // For false branch, left != right
                        // Implement logic to reflect inequality
                    }
                }
            }
            // Return the updated state after applying the conditional transfer function

        }return newState;
    }

    @Override
    public LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken,int upperBound)
    {
        AbstractStmtSwitch<LatticeElement> stmtSwitch = new AbstractStmtSwitch<LatticeElement>() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                setResult(tf_assignstmt(stmt));
            }

            @Override
            public void caseIfStmt(IfStmt stmt) {
                setResult(tf_condstmt(conditionTaken,stmt,upperBound));
            }

            @Override
            public void defaultCase(Object obj) {
                // for any other type of statement apply the identity function.
                if (st instanceof DefinitionStmt) {
                    DefinitionStmt defStmt = (DefinitionStmt) st;
                    Value lhs = defStmt.getLeftOp(); // Get the left side of the statement

                    if (lhs instanceof Local) {
                        String variableName = ((Local) lhs).getName(); // Extract variable name
                        System.out.println("Left variable: " + variableName);
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
        return new IALatticeElement2(cloning(state));
    }


    @Override
    public boolean lessThan(LatticeElement r) {
        boolean var=true;

//        if(((IALatticeElement2) r).getState().size()!=this.getState().size())
//        {
//            var=false;
//            return var;
//        }
//
//
//        // Compare each entry in map1 with map2
//        for (Map.Entry<String, Interval> entry : ((IALatticeElement2) r).getState().entrySet()) {
//            String key = entry.getKey();
//            Interval interval1 = entry.getValue();
//
//            // Get the interval for the corresponding key in map2
//            Interval interval2 = this.getState().get(key);
//
//            // Check if map2 has the key and if the intervals are equal
//            if(interval1.)
//        }





        return var;
    }

    @Override
    public boolean propogationCondition() {
        return this.propogationCondition;
    }


    public Map<String, Interval> cloning(Map<String, Interval> x) {
        Map<String, Interval> cloned = new HashMap<>();

        for (Map.Entry<String, Interval> entry : x.entrySet()) {
            cloned.put(entry.getKey(), new Interval(entry.getValue().getLowerBound(),entry.getValue().getUpperBound()));
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
    private final int lowerBound;
    private final int upperBound;

    private String finalLowerBound;
    private String finalUpperBound;

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

    public static final Interval BOTTOM = new Interval(1000, -1000);
    public static final Interval TOP = new Interval(-1000, 1000);

    public Interval()
    {
        this.lowerBound= BOTTOM.lowerBound;
        this.upperBound= BOTTOM.upperBound;
    }
    public Interval(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.finalLowerBound= String.valueOf(lowerBound);
        this.finalUpperBound= String.valueOf(upperBound);
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public boolean isEmpty()
    {
        return lowerBound>upperBound;
    }

    // Method for addition of two intervals
    public Interval add(Interval other) {
        return new Interval(this.lowerBound + other.lowerBound, this.upperBound + other.upperBound);
    }

    // Method for subtraction of two intervals
    public Interval subtract(Interval other) {
        return new Interval(this.lowerBound - other.upperBound, this.upperBound - other.lowerBound);
    }

    // Method for multiplication of two intervals
    public Interval multiply(Interval other) {
        int[] products = {
                this.lowerBound * other.lowerBound,
                this.lowerBound * other.upperBound,
                this.upperBound * other.lowerBound,
                this.upperBound * other.upperBound
        };
        // System.out.println(this.lowerBound);
        // System.out.println( this.upperBound);
        // System.out.println(other.lowerBound);
        // System.out.println( other.upperBound);
        // System.out.println(products[0]);
        // System.out.println(products[1]);
        // System.out.println(products[2]);
        // System.out.println(products[3]);
        int minProduct = Math.min(Math.min(products[0], products[1]), Math.min(products[2], products[3]));
        int maxProduct = Math.max(Math.max(products[0], products[1]), Math.max(products[2], products[3]));
        // System.out.println(minProduct);
        // System.out.println(maxProduct);
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
        if(other.lowerBound==-1000 && other.upperBound==1000)
        {
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